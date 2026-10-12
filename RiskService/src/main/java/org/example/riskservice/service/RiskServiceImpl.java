package org.example.riskservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.riskservice.dto.ApplicationResponse;
import org.example.riskservice.dto.ApproveRiskRequest;
import org.example.riskservice.dto.DeclineRiskRequest;
import org.example.riskservice.dto.QuoteGenerationRequest;
import org.example.riskservice.dto.RiskAssessmentRequest;
import org.example.riskservice.entity.RiskAssessment;
import org.example.riskservice.enums.RiskAssessmentStatus;
import org.example.riskservice.enums.RiskCategory;
import org.example.riskservice.exceptions.ApplicationNotEligibleForRiskException;
import org.example.riskservice.exceptions.ApplicationNotFoundException;
import org.example.riskservice.exceptions.ApplicationServiceException;
import org.example.riskservice.exceptions.InvalidRiskAssessmentRequestException;
import org.example.riskservice.exceptions.PolicyServiceException;
import org.example.riskservice.exceptions.RiskAssessmentAlreadyFinalizedException;
import org.example.riskservice.exceptions.RiskAssessmentNotFoundException;
import org.example.riskservice.repository.RiskAssessmentRepository;
import org.example.riskservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {

    private final RiskAssessmentRepository riskAssessmentRepository;

    private final WebClient webClient;

    private final R2dbcEntityTemplate template;

    private final JwtService jwtService;

    @Value(
            "${services.application-service.url:http://localhost:8082}"
    )
    private String applicationServiceUrl;

    @Value(
            "${services.policy-service.url:http://localhost:8085}"
    )
    private String policyServiceUrl;

    // =========================================================
    // CALCULATE RISK
    // =========================================================

    @Override
    public Mono<RiskAssessment> calculateRisk(
            RiskAssessmentRequest request
    ) {
        validateRiskAssessmentRequest(request);

        String applicationId =
                request.getApplicationId();

        String serviceToken =
                jwtService.generateServiceToken();

        log.info(
                "Starting risk assessment calculation for application {}",
                applicationId
        );

        int builderScore =
                resolveScore(
                        request.getBuilderScore(),
                        40,
                        95
                );

        int financialScore =
                resolveScore(
                        request.getFinancialScore(),
                        40,
                        95
                );

        int projectProgressScore =
                resolveScore(
                        request.getProjectProgressScore(),
                        35,
                        95
                );

        int locationScore =
                resolveScore(
                        request.getLocationScore(),
                        40,
                        95
                );

        int approvalScore =
                resolveScore(
                        request.getApprovalScore(),
                        40,
                        95
                );

        int finalRiskScore =
                calculateAverageScore(
                        builderScore,
                        financialScore,
                        projectProgressScore,
                        locationScore,
                        approvalScore
                );

        String category =
                determineRiskCategory(
                        finalRiskScore
                );

        double multiplier =
                determinePremiumMultiplier(
                        category,
                        finalRiskScore
                );

        String assessedBy =
                request.getAssessedBy();

        String remarks =
                String.format(
                        "Risk assessment calculated: "
                                + "Score=%d/100, "
                                + "Category=%s, "
                                + "Multiplier=%.2f "
                                + "(Builder=%d, Financial=%d, "
                                + "Progress=%d, Location=%d, "
                                + "Approval=%d)",
                        finalRiskScore,
                        category,
                        multiplier,
                        builderScore,
                        financialScore,
                        projectProgressScore,
                        locationScore,
                        approvalScore
                );

        log.info(
                "Risk result for application {}: "
                        + "Score={}, Category={}, Multiplier={}",
                applicationId,
                finalRiskScore,
                category,
                multiplier
        );

        /*
         * Validate the application before accessing or modifying
         * the Risk Service database.
         */
        return validateApplication(
                applicationId,
                serviceToken
        ).flatMap(application ->
                saveOrUpdateRiskAssessment(
                        applicationId,
                        finalRiskScore,
                        category,
                        multiplier,
                        remarks,
                        assessedBy
                )
        );
    }

    // =========================================================
    // SAVE OR UPDATE RISK ASSESSMENT
    // =========================================================

    private Mono<RiskAssessment> saveOrUpdateRiskAssessment(
            String applicationId,
            int finalRiskScore,
            String category,
            double multiplier,
            String remarks,
            String assessedBy
    ) {
        return riskAssessmentRepository
                .findByApplicationId(applicationId)

                .flatMap(existingAssessment -> {

                    checkAssessmentCanBeModified(
                            existingAssessment
                    );

                    log.info(
                            "Updating risk assessment {} for application {}",
                            existingAssessment.getAssessmentId(),
                            applicationId
                    );

                    updateAssessmentValues(
                            existingAssessment,
                            finalRiskScore,
                            category,
                            multiplier,
                            remarks,
                            assessedBy
                    );

                    existingAssessment.setNew(false);

                    return template.update(
                            existingAssessment
                    );
                })

                .switchIfEmpty(
                        Mono.defer(() -> {

                            log.info(
                                    "Creating risk assessment for application {}",
                                    applicationId
                            );

                            RiskAssessment assessment =
                                    new RiskAssessment();

                            assessment.setAssessmentId(
                                    UUID.randomUUID().toString()
                            );

                            assessment.setApplicationId(
                                    applicationId
                            );

                            updateAssessmentValues(
                                    assessment,
                                    finalRiskScore,
                                    category,
                                    multiplier,
                                    remarks,
                                    assessedBy
                            );

                            assessment.setNew(true);

                            return template
                                    .insert(RiskAssessment.class)
                                    .using(assessment);
                        })
                )

                .doOnSuccess(savedAssessment -> {
                    if (savedAssessment != null) {
                        log.info(
                                "Risk assessment saved: "
                                        + "ID={}, Score={}, Category={}",
                                savedAssessment.getAssessmentId(),
                                savedAssessment.getRiskScore(),
                                savedAssessment.getRiskCategory()
                        );
                    }
                });
    }

    // =========================================================
    // GET RISK ASSESSMENT
    // =========================================================

    @Override
    public Mono<RiskAssessment> getRiskAssessment(
            String applicationId
    ) {
        validateRequiredValue(
                applicationId,
                "Application ID is required"
        );

        log.info(
                "Fetching risk assessment for application {}",
                applicationId
        );

        return riskAssessmentRepository
                .findByApplicationId(applicationId)

                .switchIfEmpty(
                        Mono.error(
                                new RiskAssessmentNotFoundException(
                                        "No risk assessment found for application: "
                                                + applicationId
                                )
                        )
                )

                .doOnSuccess(result -> {
                    if (result != null) {
                        log.info(
                                "Found risk assessment {} for application {}",
                                result.getAssessmentId(),
                                applicationId
                        );
                    }
                });
    }

    // =========================================================
    // APPROVE RISK ASSESSMENT
    // =========================================================

    @Override
    public Mono<RiskAssessment> approveRiskAssessment(
            String assessmentId,
            ApproveRiskRequest request
    ) {
        validateRequiredValue(
                assessmentId,
                "Assessment ID is required"
        );

        validateApproveRequest(request);

        log.info(
                "Risk approval started for assessment {}",
                assessmentId
        );

        String serviceToken =
                jwtService.generateServiceToken();

        return riskAssessmentRepository
                .findById(assessmentId)

                .switchIfEmpty(
                        Mono.error(
                                new RiskAssessmentNotFoundException(
                                        "Risk assessment not found: "
                                                + assessmentId
                                )
                        )
                )

                .flatMap(assessment -> {

                    checkAssessmentCanBeModified(
                            assessment
                    );

                    assessment.setStatus(
                            RiskAssessmentStatus.APPROVED.name()
                    );

                    assessment.setRemarks(
                            request.getRemarks()
                    );

                    assessment.setAssessedBy(
                            request.getApprovedBy()
                    );

                    assessment.setAssessedAt(
                            LocalDateTime.now()
                    );

                    assessment.setNew(false);

                    return template
                            .update(assessment)

                            .flatMap(savedAssessment ->
                                    generateQuote(
                                            savedAssessment,
                                            serviceToken
                                    )
                            );
                })

                .doOnSuccess(savedAssessment -> {
                    if (savedAssessment != null) {
                        log.info(
                                "Risk assessment approved: ID={}",
                                assessmentId
                        );
                    }
                });
    }

    // =========================================================
    // DECLINE RISK ASSESSMENT
    // =========================================================

    @Override
    public Mono<RiskAssessment> declineRiskAssessment(
            String assessmentId,
            DeclineRiskRequest request
    ) {
        validateRequiredValue(
                assessmentId,
                "Assessment ID is required"
        );

        validateDeclineRequest(request);

        log.info(
                "Risk assessment decline started for assessment {}",
                assessmentId
        );

        return riskAssessmentRepository
                .findById(assessmentId)

                .switchIfEmpty(
                        Mono.error(
                                new RiskAssessmentNotFoundException(
                                        "Risk assessment not found: "
                                                + assessmentId
                                )
                        )
                )

                .flatMap(assessment -> {

                    checkAssessmentCanBeModified(
                            assessment
                    );

                    assessment.setStatus(
                            RiskAssessmentStatus.DECLINED.name()
                    );

                    assessment.setRemarks(
                            request.getRemarks()
                    );

                    assessment.setAssessedBy(
                            request.getDeclinedBy()
                    );

                    assessment.setAssessedAt(
                            LocalDateTime.now()
                    );

                    assessment.setNew(false);

                    return template.update(
                            assessment
                    );
                })

                .doOnSuccess(savedAssessment -> {
                    if (savedAssessment != null) {
                        log.info(
                                "Risk assessment declined: ID={}",
                                assessmentId
                        );
                    }
                });
    }

    // =========================================================
    // VALIDATE APPLICATION WITH APPLICATION SERVICE
    // =========================================================

    private Mono<ApplicationResponse> validateApplication(
            String applicationId,
            String serviceToken
    ) {
        String targetUri =
                applicationServiceUrl
                        + "/api/v1/applications/"
                        + applicationId;

        log.info(
                "Validating application {} with Application Service",
                applicationId
        );

        return webClient
                .get()

                .uri(targetUri)

                .headers(headers ->
                        headers.setBearerAuth(
                                serviceToken
                        )
                )

                .retrieve()

                /*
                 * Convert an Application Service 404 response into
                 * the Risk Service's ApplicationNotFoundException.
                 */
                .onStatus(
                        status -> status.value() == 404,
                        response ->
                                response
                                        .bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .flatMap(body ->
                                                Mono.error(
                                                        new ApplicationNotFoundException(
                                                                "Application not found: "
                                                                        + applicationId
                                                        )
                                                )
                                        )
                )

                /*
                 * Convert other downstream 4xx and 5xx responses
                 * into ApplicationServiceException.
                 */
                .onStatus(
                        HttpStatusCode::isError,
                        response ->
                                response
                                        .bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .flatMap(body ->
                                                Mono.error(
                                                        new ApplicationServiceException(
                                                                "Application Service rejected "
                                                                        + "the validation request"
                                                        )
                                                )
                                        )
                )

                .bodyToMono(
                        ApplicationResponse.class
                )

                .switchIfEmpty(
                        Mono.error(
                                new ApplicationNotFoundException(
                                        "Application not found: "
                                                + applicationId
                                )
                        )
                )

                .flatMap(application -> {

                    if (application.getApplicationId() == null
                            || !applicationId.equals(
                            application.getApplicationId()
                    )) {
                        return Mono.error(
                                new ApplicationServiceException(
                                        "Application ID returned by "
                                                + "Application Service does not match"
                                )
                        );
                    }

                    if (!"VERIFIED".equals(
                            application.getStatus()
                    )) {
                        return Mono.error(
                                new ApplicationNotEligibleForRiskException(
                                        "Application must be VERIFIED "
                                                + "before risk assessment. "
                                                + "Current status: "
                                                + application.getStatus()
                                )
                        );
                    }

                    log.info(
                            "Application {} is valid and VERIFIED",
                            applicationId
                    );

                    return Mono.just(
                            application
                    );
                })

                .onErrorMap(error -> {

                    if (error instanceof ApplicationNotFoundException
                            || error instanceof
                            ApplicationNotEligibleForRiskException
                            || error instanceof
                            ApplicationServiceException) {

                        return error;
                    }

                    log.error(
                            "Unable to validate application {} "
                                    + "with Application Service",
                            applicationId,
                            error
                    );

                    return new ApplicationServiceException(
                            "Unable to validate application "
                                    + "with Application Service",
                            error
                    );
                });
    }

    // =========================================================
    // GENERATE QUOTE THROUGH POLICY SERVICE
    // =========================================================

    private Mono<RiskAssessment> generateQuote(
            RiskAssessment assessment,
            String serviceToken
    ) {
        return validateApplication(
                assessment.getApplicationId(),
                serviceToken
        ).flatMap(application -> {
            QuoteGenerationRequest quoteRequest =
                    new QuoteGenerationRequest();

            quoteRequest.setApplicationId(
                    assessment.getApplicationId()
            );

            if (application.getPropertyValue() != null
                    && application.getPropertyValue().doubleValue() > 0) {
                quoteRequest.setCoverageAmount(
                        application.getPropertyValue().doubleValue()
                );
            }

            quoteRequest.setGeneratedBy(
                    assessment.getAssessedBy()
            );

            String targetUri =
                    policyServiceUrl
                            + "/api/v1/quotes/applications/"
                            + assessment.getApplicationId();

            log.info(
                    "Triggering dynamic quote generation at {} for application {} with propertyValue={}",
                    targetUri,
                    assessment.getApplicationId(),
                    quoteRequest.getCoverageAmount()
            );

            return webClient
                    .post()
                    .uri(targetUri)
                    .headers(headers ->
                            headers.setBearerAuth(
                                    serviceToken
                            )
                    )
                    .bodyValue(
                            quoteRequest
                    )
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response ->
                                    response
                                            .bodyToMono(String.class)
                                            .defaultIfEmpty("")
                                            .flatMap(body ->
                                                    Mono.error(
                                                            new PolicyServiceException(
                                                                    "Unable to generate quote"
                                                            )
                                                    )
                                            )
                    )
                    .bodyToMono(
                            Object.class
                    )
                    .thenReturn(
                            assessment
                    );
        }).onErrorMap(error -> {

            if (error instanceof PolicyServiceException) {
                return error;
            }

            log.error(
                    "Policy Service quote generation failed "
                            + "for application {}",
                    assessment.getApplicationId(),
                    error
            );

            return new PolicyServiceException(
                    "Unable to generate quote",
                    error
            );
        });
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRiskAssessmentRequest(
            RiskAssessmentRequest request
    ) {
        if (request == null) {
            throw new InvalidRiskAssessmentRequestException(
                    "Risk assessment request is required"
            );
        }

        validateRequiredValue(
                request.getApplicationId(),
                "Application ID is required"
        );

        validateRequiredValue(
                request.getAssessedBy(),
                "Assessed by is required"
        );

        validateScore(
                request.getBuilderScore(),
                "Builder score"
        );

        validateScore(
                request.getFinancialScore(),
                "Financial score"
        );

        validateScore(
                request.getProjectProgressScore(),
                "Project progress score"
        );

        validateScore(
                request.getLocationScore(),
                "Location score"
        );

        validateScore(
                request.getApprovalScore(),
                "Approval score"
        );
    }

    private void validateApproveRequest(
            ApproveRiskRequest request
    ) {
        if (request == null) {
            throw new InvalidRiskAssessmentRequestException(
                    "Approval request is required"
            );
        }

        validateRequiredValue(
                request.getRemarks(),
                "Remarks are required"
        );

        validateRequiredValue(
                request.getApprovedBy(),
                "Approved by is required"
        );
    }

    private void validateDeclineRequest(
            DeclineRiskRequest request
    ) {
        if (request == null) {
            throw new InvalidRiskAssessmentRequestException(
                    "Decline request is required"
            );
        }

        validateRequiredValue(
                request.getRemarks(),
                "Remarks are required"
        );

        validateRequiredValue(
                request.getDeclinedBy(),
                "Declined by is required"
        );
    }

    private void validateRequiredValue(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidRiskAssessmentRequestException(
                    message
            );
        }
    }

    private void validateScore(
            Integer score,
            String fieldName
    ) {
        if (score != null
                && (score < 0 || score > 100)) {

            throw new InvalidRiskAssessmentRequestException(
                    fieldName
                            + " must be between 0 and 100"
            );
        }
    }

    // =========================================================
    // FINAL-STATE PROTECTION
    // =========================================================

    private void checkAssessmentCanBeModified(
            RiskAssessment assessment
    ) {
        String currentStatus =
                assessment.getStatus();

        if (RiskAssessmentStatus.APPROVED.name()
                .equals(currentStatus)) {

            throw new RiskAssessmentAlreadyFinalizedException(
                    "Risk assessment is already finalized as APPROVED"
            );
        }

        if (RiskAssessmentStatus.DECLINED.name()
                .equals(currentStatus)) {

            throw new RiskAssessmentAlreadyFinalizedException(
                    "Risk assessment is already finalized as DECLINED"
            );
        }
    }

    // =========================================================
    // RISK CALCULATION HELPERS
    // =========================================================

    private int resolveScore(
            Integer providedScore,
            int minimumGeneratedValue,
            int maximumGeneratedValue
    ) {
        if (providedScore != null) {
            return providedScore;
        }

        return ThreadLocalRandom
                .current()
                .nextInt(
                        minimumGeneratedValue,
                        maximumGeneratedValue + 1
                );
    }

    private int calculateAverageScore(
            int builderScore,
            int financialScore,
            int projectProgressScore,
            int locationScore,
            int approvalScore
    ) {
        // Dynamic actuarial weighted health score:
        // Statutory Approvals & RERA (25%) + Builder Track Record (25%)
        // + Construction Progress (20%) + Financial Solvency (15%) + Location (15%)
        double weightedScore =
                (approvalScore * 0.25)
                        + (builderScore * 0.25)
                        + (projectProgressScore * 0.20)
                        + (financialScore * 0.15)
                        + (locationScore * 0.15);

        return (int) Math.round(
                weightedScore
        );
    }

    private String determineRiskCategory(
            int riskScore
    ) {
        if (riskScore >= 75) {
            return RiskCategory.LOW.name();
        }

        if (riskScore >= 50) {
            return RiskCategory.MEDIUM.name();
        }

        return RiskCategory.HIGH.name();
    }

    private double determinePremiumMultiplier(
            String category,
            int healthScore
    ) {
        if (RiskCategory.LOW.name().equals(category)) {
            // Low risk (Health 75-100): multiplier ranges between 0.85x and 1.00x
            double mult = 0.85 + ((100.0 - Math.max(75, healthScore)) / 25.0) * 0.15;
            return Math.round(mult * 100.0) / 100.0;
        }

        if (RiskCategory.MEDIUM.name().equals(category)) {
            // Medium risk (Health 50-74): multiplier ranges between 1.15x and 1.50x
            double mult = 1.15 + ((74.0 - Math.max(50, healthScore)) / 24.0) * 0.35;
            return Math.round(mult * 100.0) / 100.0;
        }

        // High risk (Health < 50): multiplier ranges between 1.75x and 2.60x
        double mult = 1.75 + ((49.0 - Math.max(0, healthScore)) / 49.0) * 0.85;
        return Math.round(mult * 100.0) / 100.0;
    }

    // =========================================================
    // ENTITY UPDATE HELPER
    // =========================================================

    private void updateAssessmentValues(
            RiskAssessment assessment,
            int riskScore,
            String category,
            double multiplier,
            String remarks,
            String assessedBy
    ) {
        assessment.setRiskScore(
                riskScore
        );

        assessment.setRiskCategory(
                category
        );

        assessment.setPremiumMultiplier(
                multiplier
        );

        assessment.setRemarks(
                remarks
        );

        assessment.setStatus(
                RiskAssessmentStatus.PENDING.name()
        );

        assessment.setAssessedBy(
                assessedBy
        );

        assessment.setAssessedAt(
                LocalDateTime.now()
        );
    }
}