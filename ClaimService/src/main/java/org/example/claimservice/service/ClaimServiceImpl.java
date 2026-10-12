package org.example.claimservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.claimservice.dto.ApplicationDto;
import org.example.claimservice.dto.CompensationPaymentRequest;
import org.example.claimservice.dto.CreateClaimRequest;
import org.example.claimservice.dto.PolicyDto;
import org.example.claimservice.dto.QuoteDto;
import org.example.claimservice.entity.Claim;
import org.example.claimservice.enums.ClaimStatus;
import org.example.claimservice.exceptions.ApplicationServiceException;
import org.example.claimservice.exceptions.ClaimAlreadyExistsException;
import org.example.claimservice.exceptions.ClaimAlreadyFinalizedException;
import org.example.claimservice.exceptions.ClaimNotEligibleException;
import org.example.claimservice.exceptions.ClaimNotFoundException;
import org.example.claimservice.exceptions.InvalidClaimRequestException;
import org.example.claimservice.exceptions.InvalidClaimStateException;
import org.example.claimservice.exceptions.PaymentServiceException;
import org.example.claimservice.exceptions.PolicyNotEligibleForClaimException;
import org.example.claimservice.exceptions.PolicyServiceException;
import org.example.claimservice.exceptions.QuoteServiceException;
import org.example.claimservice.repository.ClaimRepository;
import org.example.claimservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimServiceImpl implements ClaimService {

    private final R2dbcEntityTemplate template;
    private final WebClient webClient;
    private final ClaimRepository claimRepository;
    private final JwtService jwtService;

    @Value("${services.policy-service.url:http://localhost:8085}")
    private String policyServiceUrl;

    @Value("${services.application-service.url:http://localhost:8082}")
    private String applicationServiceUrl;

    @Value("${services.payment-service.url:http://localhost:8086}")
    private String paymentServiceUrl;

    @Override
    public Mono<Claim> createClaim(
            CreateClaimRequest request,
            Integer callerUserId,
            String callerRole
    ) {
        validateCreateClaimRequest(request);

        if (!"BUYER".equalsIgnoreCase(callerRole)) {
            return Mono.error(new AccessDeniedException(
                    "Only BUYER can create a claim"
            ));
        }

        if (callerUserId == null) {
            return Mono.error(new AccessDeniedException(
                    "Caller user ID is missing"
            ));
        }

        String policyId = request.getPolicyId();
        String serviceToken = jwtService.generateServiceToken();

        log.info(
                "Creating claim for policy {} by buyer userId={}",
                policyId,
                callerUserId
        );

        return fetchPolicy(policyId, serviceToken)
                .flatMap(policy -> {
                    validatePolicyForClaim(policy, policyId);
                    String canonicalPolicyId = policy.getPolicyId();

                    return claimRepository.findByPolicyId(canonicalPolicyId)
                            .collectList()
                            .flatMap(existingClaims -> {
                                if (!existingClaims.isEmpty()) {
                                    boolean hasActive = existingClaims.stream().anyMatch(c ->
                                            !"CLOSED".equalsIgnoreCase(c.getStatus()) && !"REJECTED".equalsIgnoreCase(c.getStatus())
                                    );
                                    String polLabel = policy.getPolicyNumber() != null ? policy.getPolicyNumber() : canonicalPolicyId;
                                    if (hasActive) {
                                        return Mono.error(new ClaimAlreadyExistsException(
                                                "An active claim is already in progress for Policy " + polLabel + ". Please await adjudication before filing updates."
                                        ));
                                    } else {
                                        return Mono.error(new ClaimAlreadyExistsException(
                                                "A claim has already been submitted and finalized for Policy " + polLabel + ". Each insurance policy is limited to one claim."
                                        ));
                                    }
                                }

                                return fetchApplication(
                                        policy.getApplicationId(),
                                        serviceToken
                                ).map(application -> new PolicyAndApplication(
                                        policy,
                                        application
                                ));
                            });
                })
                .flatMap(result -> {
                    PolicyDto policy = result.policy();
                    ApplicationDto application = result.application();

                    if (application.getApplicationId() == null
                            || !application.getApplicationId()
                            .equals(policy.getApplicationId())) {
                        return Mono.error(new ApplicationServiceException(
                                "Application ID returned by Application Service "
                                        + "does not match policy"
                        ));
                    }

                    if (application.getUserId() == null
                            || !application.getUserId().equals(callerUserId)) {
                        return Mono.error(new AccessDeniedException(
                                "You do not own the application associated with this policy"
                        ));
                    }

                    LocalDate promisedDate =
                            application.getPromisedPossessionDate();

                    LocalDate actualDate =
                            request.getActualPossessionDate();

                    if (actualDate != null && promisedDate != null
                            && actualDate.isBefore(promisedDate)) {
                        return Mono.error(new ClaimNotEligibleException(
                                "Actual possession date cannot be before "
                                        + "the promised possession date"
                        ));
                    }

                    Claim claim = buildClaim(request, policy);
                    return template.insert(Claim.class).using(claim);
                })
                .doOnSuccess(savedClaim -> {
                    if (savedClaim != null) {
                        log.info(
                                "Claim created. ClaimId={}, PolicyId={}, Status={}",
                                savedClaim.getClaimId(),
                                savedClaim.getPolicyId(),
                                savedClaim.getStatus()
                        );
                    }
                });
    }

    @Override
    public reactor.core.publisher.Flux<Claim> getClaims() {
        return claimRepository.findAll();
    }

    @Override
    public reactor.core.publisher.Flux<Claim> getClaims(
            Integer callerUserId,
            String callerRole
    ) {
        log.info(
                "Fetching claims by caller userId={}, role={}",
                callerUserId,
                callerRole
        );

        if ("ADMIN".equalsIgnoreCase(callerRole)) {
            return claimRepository.findAll();
        }

        if ("BUYER".equalsIgnoreCase(callerRole)) {
            if (callerUserId == null) {
                return reactor.core.publisher.Flux.empty();
            }

            return claimRepository.findAll()
                    .filterWhen(claim -> isClaimOwnedByCaller(claim, callerUserId, callerRole));
        }

        return reactor.core.publisher.Flux.empty();
    }

    @Override
    public Mono<Claim> getClaim(String claimId) {
        return getClaim(claimId, null, "ADMIN");
    }

    @Override
    public Mono<Claim> getClaim(
            String claimId,
            Integer callerUserId,
            String callerRole
    ) {
        validateClaimId(claimId);

        return findClaimById(claimId)
                .flatMap(claim -> verifyClaimOwnership(
                        claim,
                        callerUserId,
                        callerRole
                ).then(Mono.just(claim)));
    }

    @Override
    public Mono<Claim> calculateDelay(String claimId) {
        validateClaimId(claimId);

        return findClaimById(claimId)
                .flatMap(claim -> {
                    validateClaimNotFinalized(claim);

                    if (!ClaimStatus.DOCUMENTS_SUBMITTED.name()
                            .equalsIgnoreCase(claim.getStatus())
                            && !ClaimStatus.DRAFT.name()
                            .equalsIgnoreCase(claim.getStatus())) {
                        return Mono.error(new InvalidClaimStateException(
                                "Delay can be calculated only for DRAFT or "
                                        + "DOCUMENTS_SUBMITTED claims. Current status: "
                                        + claim.getStatus()
                        ));
                    }

                    String token = jwtService.generateServiceToken();

                    return Mono.zip(
                            fetchApplication(claim.getApplicationId(), token),
                            fetchPolicy(claim.getPolicyId(), token)
                    ).flatMap(tuple -> {
                        ApplicationDto application = tuple.getT1();
                        PolicyDto policy = tuple.getT2();

                        validateClaimReferences(claim, policy, application);

                        LocalDate promisedDate =
                                application.getPromisedPossessionDate();

                        if (promisedDate == null) {
                            return Mono.error(new ClaimNotEligibleException(
                                    "Promised possession date is missing from application"
                            ));
                        }

                        LocalDate calculationDate =
                                claim.getActualPossessionDate() != null
                                        ? claim.getActualPossessionDate()
                                        : LocalDate.now();

                        long delayMonths = 0;
                        if (calculationDate.isAfter(promisedDate)) {
                            delayMonths = ChronoUnit.MONTHS.between(
                                    promisedDate,
                                    calculationDate
                            );
                            if (delayMonths == 0) {
                                delayMonths = 1;
                            }
                        } else {
                            // Delay reported via developer delay notice or ongoing project delay
                            delayMonths = Math.max(1, claim.getClaimedAmount() != null && claim.getClaimedAmount() > 0 ? 3 : 1);
                        }

                        claim.setEligibleDelayMonths(
                                Math.toIntExact(delayMonths)
                        );
                        claim.setStatus(ClaimStatus.DELAY_VERIFIED.name());
                        claim.setRemarks(
                                "Delay verified: " + delayMonths + " month(s)"
                        );
                        claim.setNew(false);

                        return template.update(claim);
                    });
                });
    }

    @Override
    public Mono<Claim> calculateCompensation(String claimId) {
        validateClaimId(claimId);

        return findClaimById(claimId)
                .flatMap(claim -> {
                    validateClaimNotFinalized(claim);

                    if (!ClaimStatus.DELAY_VERIFIED.name()
                            .equalsIgnoreCase(claim.getStatus())) {
                        return Mono.error(new InvalidClaimStateException(
                                "Delay must be verified before compensation calculation. "
                                        + "Current status: " + claim.getStatus()
                        ));
                    }

                    if (claim.getEligibleDelayMonths() == null) {
                        return Mono.error(new InvalidClaimStateException(
                                "Eligible delay months are missing"
                        ));
                    }

                    if (claim.getEligibleDelayMonths() <= 0) {
                        return Mono.error(new ClaimNotEligibleException(
                                "Claim is not eligible because there is no possession delay"
                        ));
                    }

                    String token = jwtService.generateServiceToken();

                    return fetchPolicy(claim.getPolicyId(), token)
                            .flatMap(policy -> {
                                validatePolicyForExistingClaim(claim, policy);
                                return fetchQuote(policy.getQuoteId(), token);
                            })
                            .flatMap(quote -> {
                                validateQuoteForCompensation(claim, quote);

                                int compensatedMonths = Math.min(
                                        claim.getEligibleDelayMonths(),
                                        quote.getMaximumDelayMonths()
                                );

                                double calculatedAmount =
                                        compensatedMonths
                                                * quote.getMonthlyCompensation();

                                double approvedAmount = Math.min(
                                        Math.min(
                                                calculatedAmount,
                                                quote.getMaximumPayout()
                                        ),
                                        claim.getClaimedAmount()
                                );

                                approvedAmount = roundMoney(approvedAmount);

                                if (approvedAmount <= 0) {
                                    return Mono.error(new ClaimNotEligibleException(
                                            "Calculated compensation must be greater than zero"
                                    ));
                                }

                                claim.setApprovedAmount(approvedAmount);
                                claim.setRemarks(
                                        "Compensation calculated for "
                                                + compensatedMonths
                                                + " eligible month(s)"
                                );
                                claim.setNew(false);

                                return template.update(claim);
                            });
                });
    }

    @Override
    public Mono<Claim> verifyInsuranceEligibility(String claimId) {
        validateClaimId(claimId);

        return findClaimById(claimId)
                .flatMap(claim -> {
                    validateClaimNotFinalized(claim);

                    if (!ClaimStatus.DELAY_VERIFIED.name()
                            .equalsIgnoreCase(claim.getStatus())) {
                        return Mono.error(new InvalidClaimStateException(
                                "Delay must be verified before insurance eligibility "
                                        + "verification. Current status: "
                                        + claim.getStatus()
                        ));
                    }

                    if (claim.getEligibleDelayMonths() == null
                            || claim.getEligibleDelayMonths() <= 0) {
                        return Mono.error(new ClaimNotEligibleException(
                                "Claim has no eligible delay"
                        ));
                    }

                    if (claim.getApprovedAmount() == null
                            || claim.getApprovedAmount() <= 0) {
                        return Mono.error(new InvalidClaimStateException(
                                "Compensation must be calculated before "
                                        + "insurance eligibility verification"
                        ));
                    }

                    String token = jwtService.generateServiceToken();

                    return fetchPolicy(claim.getPolicyId(), token)
                            .flatMap(policy -> {
                                validatePolicyForExistingClaim(claim, policy);

                                claim.setStatus(
                                        ClaimStatus.ELIGIBILITY_VERIFIED.name()
                                );
                                claim.setRemarks(
                                        "Insurance eligibility verified"
                                );
                                claim.setNew(false);

                                return template.update(claim);
                            });
                });
    }

    @Override
    public Mono<Claim> approveClaim(
            String claimId
    ) {
        validateClaimId(claimId);

        log.info(
                "Approving claim {}",
                claimId
        );

        return findClaimById(claimId)
                .flatMap(claim -> {

                    validateClaimNotFinalized(claim);

                    if (!ClaimStatus.ELIGIBILITY_VERIFIED.name()
                            .equalsIgnoreCase(
                                    claim.getStatus()
                            )) {

                        return Mono.error(
                                new InvalidClaimStateException(
                                        "Claim must be ELIGIBILITY_VERIFIED "
                                                + "before approval. "
                                                + "Current status: "
                                                + claim.getStatus()
                                )
                        );
                    }

                    if (claim.getApprovedAmount() == null
                            || claim.getApprovedAmount() <= 0) {

                        return Mono.error(
                                new InvalidClaimStateException(
                                        "Approved amount must be calculated "
                                                + "before claim approval"
                                )
                        );
                    }

                    CompensationPaymentRequest paymentRequest =
                            new CompensationPaymentRequest();

                    paymentRequest.setClaimId(
                            claim.getClaimId()
                    );

                    paymentRequest.setPolicyId(
                            claim.getPolicyId()
                    );

                    paymentRequest.setPayoutAmount(
                            claim.getApprovedAmount()
                    );

                    /*
                     * Create the compensation payment first.
                     * Update the claim to APPROVED only when
                     * Payment Service confirms creation.
                     */
                    return createCompensationPayment(
                            paymentRequest
                    ).flatMap(compensationPayment -> {

                        claim.setStatus(
                                ClaimStatus.APPROVED.name()
                        );

                        claim.setRemarks(
                                "Claim approved"
                        );

                        claim.setNew(false);

                        return template.update(claim);
                    });
                })
                .doOnSuccess(savedClaim -> {
                    if (savedClaim != null) {
                        log.info(
                                "Claim approved and compensation "
                                        + "payment created. "
                                        + "ClaimId={}",
                                savedClaim.getClaimId()
                        );
                    }
                })
                .doOnError(error ->
                        log.error(
                                "Claim approval failed. "
                                        + "ClaimId={}, Error={}",
                                claimId,
                                error.getMessage(),
                                error
                        )
                );
    }


    @Override
    public Mono<Claim> rejectClaim(String claimId) {
        validateClaimId(claimId);

        return findClaimById(claimId)
                .flatMap(claim -> {
                    validateClaimNotFinalized(claim);

                    claim.setStatus(ClaimStatus.REJECTED.name());
                    claim.setRemarks("Claim rejected");
                    claim.setNew(false);

                    return template.update(claim);
                });
    }

    @Override
    public Mono<Claim> closeClaim(String claimId) {
        validateClaimId(claimId);

        return findClaimById(claimId)
                .flatMap(claim -> {
                    if (ClaimStatus.CLOSED.name()
                            .equalsIgnoreCase(claim.getStatus())) {
                        return Mono.error(new ClaimAlreadyFinalizedException(
                                "Claim is already finalized as CLOSED"
                        ));
                    }

                    if (!ClaimStatus.APPROVED.name()
                            .equalsIgnoreCase(claim.getStatus())
                            && !ClaimStatus.SETTLED.name()
                            .equalsIgnoreCase(claim.getStatus())) {
                        return Mono.error(new InvalidClaimStateException(
                                "Only APPROVED or SETTLED claims can be closed. "
                                        + "Current status: " + claim.getStatus()
                        ));
                    }

                    claim.setStatus(ClaimStatus.CLOSED.name());
                    claim.setRemarks(
                            "Claim closed after compensation settlement"
                    );
                    claim.setNew(false);

                    return template.update(claim);
                });
    }

    private Mono<Boolean> isClaimOwnedByCaller(
            Claim claim,
            Integer callerUserId,
            String callerRole
    ) {
        if ("ADMIN".equalsIgnoreCase(callerRole)) {
            return Mono.just(true);
        }

        if (!"BUYER".equalsIgnoreCase(callerRole) || callerUserId == null) {
            return Mono.just(false);
        }

        String token = jwtService.generateServiceToken();

        if (claim.getApplicationId() != null) {
            return fetchApplication(claim.getApplicationId(), token)
                    .map(application -> application.getUserId() != null && application.getUserId().equals(callerUserId))
                    .onErrorResume(e -> checkClaimOwnershipViaPolicy(claim.getPolicyId(), callerUserId, token));
        }

        return checkClaimOwnershipViaPolicy(claim.getPolicyId(), callerUserId, token);
    }

    private Mono<Boolean> checkClaimOwnershipViaPolicy(
            String policyId,
            Integer callerUserId,
            String serviceToken
    ) {
        if (policyId == null) {
            return Mono.just(false);
        }

        return fetchPolicy(policyId, serviceToken)
                .flatMap(policy -> {
                    if (policy.getApplicationId() == null) {
                        return Mono.just(false);
                    }
                    return fetchApplication(policy.getApplicationId(), serviceToken)
                            .map(app -> app.getUserId() != null && app.getUserId().equals(callerUserId))
                            .onErrorReturn(false);
                })
                .onErrorReturn(false);
    }

    private Mono<Void> verifyClaimOwnership(
            Claim claim,
            Integer callerUserId,
            String callerRole
    ) {
        return isClaimOwnedByCaller(claim, callerUserId, callerRole)
                .flatMap(isOwned -> {
                    if (Boolean.TRUE.equals(isOwned)) {
                        return Mono.empty();
                    }
                    return Mono.error(new AccessDeniedException("You do not own this claim"));
                });
    }

    private Mono<Claim> findClaimById(String claimId) {
        return claimRepository.findById(claimId)
                .switchIfEmpty(Mono.error(
                        new ClaimNotFoundException(
                                "Claim not found with ID: " + claimId
                        )
                ));
    }

    private Mono<PolicyDto> fetchPolicy(
            String policyId,
            String serviceToken
    ) {
        String targetUri =
                policyServiceUrl + "/api/v1/policies/" + policyId;

        return webClient.get()
                .uri(targetUri)
                .headers(headers ->
                        headers.setBearerAuth(serviceToken)
                )
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> Mono.error(
                                new PolicyServiceException(
                                        "Policy not found: " + policyId
                                )
                        )
                )
                .onStatus(
                        status -> status.value() == 401,
                        response -> Mono.error(
                                new PolicyServiceException(
                                        "Policy Service rejected the Claim Service token"
                                )
                        )
                )
                .onStatus(
                        status -> status.value() == 403,
                        response -> Mono.error(
                                new PolicyServiceException(
                                        "Claim Service is not authorized to access Policy Service"
                                )
                        )
                )
                .onStatus(
                        HttpStatusCode::isError,
                        response -> Mono.error(
                                new PolicyServiceException(
                                        "Policy Service failed to return policy"
                                )
                        )
                )
                .bodyToMono(PolicyDto.class)
                .switchIfEmpty(Mono.error(
                        new PolicyServiceException(
                                "Policy not found: " + policyId
                        )
                ))
                .onErrorMap(error -> {
                    if (error instanceof PolicyServiceException) {
                        return error;
                    }

                    return new PolicyServiceException(
                            "Unable to fetch policy from Policy Service",
                            error
                    );
                });
    }

    private Mono<QuoteDto> fetchQuote(
            String quoteId,
            String serviceToken
    ) {
        String targetUri =
                policyServiceUrl + "/api/v1/quotes/" + quoteId;

        return webClient.get()
                .uri(targetUri)
                .headers(headers ->
                        headers.setBearerAuth(serviceToken)
                )
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> Mono.error(
                                new QuoteServiceException(
                                        "Quote not found: " + quoteId
                                )
                        )
                )
                .onStatus(
                        status -> status.value() == 401,
                        response -> Mono.error(
                                new QuoteServiceException(
                                        "Policy Service rejected the Claim Service token"
                                )
                        )
                )
                .onStatus(
                        status -> status.value() == 403,
                        response -> Mono.error(
                                new QuoteServiceException(
                                        "Claim Service is not authorized to access Policy Service"
                                )
                        )
                )
                .onStatus(
                        HttpStatusCode::isError,
                        response -> Mono.error(
                                new QuoteServiceException(
                                        "Policy Service failed to return quote"
                                )
                        )
                )
                .bodyToMono(QuoteDto.class)
                .switchIfEmpty(Mono.error(
                        new QuoteServiceException(
                                "Quote not found: " + quoteId
                        )
                ))
                .onErrorMap(error -> {
                    if (error instanceof QuoteServiceException) {
                        return error;
                    }

                    return new QuoteServiceException(
                            "Unable to fetch quote from Policy Service",
                            error
                    );
                });
    }

    private Mono<ApplicationDto> fetchApplication(
            String applicationId,
            String serviceToken
    ) {
        String targetUri =
                applicationServiceUrl
                        + "/api/v1/applications/"
                        + applicationId;

        return webClient.get()
                .uri(targetUri)
                .headers(headers ->
                        headers.setBearerAuth(serviceToken)
                )
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> Mono.error(
                                new ApplicationServiceException(
                                        "Application not found: " + applicationId
                                )
                        )
                )
                .onStatus(
                        status -> status.value() == 401,
                        response -> Mono.error(
                                new ApplicationServiceException(
                                        "Application Service rejected the Claim Service token"
                                )
                        )
                )
                .onStatus(
                        status -> status.value() == 403,
                        response -> Mono.error(
                                new ApplicationServiceException(
                                        "Claim Service is not authorized to access Application Service"
                                )
                        )
                )
                .onStatus(
                        HttpStatusCode::isError,
                        response -> Mono.error(
                                new ApplicationServiceException(
                                        "Application Service failed to return application"
                                )
                        )
                )
                .bodyToMono(ApplicationDto.class)
                .switchIfEmpty(Mono.error(
                        new ApplicationServiceException(
                                "Application not found: " + applicationId
                        )
                ))
                .onErrorMap(error -> {
                    if (error instanceof ApplicationServiceException) {
                        return error;
                    }

                    return new ApplicationServiceException(
                            "Unable to fetch application from Application Service",
                            error
                    );
                });
    }
    private Mono<Object> createCompensationPayment(
            CompensationPaymentRequest request
    ) {
        String serviceToken =
                jwtService.generateServiceToken();

        String targetUri =
                paymentServiceUrl
                        + "/api/v1/compensation-payments";

        log.info(
                "Calling Payment Service. "
                        + "ClaimId={}, PolicyId={}, "
                        + "PayoutAmount={}, URL={}",
                request.getClaimId(),
                request.getPolicyId(),
                request.getPayoutAmount(),
                targetUri
        );

        return webClient
                .post()
                .uri(targetUri)
                .headers(headers ->
                        headers.setBearerAuth(
                                serviceToken
                        )
                )
                .bodyValue(request)
                .retrieve()

                .onStatus(
                        HttpStatusCode::isError,
                        response -> response
                                .bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error(
                                            "Payment Service rejected "
                                                    + "compensation request. "
                                                    + "Status={} Body={}",
                                            response.statusCode(),
                                            body
                                    );

                                    return Mono.error(
                                            new PaymentServiceException(
                                                    "Payment Service returned "
                                                            + response.statusCode()
                                                            + ": "
                                                            + body
                                            )
                                    );
                                })
                )

                .bodyToMono(Object.class)

                .switchIfEmpty(
                        Mono.error(
                                new PaymentServiceException(
                                        "Payment Service returned "
                                                + "an empty response"
                                )
                        )
                )

                .doOnSuccess(response ->
                        log.info(
                                "Compensation payment created "
                                        + "successfully for claim {}",
                                request.getClaimId()
                        )
                )

                .onErrorMap(error -> {

                    if (error instanceof PaymentServiceException) {
                        return error;
                    }

                    log.error(
                            "Unable to call Payment Service "
                                    + "for claim {}",
                            request.getClaimId(),
                            error
                    );

                    return new PaymentServiceException(
                            "Unable to create compensation payment: "
                                    + error.getClass().getSimpleName()
                                    + ": "
                                    + error.getMessage(),
                            error
                    );
                });
    }


    private void validateCreateClaimRequest(
            CreateClaimRequest request
    ) {
        if (request == null) {
            throw new InvalidClaimRequestException(
                    "Create claim request is required"
            );
        }

        validateRequiredValue(
                request.getPolicyId(),
                "Policy ID is required"
        );

        validateRequiredValue(
                request.getReason(),
                "Claim reason is required"
        );

        if (request.getClaimedAmount() == null
                || request.getClaimedAmount() <= 0) {
            throw new InvalidClaimRequestException(
                    "Claimed amount must be greater than zero"
            );
        }
    }

    private void validateClaimId(String claimId) {
        validateRequiredValue(
                claimId,
                "Claim ID is required"
        );
    }

    private void validateRequiredValue(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidClaimRequestException(message);
        }
    }

    private void validatePolicyForClaim(
            PolicyDto policy,
            String requestedPolicyId
    ) {
        if (policy.getPolicyId() == null
                || (!requestedPolicyId.equalsIgnoreCase(policy.getPolicyId())
                    && !requestedPolicyId.equalsIgnoreCase(policy.getPolicyNumber()))) {
            throw new PolicyServiceException(
                    "Policy ID returned by Policy Service does not match"
            );
        }

        if (policy.getApplicationId() == null
                || policy.getApplicationId().isBlank()) {
            throw new PolicyNotEligibleForClaimException(
                    "Policy application ID is missing"
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(policy.getPolicyStatus())) {
            throw new PolicyNotEligibleForClaimException(
                    "Policy must be ACTIVE before claim creation. "
                            + "Current status: "
                            + policy.getPolicyStatus()
            );
        }

        LocalDate today = LocalDate.now();

        if (policy.getStartDate() != null
                && today.isBefore(policy.getStartDate())) {
            throw new PolicyNotEligibleForClaimException(
                    "Policy coverage has not started"
            );
        }

        if (policy.getEndDate() != null
                && today.isAfter(policy.getEndDate())) {
            throw new PolicyNotEligibleForClaimException(
                    "Policy coverage has expired"
            );
        }

        if (policy.getQuoteId() == null
                || policy.getQuoteId().isBlank()) {
            throw new PolicyNotEligibleForClaimException(
                    "Policy quote ID is missing"
            );
        }
    }

    private void validatePolicyForExistingClaim(
            Claim claim,
            PolicyDto policy
    ) {
        if (policy.getPolicyId() == null
                || !claim.getPolicyId().equals(policy.getPolicyId())) {
            throw new PolicyServiceException(
                    "Policy ID returned by Policy Service does not match claim"
            );
        }

        if (!claim.getApplicationId()
                .equals(policy.getApplicationId())) {
            throw new PolicyServiceException(
                    "Policy application ID does not match claim"
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(policy.getPolicyStatus())) {
            throw new PolicyNotEligibleForClaimException(
                    "Policy must remain ACTIVE during claim processing. "
                            + "Current status: "
                            + policy.getPolicyStatus()
            );
        }
    }

    private void validateClaimReferences(
            Claim claim,
            PolicyDto policy,
            ApplicationDto application
    ) {
        validatePolicyForExistingClaim(claim, policy);

        if (application.getApplicationId() == null
                || !claim.getApplicationId()
                .equals(application.getApplicationId())) {
            throw new ApplicationServiceException(
                    "Application ID returned by Application Service "
                            + "does not match claim"
            );
        }
    }

    private void validateQuoteForCompensation(
            Claim claim,
            QuoteDto quote
    ) {
        if (quote.getQuoteId() == null
                || quote.getQuoteId().isBlank()) {
            throw new QuoteServiceException(
                    "Quote ID returned by Policy Service is missing"
            );
        }

        if (!claim.getApplicationId()
                .equals(quote.getApplicationId())) {
            throw new QuoteServiceException(
                    "Quote application ID does not match claim"
            );
        }

        if (!"ACCEPTED".equalsIgnoreCase(quote.getStatus())) {
            throw new ClaimNotEligibleException(
                    "Associated quote must be ACCEPTED. "
                            + "Current status: "
                            + quote.getStatus()
            );
        }

        if (quote.getMonthlyCompensation() == null
                || quote.getMonthlyCompensation() <= 0) {
            throw new ClaimNotEligibleException(
                    "Quote monthly compensation must be greater than zero"
            );
        }

        if (quote.getMaximumDelayMonths() == null
                || quote.getMaximumDelayMonths() <= 0) {
            throw new ClaimNotEligibleException(
                    "Quote maximum delay months must be greater than zero"
            );
        }

        if (quote.getMaximumPayout() == null
                || quote.getMaximumPayout() <= 0) {
            throw new ClaimNotEligibleException(
                    "Quote maximum payout must be greater than zero"
            );
        }
    }

    private void validateClaimNotFinalized(Claim claim) {
        String status = claim.getStatus();

        if (ClaimStatus.APPROVED.name().equalsIgnoreCase(status)
                || ClaimStatus.REJECTED.name().equalsIgnoreCase(status)
                || ClaimStatus.SETTLED.name().equalsIgnoreCase(status)
                || ClaimStatus.CLOSED.name().equalsIgnoreCase(status)) {
            throw new ClaimAlreadyFinalizedException(
                    "Claim is already finalized as " + status
            );
        }
    }

    private Claim buildClaim(
            CreateClaimRequest request,
            PolicyDto policy
    ) {
        Claim claim = new Claim();

        claim.setClaimId(UUID.randomUUID().toString());
        claim.setPolicyId(policy.getPolicyId());
        claim.setApplicationId(policy.getApplicationId());
        claim.setReason(request.getReason().trim());
        claim.setClaimedAmount(
                roundMoney(request.getClaimedAmount())
        );
        claim.setActualPossessionDate(
                request.getActualPossessionDate()
        );
        claim.setStatus(ClaimStatus.DOCUMENTS_SUBMITTED.name());
        claim.setRemarks("Claim submitted for review");
        claim.setCreatedAt(LocalDateTime.now());
        claim.setNew(true);

        return claim;
    }

    private double roundMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    private record PolicyAndApplication(
            PolicyDto policy,
            ApplicationDto application
    ) {
    }
}
