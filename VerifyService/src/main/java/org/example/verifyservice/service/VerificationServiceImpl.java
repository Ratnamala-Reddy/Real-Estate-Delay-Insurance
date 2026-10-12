package org.example.verifyservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.verifyservice.dto.AdditionalInfoRequest;
import org.example.verifyservice.dto.ApplicationResponse;
import org.example.verifyservice.dto.ApplicationStatusUpdateRequest;
import org.example.verifyservice.dto.ApprovalRequest;
import org.example.verifyservice.dto.PropertyVerificationRequest;
import org.example.verifyservice.dto.RejectVerificationRequest;
import org.example.verifyservice.dto.RiskAssessmentRequest;
import org.example.verifyservice.entity.VerificationRecord;
import org.example.verifyservice.enums.VerificationStatus;
import org.example.verifyservice.exception.*;
import org.example.verifyservice.repository.VerificationRepository;
import org.example.verifyservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class VerificationServiceImpl
        implements VerificationService {

    private final WebClient webClient;

    private final VerificationRepository verificationRepository;

    private final R2dbcEntityTemplate template;

    private final JwtService jwtService;

    @Value(
            "${services.application-service.url:http://localhost:8082}"
    )
    private String applicationServiceUrl;

    @Value(
            "${services.risk-service.url:http://localhost:8084}"
    )
    private String riskServiceUrl;

    // =========================================================
    // VALIDATION METHODS
    // =========================================================

    private void validateApplicationId(
            String applicationId
    ) {
        if (applicationId == null
                || applicationId.isBlank()) {

            throw new InvalidVerificationRequestException(
                    "Application ID is required"
            );
        }
    }

    private void validateRemarks(
            String remarks
    ) {
        if (remarks == null
                || remarks.isBlank()) {

            throw new InvalidVerificationRequestException(
                    "Remarks are required"
            );
        }
    }

    private void validateProcessedBy(
            String processedBy,
            String fieldName
    ) {
        if (processedBy == null
                || processedBy.isBlank()) {

            throw new InvalidVerificationRequestException(
                    fieldName + " is required"
            );
        }
    }

    // =========================================================
    // CREATE OR UPDATE VERIFICATION RECORD
    // =========================================================

    /**
     * Updates an existing verification record when one exists.
     * Otherwise, inserts a new verification record.
     */
    private Mono<VerificationRecord> upsertVerificationRecord(
            String applicationId,
            String status,
            String remarks,
            String verifiedBy
    ) {
        return verificationRepository
                .findByApplicationId(applicationId)

                .flatMap(existingRecord -> {

                    String currentStatus =
                            existingRecord.getVerificationStatus();

                    if (VerificationStatus.VERIFIED.name()
                            .equals(currentStatus)) {

                        return Mono.error(
                                new VerificationAlreadyCompletedException(
                                        "Verification is already completed as VERIFIED"
                                )
                        );
                    }

                    if (VerificationStatus.REJECTED.name()
                            .equals(currentStatus)) {

                        return Mono.error(
                                new VerificationAlreadyCompletedException(
                                        "Verification is already completed as REJECTED"
                                )
                        );
                    }

                    log.info(
                            "Found verification record {} for application {}. Updating record.",
                            existingRecord.getVerificationId(),
                            applicationId
                    );

                    existingRecord.setVerificationStatus(
                            status
                    );

                    existingRecord.setRemarks(
                            remarks
                    );

                    existingRecord.setVerifiedBy(
                            verifiedBy
                    );

                    existingRecord.setVerifiedAt(
                            LocalDateTime.now()
                    );

                    existingRecord.setNew(false);

                    return template.update(
                            existingRecord
                    );
                })

                .switchIfEmpty(
                        Mono.defer(() -> {

                            log.info(
                                    "No verification record found for application {}. Creating record.",
                                    applicationId
                            );

                            VerificationRecord newRecord =
                                    new VerificationRecord();

                            newRecord.setVerificationId(
                                    UUID.randomUUID().toString()
                            );

                            newRecord.setApplicationId(
                                    applicationId
                            );

                            newRecord.setVerificationStatus(
                                    status
                            );

                            newRecord.setRemarks(
                                    remarks
                            );

                            newRecord.setVerifiedBy(
                                    verifiedBy
                            );

                            newRecord.setVerifiedAt(
                                    LocalDateTime.now()
                            );

                            newRecord.setNew(true);

                            return template
                                    .insert(VerificationRecord.class)
                                    .using(newRecord);
                        })
                );
    }

    // =========================================================
    // GET VERIFICATION QUEUE
    // =========================================================

    @Override
    public Flux<ApplicationResponse> getVerificationQueue() {

        log.info(
                "Fetching verification queue from {}/api/v1/applications/all",
                applicationServiceUrl
        );

        String serviceToken =
                jwtService.generateServiceToken();

        return webClient
                .get()

                .uri(
                        applicationServiceUrl
                                + "/api/v1/applications/all"
                )

                .headers(headers ->
                        headers.setBearerAuth(
                                serviceToken
                        )
                )

                .retrieve()

                .bodyToFlux(
                        ApplicationResponse.class
                )

                .filter(application ->
                        "SUBMITTED".equals(
                                application.getStatus()
                        )
                )

                .doOnComplete(() ->
                        log.info(
                                "Verification queue fetched successfully"
                        )
                )

                .onErrorMap(error -> {

                    if (error
                            instanceof ApplicationServiceException) {

                        return error;
                    }

                    log.error(
                            "Failed to fetch verification queue",
                            error
                    );

                    return new ApplicationServiceException(
                            "Unable to fetch verification queue from Application Service",
                            error
                    );
                });
    }

    // =========================================================
    // PROPERTY VERIFICATION
    // =========================================================

    @Override
    public Mono<VerificationRecord> verifyProperty(
            String applicationId,
            PropertyVerificationRequest request
    ) {
        validateApplicationId(applicationId);

        if (request == null) {
            return Mono.error(
                    new InvalidVerificationRequestException(
                            "Property verification request is required"
                    )
            );
        }

        validateRemarks(
                request.getRemarks()
        );

        validateProcessedBy(
                request.getVerifiedBy(),
                "Verified by"
        );

        log.info(
                "Property verification started for application {}",
                applicationId
        );

        return upsertVerificationRecord(
                applicationId,
                VerificationStatus.PROPERTY_VERIFIED.name(),
                request.getRemarks(),
                request.getVerifiedBy()
        )

                .doOnSuccess(savedRecord -> {
                    if (savedRecord != null) {
                        log.info(
                                "Property verification record processed: {}",
                                savedRecord.getVerificationId()
                        );
                    }
                })

                .doOnError(error ->
                        log.error(
                                "Failed to process property verification record",
                                error
                        )
                );
    }

    // =========================================================
    // REQUEST ADDITIONAL INFORMATION
    // =========================================================

    @Override
    public Mono<VerificationRecord> requestInformation(
            String applicationId,
            AdditionalInfoRequest request
    ) {
        validateApplicationId(applicationId);

        if (request == null) {
            return Mono.error(
                    new InvalidVerificationRequestException(
                            "Additional information request is required"
                    )
            );
        }

        validateRemarks(
                request.getRemarks()
        );

        validateProcessedBy(
                request.getRequestedBy(),
                "Requested by"
        );

        log.info(
                "Additional information requested for application {}",
                applicationId
        );

        return upsertVerificationRecord(
                applicationId,
                VerificationStatus
                        .ADDITIONAL_INFORMATION_REQUIRED
                        .name(),
                request.getRemarks(),
                request.getRequestedBy()
        )

                .doOnSuccess(savedRecord -> {
                    if (savedRecord != null) {
                        log.info(
                                "Information request recorded: {}",
                                savedRecord.getVerificationId()
                        );
                    }
                })

                .doOnError(error ->
                        log.error(
                                "Failed to record information request",
                                error
                        )
                );
    }

    // =========================================================
    // APPROVE VERIFICATION
    // =========================================================

    @Override
    public Mono<VerificationRecord> approveVerification(
            String applicationId,
            ApprovalRequest request
    ) {
        validateApplicationId(applicationId);

        if (request == null) {
            return Mono.error(
                    new InvalidVerificationRequestException(
                            "Approval request is required"
                    )
            );
        }

        validateRemarks(
                request.getRemarks()
        );

        validateProcessedBy(
                request.getApprovedBy(),
                "Approved by"
        );

        log.info(
                "Verification approval started for application {}",
                applicationId
        );

        int builderScore =
                ThreadLocalRandom.current()
                        .nextInt(60, 101);

        int financialScore =
                ThreadLocalRandom.current()
                        .nextInt(60, 101);

        int projectProgressScore =
                ThreadLocalRandom.current()
                        .nextInt(60, 101);

        int locationScore =
                ThreadLocalRandom.current()
                        .nextInt(60, 101);

        int approvalScore =
                ThreadLocalRandom.current()
                        .nextInt(60, 101);

        log.info(
                "Generated risk scores: builder={}, financial={}, "
                        + "progress={}, location={}, approval={}",
                builderScore,
                financialScore,
                projectProgressScore,
                locationScore,
                approvalScore
        );

        RiskAssessmentRequest riskRequest =
                new RiskAssessmentRequest();

        riskRequest.setApplicationId(
                applicationId
        );

        riskRequest.setAssessedBy(
                request.getApprovedBy()
        );

        riskRequest.setBuilderScore(
                builderScore
        );

        riskRequest.setFinancialScore(
                financialScore
        );

        riskRequest.setProjectProgressScore(
                projectProgressScore
        );

        riskRequest.setLocationScore(
                locationScore
        );

        riskRequest.setApprovalScore(
                approvalScore
        );

        ApplicationStatusUpdateRequest statusRequest =
                new ApplicationStatusUpdateRequest();

        statusRequest.setStatus(
                "VERIFIED"
        );

        String serviceToken =
                jwtService.generateServiceToken();

        return upsertVerificationRecord(
                applicationId,
                VerificationStatus.VERIFIED.name(),
                request.getRemarks(),
                request.getApprovedBy()
        )

                // Update status in Application Service.
                .flatMap(savedRecord ->
                        webClient
                                .patch()

                                .uri(
                                        applicationServiceUrl
                                                + "/api/v1/applications/"
                                                + applicationId
                                                + "/status"
                                )

                                .headers(headers ->
                                        headers.setBearerAuth(
                                                serviceToken
                                        )
                                )

                                .bodyValue(
                                        statusRequest
                                )

                                .retrieve()

                                .bodyToMono(
                                        Object.class
                                )

                                .thenReturn(
                                        savedRecord
                                )

                                .doOnSuccess(result ->
                                        log.info(
                                                "Application status updated to VERIFIED for {}",
                                                applicationId
                                        )
                                )

                                .onErrorMap(error -> {

                                    if (error
                                            instanceof ApplicationServiceException) {

                                        return error;
                                    }

                                    log.error(
                                            "Failed to update application {} to VERIFIED",
                                            applicationId,
                                            error
                                    );

                                    return new ApplicationServiceException(
                                            "Unable to update application status to VERIFIED",
                                            error
                                    );
                                })
                )

                // Trigger risk assessment.
                .flatMap(savedRecord -> {

                    log.info(
                            "Triggering Risk Service for application {}",
                            applicationId
                    );

                    return webClient
                            .post()

                            .uri(
                                    riskServiceUrl
                                            + "/api/v1/risk-assessments"
                            )

                            .headers(headers ->
                                    headers.setBearerAuth(
                                            serviceToken
                                    )
                            )

                            .bodyValue(
                                    riskRequest
                            )

                            .retrieve()

                            .bodyToMono(
                                    Object.class
                            )

                            .thenReturn(
                                    savedRecord
                            )

                            .doOnSuccess(result ->
                                    log.info(
                                            "Risk assessment created successfully for {}",
                                            applicationId
                                    )
                            )

                            .onErrorMap(error -> {

                                if (error
                                        instanceof RiskServiceException) {

                                    return error;
                                }

                                log.error(
                                        "Failed to create risk assessment for {}",
                                        applicationId,
                                        error
                                );

                                return new RiskServiceException(
                                        "Unable to create risk assessment",
                                        error
                                );
                            });
                });
    }

    // =========================================================
    // REJECT APPLICATION
    // =========================================================

    @Override
    public Mono<VerificationRecord> rejectApplication(
            String applicationId,
            RejectVerificationRequest request
    ) {
        validateApplicationId(applicationId);

        if (request == null) {
            return Mono.error(
                    new InvalidVerificationRequestException(
                            "Rejection request is required"
                    )
            );
        }

        validateRemarks(
                request.getRemarks()
        );

        validateProcessedBy(
                request.getRejectedBy(),
                "Rejected by"
        );

        log.info(
                "Verification rejection started for application {}",
                applicationId
        );

        ApplicationStatusUpdateRequest statusRequest =
                new ApplicationStatusUpdateRequest();

        statusRequest.setStatus(
                "REJECTED"
        );

        String serviceToken =
                jwtService.generateServiceToken();

        return upsertVerificationRecord(
                applicationId,
                VerificationStatus.REJECTED.name(),
                request.getRemarks(),
                request.getRejectedBy()
        )

                .flatMap(savedRecord ->
                        webClient
                                .patch()

                                .uri(
                                        applicationServiceUrl
                                                + "/api/v1/applications/"
                                                + applicationId
                                                + "/status"
                                )

                                .headers(headers ->
                                        headers.setBearerAuth(
                                                serviceToken
                                        )
                                )

                                .bodyValue(
                                        statusRequest
                                )

                                .retrieve()

                                .bodyToMono(
                                        Object.class
                                )

                                .thenReturn(
                                        savedRecord
                                )

                                .doOnSuccess(result ->
                                        log.info(
                                                "Application status updated to REJECTED for {}",
                                                applicationId
                                        )
                                )

                                .onErrorMap(error -> {

                                    if (error
                                            instanceof ApplicationServiceException) {

                                        return error;
                                    }

                                    log.error(
                                            "Failed to update application {} to REJECTED",
                                            applicationId,
                                            error
                                    );

                                    return new ApplicationServiceException(
                                            "Unable to update application status to REJECTED",
                                            error
                                    );
                                })
                );
    }

    // =========================================================
    // GET VERIFICATION RESULT
    // =========================================================

    @Override
    public Mono<VerificationRecord> getVerificationResult(
            String applicationId
    ) {
        validateApplicationId(applicationId);

        log.info(
                "Fetching verification result for application {}",
                applicationId
        );

        return verificationRepository
                .findByApplicationId(
                        applicationId
                )

                .switchIfEmpty(
                        Mono.error(
                                new VerificationNotFoundException(
                                        "No verification record found for application: "
                                                + applicationId
                                )
                        )
                )

                .doOnSuccess(result ->
                        log.info(
                                "Verification result fetched successfully for application {}",
                                applicationId
                        )
                )

                .doOnError(error ->
                        log.error(
                                "Failed to fetch verification result for application {}",
                                applicationId,
                                error
                        )
                );
    }
}