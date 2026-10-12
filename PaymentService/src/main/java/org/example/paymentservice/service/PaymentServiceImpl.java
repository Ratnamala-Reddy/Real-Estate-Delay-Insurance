package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.ApplicationDto;
import org.example.paymentservice.dto.ClaimSummaryDto;
import org.example.paymentservice.dto.PolicySummaryDto;
import org.example.paymentservice.dto.CompensationPaymentRequest;
import org.example.paymentservice.dto.CompensationWebhookRequest;
import org.example.paymentservice.dto.PolicyActivationRequest;
import org.example.paymentservice.dto.PremiumPaymentRequest;
import org.example.paymentservice.dto.PremiumPaymentWebhookRequest;
import org.example.paymentservice.dto.QuoteDto;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.entity.PremiumPayment;
import org.example.paymentservice.enums.CompensationPaymentStatus;
import org.example.paymentservice.enums.PremiumPaymentStatus;
import org.example.paymentservice.exception.ApplicationServiceException;
import org.example.paymentservice.exception.InvalidPaymentRequestException;
import org.example.paymentservice.exception.InvalidPaymentStatusException;
import org.example.paymentservice.exception.PaymentAlreadyFinalizedException;
import org.example.paymentservice.exception.PaymentNotFoundException;
import org.example.paymentservice.exception.PolicyServiceException;
import org.example.paymentservice.exception.PremiumPaymentAlreadyExistsException;
import org.example.paymentservice.exception.QuoteNotEligibleForPaymentException;
import org.example.paymentservice.exception.QuoteServiceException;
import org.example.paymentservice.repository.CompensationPaymentRepository;
import org.example.paymentservice.repository.PremiumPaymentRepository;
import org.example.paymentservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PremiumPaymentRepository premiumPaymentRepository;

    private final CompensationPaymentRepository compensationPaymentRepository;

    private final R2dbcEntityTemplate template;

    private final WebClient webClient;

    private final JwtService jwtService;

    @Value(
            "${services.application-service.url:http://localhost:8082}"
    )
    private String applicationServiceUrl;

    @Value(
            "${services.policy-service.url:http://localhost:8085}"
    )
    private String policyServiceUrl;

    @Value(
            "${services.claim-service.url:http://localhost:8087}"
    )
    private String claimServiceUrl;

    // =========================================================
    // CREATE PREMIUM PAYMENT
    // =========================================================

    @Override
    public Mono<PremiumPayment> createPremiumPayment(
            PremiumPaymentRequest request,
            Integer callerUserId,
            String callerRole
    ) {
        validatePremiumPaymentRequest(request);

        if (!"BUYER".equalsIgnoreCase(callerRole)) {
            return Mono.error(
                    new AccessDeniedException(
                            "Only BUYER can create a premium payment"
                    )
            );
        }

        if (callerUserId == null) {
            return Mono.error(
                    new AccessDeniedException(
                            "Caller user ID is missing"
                    )
            );
        }

        String quoteId =
                request.getQuoteId();

        String serviceToken =
                jwtService.generateServiceToken();

        log.info(
                "Creating premium payment for quote {} "
                        + "by buyer userId={}",
                quoteId,
                callerUserId
        );

        return premiumPaymentRepository
                .existsByQuoteId(quoteId)

                .flatMap(paymentExists -> {

                    if (Boolean.TRUE.equals(paymentExists)) {
                        return Mono.error(
                                new PremiumPaymentAlreadyExistsException(
                                        "Premium payment already exists "
                                                + "for quote: "
                                                + quoteId
                                )
                        );
                    }

                    return fetchQuote(
                            quoteId,
                            serviceToken
                    );
                })

                .flatMap(quote -> {

                    validateQuoteForPayment(
                            quote,
                            quoteId
                    );

                    return fetchApplication(
                            quote.getApplicationId(),
                            serviceToken
                    ).map(application ->
                            new QuoteAndApplication(
                                    quote,
                                    application
                            )
                    );
                })

                .flatMap(result -> {

                    QuoteDto quote =
                            result.quote();

                    ApplicationDto application =
                            result.application();

                    if (application.getUserId() == null
                            || !application
                            .getUserId()
                            .equals(callerUserId)) {

                        log.warn(
                                "Payment ownership violation: "
                                        + "caller userId={} attempted quote {} "
                                        + "for application {} owned by userId={}",
                                callerUserId,
                                quote.getQuoteId(),
                                quote.getApplicationId(),
                                application.getUserId()
                        );

                        return Mono.error(
                                new AccessDeniedException(
                                        "You do not own the application "
                                                + "associated with this quote"
                                )
                        );
                    }

                    PremiumPayment payment =
                            buildPremiumPayment(
                                    request,
                                    quote
                            );

                    return template
                            .insert(PremiumPayment.class)
                            .using(payment);
                })

                .doOnSuccess(savedPayment -> {
                    if (savedPayment != null) {
                        log.info(
                                "Premium payment created. "
                                        + "PaymentId={}, QuoteId={}, Status={}",
                                savedPayment.getPaymentId(),
                                savedPayment.getQuoteId(),
                                savedPayment.getStatus()
                        );
                    }
                });
    }

    @Override
    public Flux<PremiumPayment> getAllPremiumPayments() {
        log.info("Fetching all premium payments from repository");
        return premiumPaymentRepository.findAll();
    }

    @Override
    public Flux<PremiumPayment> getAllPremiumPayments(
            Integer callerUserId,
            String callerRole
    ) {
        log.info("Fetching premium payments for caller userId={}, role={}", callerUserId, callerRole);
        if ("ADMIN".equalsIgnoreCase(callerRole)) {
            return premiumPaymentRepository.findAll();
        }

        if ("BUYER".equalsIgnoreCase(callerRole)) {
            if (callerUserId == null) {
                return Flux.empty();
            }
            String serviceToken = jwtService.generateServiceToken();
            return premiumPaymentRepository.findAll()
                    .filterWhen(payment -> isPremiumPaymentOwnedByCaller(payment, callerUserId, serviceToken));
        }

        return Flux.empty();
    }

    private Mono<Boolean> isPremiumPaymentOwnedByCaller(
            PremiumPayment payment,
            Integer callerUserId,
            String serviceToken
    ) {
        if (payment.getApplicationId() != null) {
            return fetchApplication(payment.getApplicationId(), serviceToken)
                    .map(app -> app.getUserId() != null && app.getUserId().equals(callerUserId))
                    .onErrorResume(e -> checkPremiumOwnershipViaQuote(payment.getQuoteId(), callerUserId, serviceToken));
        }
        return checkPremiumOwnershipViaQuote(payment.getQuoteId(), callerUserId, serviceToken);
    }

    private Mono<Boolean> checkPremiumOwnershipViaQuote(
            String quoteId,
            Integer callerUserId,
            String serviceToken
    ) {
        if (quoteId == null) {
            return Mono.just(false);
        }
        return fetchQuote(quoteId, serviceToken)
                .flatMap(quote -> {
                    if (quote.getApplicationId() == null) {
                        return Mono.just(false);
                    }
                    return fetchApplication(quote.getApplicationId(), serviceToken)
                            .map(app -> app.getUserId() != null && app.getUserId().equals(callerUserId))
                            .onErrorReturn(false);
                })
                .onErrorReturn(false);
    }

    // =========================================================
    // GET PREMIUM PAYMENT
    // =========================================================

    @Override
    public Mono<PremiumPayment> getPremiumPayment(
            String paymentId,
            Integer callerUserId,
            String callerRole
    ) {
        validateRequiredValue(
                paymentId,
                "Payment ID is required"
        );

        log.info(
                "Fetching premium payment {} "
                        + "by caller userId={}, role={}",
                paymentId,
                callerUserId,
                callerRole
        );

        return premiumPaymentRepository
                .findById(paymentId)

                .switchIfEmpty(
                        Mono.error(
                                new PaymentNotFoundException(
                                        "Premium payment not found with ID: "
                                                + paymentId
                                )
                        )
                )

                .flatMap(payment -> {

                    if ("ADMIN".equalsIgnoreCase(
                            callerRole
                    )) {
                        return Mono.just(payment);
                    }

                    if (!"BUYER".equalsIgnoreCase(
                            callerRole
                    )) {
                        return Mono.error(
                                new AccessDeniedException(
                                        "Unsupported caller role"
                                )
                        );
                    }

                    if (callerUserId == null) {
                        return Mono.error(
                                new AccessDeniedException(
                                        "Caller user ID is missing"
                                )
                        );
                    }

                    String serviceToken =
                            jwtService.generateServiceToken();

                    return fetchApplication(
                            payment.getApplicationId(),
                            serviceToken
                    ).flatMap(application -> {

                        if (application.getUserId() == null
                                || !application
                                .getUserId()
                                .equals(callerUserId)) {

                            return Mono.error(
                                    new AccessDeniedException(
                                            "You do not own this payment"
                                    )
                            );
                        }

                        return Mono.just(payment);
                    });
                });
    }

    // =========================================================
    // PROCESS PREMIUM PAYMENT WEBHOOK
    // =========================================================

    @Override
    public Mono<PremiumPayment> processPremiumWebhook(
            PremiumPaymentWebhookRequest request
    ) {
        validatePremiumWebhookRequest(request);

        String requestedStatus =
                normalizePremiumStatus(
                        request.getPaymentStatus()
                );

        String paymentId =
                request.getPaymentId();

        log.info(
                "Processing premium-payment webhook. "
                        + "PaymentId={}, Status={}",
                paymentId,
                requestedStatus
        );

        return premiumPaymentRepository
                .findById(paymentId)

                .switchIfEmpty(
                        Mono.error(
                                new PaymentNotFoundException(
                                        "Premium payment not found with ID: "
                                                + paymentId
                                )
                        )
                )

                .flatMap(payment -> {

                    validatePremiumPaymentCanBeProcessed(
                            payment
                    );

                    return premiumPaymentRepository
                            .existsByTransactionReference(
                                    request.getTransactionReference()
                            )

                            .flatMap(referenceExists -> {

                                boolean sameReference =
                                        request.getTransactionReference()
                                                .equals(
                                                        payment.getTransactionReference()
                                                );

                                if (Boolean.TRUE.equals(referenceExists)
                                        && !sameReference) {

                                    return Mono.error(
                                            new InvalidPaymentRequestException(
                                                    "Transaction reference "
                                                            + "is already in use"
                                            )
                                    );
                                }

                                payment.setTransactionReference(
                                        request.getTransactionReference()
                                );

                                payment.setStatus(
                                        requestedStatus
                                );

                                payment.setNew(false);

                                return premiumPaymentRepository
                                        .save(payment);
                            });
                })

                .flatMap(savedPayment -> {

                    if (!PremiumPaymentStatus.SUCCESS.name()
                            .equals(savedPayment.getStatus())) {

                        log.info(
                                "Premium payment {} finalized as {}",
                                savedPayment.getPaymentId(),
                                savedPayment.getStatus()
                        );

                        return Mono.just(savedPayment);
                    }

                    return createPolicyAfterSuccessfulPayment(
                            savedPayment
                    );
                });
    }

    // =========================================================
    // CREATE POLICY AFTER SUCCESS
    // =========================================================

    private Mono<PremiumPayment>
    createPolicyAfterSuccessfulPayment(
            PremiumPayment payment
    ) {
        PolicyActivationRequest activationRequest =
                new PolicyActivationRequest();

        activationRequest.setQuoteId(
                payment.getQuoteId()
        );

        activationRequest.setPaymentId(
                payment.getPaymentId()
        );

        String serviceToken =
                jwtService.generateServiceToken();

        String targetUri =
                policyServiceUrl
                        + "/api/v1/policies";

        log.info(
                "Triggering policy creation for payment {} "
                        + "and quote {}",
                payment.getPaymentId(),
                payment.getQuoteId()
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
                        activationRequest
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
                                                                "Policy Service failed "
                                                                        + "to create policy"
                                                        )
                                                )
                                        )
                )

                .bodyToMono(Object.class)

                .thenReturn(payment)

                .doOnSuccess(savedPayment ->
                        log.info(
                                "Policy creation completed "
                                        + "for payment {}",
                                savedPayment.getPaymentId()
                        )
                )

                .onErrorMap(error -> {

                    if (error instanceof
                            PolicyServiceException) {

                        return error;
                    }

                    log.error(
                            "Policy creation failed "
                                    + "for payment {}",
                            payment.getPaymentId(),
                            error
                    );

                    return new PolicyServiceException(
                            "Unable to create policy "
                                    + "after successful payment",
                            error
                    );
                });
    }

    // =========================================================
    // CREATE COMPENSATION PAYMENT
    // =========================================================

    @Override
    public Mono<CompensationPayment>
    createCompensationPayment(
            CompensationPaymentRequest request
    ) {
        validateCompensationPaymentRequest(
                request
        );

        log.info(
                "Creating compensation payment for claim {}",
                request.getClaimId()
        );

        CompensationPayment payment =
                new CompensationPayment();

        payment.setPaymentId(
                UUID.randomUUID().toString()
        );

        payment.setClaimId(
                request.getClaimId()
        );

        payment.setPolicyId(
                request.getPolicyId()
        );

        payment.setPayoutAmount(
                request.getPayoutAmount()
        );

        payment.setTransactionReference(
                "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        payment.setStatus(
                CompensationPaymentStatus.INITIATED.name()
        );

        payment.setCreatedAt(
                LocalDateTime.now()
        );

        payment.setNew(true);

        return template
                .insert(CompensationPayment.class)
                .using(payment)

                .doOnSuccess(savedPayment -> {
                    if (savedPayment != null) {
                        log.info(
                                "Compensation payment created. "
                                        + "PaymentId={}",
                                savedPayment.getPaymentId()
                        );
                    }
                });
    }

    // =========================================================
    // GET COMPENSATION PAYMENT
    // =========================================================

    @Override
    public Mono<CompensationPayment> getCompensationPayment(
            String paymentId
    ) {
        validateRequiredValue(
                paymentId,
                "Payment ID is required"
        );

        log.info(
                "Fetching compensation payment {}",
                paymentId
        );

        return compensationPaymentRepository
                .findById(paymentId)

                .switchIfEmpty(
                        Mono.error(
                                new PaymentNotFoundException(
                                        "Compensation payment not found with ID: "
                                                + paymentId
                                )
                        )
                );
    }

    // =========================================================
    // GET ALL COMPENSATION PAYMENTS (ADMIN)
    // =========================================================

    @Override
    public Flux<CompensationPayment> getAllCompensationPayments() {
        log.info("Fetching all compensation payments from repository");
        return compensationPaymentRepository.findAll();
    }

    @Override
    public Flux<CompensationPayment> getAllCompensationPayments(
            Integer callerUserId,
            String callerRole
    ) {
        log.info("Fetching compensation payments for caller userId={}, role={}", callerUserId, callerRole);
        if ("ADMIN".equalsIgnoreCase(callerRole)) {
            return compensationPaymentRepository.findAll();
        }

        if ("BUYER".equalsIgnoreCase(callerRole)) {
            if (callerUserId == null) {
                return Flux.empty();
            }
            String serviceToken = jwtService.generateServiceToken();
            return compensationPaymentRepository.findAll()
                    .filterWhen(payment -> isCompensationPaymentOwnedByCaller(payment, callerUserId, serviceToken));
        }

        return Flux.empty();
    }

    @Override
    public Mono<CompensationPayment> getCompensationPayment(
            String paymentId,
            Integer callerUserId,
            String callerRole
    ) {
        log.info("Fetching compensation payment {} for caller userId={}, role={}", paymentId, callerUserId, callerRole);
        return getCompensationPayment(paymentId)
                .flatMap(payment -> {
                    if ("ADMIN".equalsIgnoreCase(callerRole)) {
                        return Mono.just(payment);
                    }
                    if ("BUYER".equalsIgnoreCase(callerRole)) {
                        String serviceToken = jwtService.generateServiceToken();
                        return isCompensationPaymentOwnedByCaller(payment, callerUserId, serviceToken)
                                .flatMap(isOwned -> {
                                    if (Boolean.TRUE.equals(isOwned)) {
                                        return Mono.just(payment);
                                    }
                                    return Mono.error(new AccessDeniedException("You do not own this compensation payment"));
                                });
                    }
                    return Mono.error(new AccessDeniedException("Unsupported role"));
                });
    }

    private Mono<Boolean> isCompensationPaymentOwnedByCaller(
            CompensationPayment payment,
            Integer callerUserId,
            String serviceToken
    ) {
        if (callerUserId == null) {
            return Mono.just(false);
        }

        // 1. Check claim if present
        if (payment.getClaimId() != null) {
            String claimUri = claimServiceUrl + "/api/v1/claims/" + payment.getClaimId();
            return webClient.get()
                    .uri(claimUri)
                    .headers(h -> h.setBearerAuth(serviceToken))
                    .retrieve()
                    .bodyToMono(ClaimSummaryDto.class)
                    .flatMap(claim -> {
                        if (claim.getApplicationId() != null) {
                            return fetchApplication(claim.getApplicationId(), serviceToken)
                                    .map(app -> app.getUserId() != null && app.getUserId().equals(callerUserId))
                                    .onErrorResume(e -> checkCompensationOwnershipViaPolicy(payment.getPolicyId(), callerUserId, serviceToken));
                        }
                        if (claim.getPolicyId() != null) {
                            return checkCompensationOwnershipViaPolicy(claim.getPolicyId(), callerUserId, serviceToken);
                        }
                        return checkCompensationOwnershipViaPolicy(payment.getPolicyId(), callerUserId, serviceToken);
                    })
                    .onErrorResume(e -> checkCompensationOwnershipViaPolicy(payment.getPolicyId(), callerUserId, serviceToken));
        }

        // 2. Fallback to policy check
        return checkCompensationOwnershipViaPolicy(payment.getPolicyId(), callerUserId, serviceToken);
    }

    private Mono<Boolean> checkCompensationOwnershipViaPolicy(
            String policyId,
            Integer callerUserId,
            String serviceToken
    ) {
        if (policyId == null) {
            return Mono.just(false);
        }
        String policyUri = policyServiceUrl + "/api/v1/policies/" + policyId;
        return webClient.get()
                .uri(policyUri)
                .headers(h -> h.setBearerAuth(serviceToken))
                .retrieve()
                .bodyToMono(PolicySummaryDto.class)
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

    // =========================================================
    // PROCESS COMPENSATION WEBHOOK
    // =========================================================

    @Override
    public Mono<CompensationPayment>
    processCompensationWebhook(
            CompensationWebhookRequest request
    ) {
        validateCompensationWebhookRequest(
                request
        );

        String paymentStatus =
                normalizeCompensationStatus(
                        request.getPaymentStatus()
                );

        log.info(
                "Processing compensation webhook. "
                        + "PaymentId={}, Status={}",
                request.getPaymentId(),
                paymentStatus
        );

        return compensationPaymentRepository
                .findById(request.getPaymentId())

                .switchIfEmpty(
                        Mono.error(
                                new PaymentNotFoundException(
                                        "Compensation payment not found with ID: "
                                                + request.getPaymentId()
                                )
                        )
                )

                .flatMap(payment -> {

                    validateCompensationPaymentCanBeProcessed(
                            payment
                    );

                    payment.setTransactionReference(
                            request.getTransactionReference()
                    );

                    payment.setStatus(
                            paymentStatus
                    );

                    payment.setNew(false);

                    return compensationPaymentRepository
                            .save(payment);
                })

                .flatMap(savedPayment -> {

                    if (!CompensationPaymentStatus.PAID.name()
                            .equals(savedPayment.getStatus())) {

                        return Mono.just(savedPayment);
                    }

                    return closeClaimAfterCompensation(
                            savedPayment
                    );
                });
    }

    // =========================================================
    // CLOSE CLAIM AFTER COMPENSATION
    // =========================================================

    private Mono<CompensationPayment>
    closeClaimAfterCompensation(
            CompensationPayment payment
    ) {
        String serviceToken =
                jwtService.generateServiceToken();

        String targetUri =
                claimServiceUrl
                        + "/api/v1/claims/"
                        + payment.getClaimId()
                        + "/close";

        log.info(
                "Triggering claim closure for claim {}",
                payment.getClaimId()
        );

        return webClient
                .post()

                .uri(targetUri)

                .headers(headers ->
                        headers.setBearerAuth(
                                serviceToken
                        )
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
                                                        new IllegalStateException(
                                                                "Claim Service failed "
                                                                        + "to close claim"
                                                        )
                                                )
                                        )
                )

                .bodyToMono(Object.class)

                .thenReturn(payment);
    }

    // =========================================================
    // FETCH QUOTE FROM POLICY SERVICE
    // =========================================================

    private Mono<QuoteDto> fetchQuote(
            String quoteId,
            String serviceToken
    ) {
        String targetUri =
                policyServiceUrl
                        + "/api/v1/quotes/"
                        + quoteId;

        log.info(
                "Fetching quote {} from Policy Service",
                quoteId
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

                .onStatus(
                        status -> status.value() == 404,
                        response ->
                                Mono.error(
                                        new QuoteServiceException(
                                                "Quote not found: "
                                                        + quoteId
                                        )
                                )
                )

                .onStatus(
                        status -> status.value() == 401,
                        response ->
                                Mono.error(
                                        new QuoteServiceException(
                                                "Policy Service rejected "
                                                        + "the Payment Service token"
                                        )
                                )
                )

                .onStatus(
                        status -> status.value() == 403,
                        response ->
                                Mono.error(
                                        new QuoteServiceException(
                                                "Payment Service is not authorized "
                                                        + "to access Policy Service"
                                        )
                                )
                )

                .onStatus(
                        HttpStatusCode::isError,
                        response ->
                                Mono.error(
                                        new QuoteServiceException(
                                                "Policy Service failed "
                                                        + "to return quote"
                                        )
                                )
                )

                .bodyToMono(
                        QuoteDto.class
                )

                .switchIfEmpty(
                        Mono.error(
                                new QuoteServiceException(
                                        "Quote not found: "
                                                + quoteId
                                )
                        )
                )

                .onErrorMap(error -> {

                    if (error instanceof
                            QuoteServiceException) {

                        return error;
                    }

                    return new QuoteServiceException(
                            "Unable to fetch quote "
                                    + "from Policy Service",
                            error
                    );
                });
    }

    // =========================================================
    // FETCH APPLICATION
    // =========================================================

    private Mono<ApplicationDto> fetchApplication(
            String applicationId,
            String serviceToken
    ) {
        String targetUri =
                applicationServiceUrl
                        + "/api/v1/applications/"
                        + applicationId;

        log.info(
                "Fetching application {} from Application Service",
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

                .onStatus(
                        status -> status.value() == 404,
                        response ->
                                Mono.error(
                                        new ApplicationServiceException(
                                                "Application not found: "
                                                        + applicationId
                                        )
                                )
                )

                .onStatus(
                        status -> status.value() == 401,
                        response ->
                                Mono.error(
                                        new ApplicationServiceException(
                                                "Application Service rejected "
                                                        + "the Payment Service token"
                                        )
                                )
                )

                .onStatus(
                        status -> status.value() == 403,
                        response ->
                                Mono.error(
                                        new ApplicationServiceException(
                                                "Payment Service is not authorized "
                                                        + "to access Application Service"
                                        )
                                )
                )

                .onStatus(
                        HttpStatusCode::isError,
                        response ->
                                Mono.error(
                                        new ApplicationServiceException(
                                                "Application Service failed "
                                                        + "to return application"
                                        )
                                )
                )

                .bodyToMono(
                        ApplicationDto.class
                )

                .switchIfEmpty(
                        Mono.error(
                                new ApplicationServiceException(
                                        "Application not found: "
                                                + applicationId
                                )
                        )
                )

                .onErrorMap(error -> {

                    if (error instanceof
                            ApplicationServiceException) {

                        return error;
                    }

                    return new ApplicationServiceException(
                            "Unable to fetch application "
                                    + "from Application Service",
                            error
                    );
                });
    }

    // =========================================================
    // PREMIUM PAYMENT VALIDATION
    // =========================================================

    private void validatePremiumPaymentRequest(
            PremiumPaymentRequest request
    ) {
        if (request == null) {
            throw new InvalidPaymentRequestException(
                    "Premium payment request is required"
            );
        }

        validateRequiredValue(
                request.getQuoteId(),
                "Quote ID is required"
        );

        validateRequiredValue(
                request.getPaymentProvider(),
                "Payment provider is required"
        );
    }

    private void validatePremiumWebhookRequest(
            PremiumPaymentWebhookRequest request
    ) {
        if (request == null) {
            throw new InvalidPaymentRequestException(
                    "Premium payment webhook request is required"
            );
        }

        validateRequiredValue(
                request.getPaymentId(),
                "Payment ID is required"
        );

        validateRequiredValue(
                request.getTransactionReference(),
                "Transaction reference is required"
        );

        validateRequiredValue(
                request.getPaymentStatus(),
                "Payment status is required"
        );
    }

    private void validateQuoteForPayment(
            QuoteDto quote,
            String requestedQuoteId
    ) {
        if (quote.getQuoteId() == null
                || !requestedQuoteId.equals(
                quote.getQuoteId()
        )) {

            throw new QuoteServiceException(
                    "Quote ID returned by Policy Service "
                            + "does not match"
            );
        }

        if (quote.getApplicationId() == null
                || quote.getApplicationId().isBlank()) {

            throw new QuoteNotEligibleForPaymentException(
                    "Quote application ID is missing"
            );
        }

        if (!"ACCEPTED".equalsIgnoreCase(
                quote.getStatus()
        )) {
            throw new QuoteNotEligibleForPaymentException(
                    "Quote must be ACCEPTED "
                            + "before premium payment. "
                            + "Current status: "
                            + quote.getStatus()
            );
        }

        if (quote.getPremiumAmount() == null
                || quote.getPremiumAmount() <= 0) {

            throw new QuoteNotEligibleForPaymentException(
                    "Quote premium amount must "
                            + "be greater than zero"
            );
        }

        if (quote.getExpiryDate() != null
                && quote.getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            throw new QuoteNotEligibleForPaymentException(
                    "Quote has expired"
            );
        }
    }

    private void validatePremiumPaymentCanBeProcessed(
            PremiumPayment payment
    ) {
        String currentStatus =
                payment.getStatus();

        if (PremiumPaymentStatus.SUCCESS.name()
                .equalsIgnoreCase(currentStatus)
                || PremiumPaymentStatus.FAILED.name()
                .equalsIgnoreCase(currentStatus)
                || PremiumPaymentStatus.CANCELLED.name()
                .equalsIgnoreCase(currentStatus)
                || PremiumPaymentStatus.REFUNDED.name()
                .equalsIgnoreCase(currentStatus)) {

            throw new PaymentAlreadyFinalizedException(
                    "Premium payment is already finalized as "
                            + currentStatus
            );
        }

        if (!PremiumPaymentStatus.INITIATED.name()
                .equalsIgnoreCase(currentStatus)
                && !PremiumPaymentStatus.PENDING.name()
                .equalsIgnoreCase(currentStatus)) {

            throw new PaymentAlreadyFinalizedException(
                    "Premium payment cannot be processed "
                            + "from status: "
                            + currentStatus
            );
        }
    }

    private String normalizePremiumStatus(
            String status
    ) {
        String normalizedStatus =
                status.trim()
                        .toUpperCase(Locale.ROOT);

        if (!PremiumPaymentStatus.SUCCESS.name()
                .equals(normalizedStatus)
                && !PremiumPaymentStatus.FAILED.name()
                .equals(normalizedStatus)) {

            throw new InvalidPaymentStatusException(
                    "Premium payment webhook status "
                            + "must be SUCCESS or FAILED"
            );
        }

        return normalizedStatus;
    }

    // =========================================================
    // COMPENSATION VALIDATION
    // =========================================================

    private void validateCompensationPaymentRequest(
            CompensationPaymentRequest request
    ) {
        if (request == null) {
            throw new InvalidPaymentRequestException(
                    "Compensation payment request is required"
            );
        }

        validateRequiredValue(
                request.getClaimId(),
                "Claim ID is required"
        );

        validateRequiredValue(
                request.getPolicyId(),
                "Policy ID is required"
        );

        if (request.getPayoutAmount() == null
                || request.getPayoutAmount() <= 0) {

            throw new InvalidPaymentRequestException(
                    "Payout amount must be greater than zero"
            );
        }
    }

    private void validateCompensationWebhookRequest(
            CompensationWebhookRequest request
    ) {
        if (request == null) {
            throw new InvalidPaymentRequestException(
                    "Compensation webhook request is required"
            );
        }

        validateRequiredValue(
                request.getPaymentId(),
                "Payment ID is required"
        );

        validateRequiredValue(
                request.getTransactionReference(),
                "Transaction reference is required"
        );

        validateRequiredValue(
                request.getPaymentStatus(),
                "Payment status is required"
        );
    }

    private void validateCompensationPaymentCanBeProcessed(
            CompensationPayment payment
    ) {
        String currentStatus =
                payment.getStatus();

        if (CompensationPaymentStatus.PAID.name()
                .equalsIgnoreCase(currentStatus)
                || CompensationPaymentStatus.FAILED.name()
                .equalsIgnoreCase(currentStatus)) {

            throw new PaymentAlreadyFinalizedException(
                    "Compensation payment is already finalized as "
                            + currentStatus
            );
        }

        if (!CompensationPaymentStatus.INITIATED.name()
                .equalsIgnoreCase(currentStatus)
                && !CompensationPaymentStatus.PENDING.name()
                .equalsIgnoreCase(currentStatus)) {

            throw new PaymentAlreadyFinalizedException(
                    "Compensation payment cannot be processed "
                            + "from status: "
                            + currentStatus
            );
        }
    }

    private String normalizeCompensationStatus(
            String status
    ) {
        String normalizedStatus =
                status.trim()
                        .toUpperCase(Locale.ROOT);

        if ("SUCCESS".equals(normalizedStatus)) {
            normalizedStatus = CompensationPaymentStatus.PAID.name();
        }

        if (!CompensationPaymentStatus.PAID.name()
                .equals(normalizedStatus)
                && !CompensationPaymentStatus.FAILED.name()
                .equals(normalizedStatus)) {

            throw new InvalidPaymentStatusException(
                    "Compensation webhook status "
                            + "must be PAID, SUCCESS, or FAILED"
            );
        }

        return normalizedStatus;
    }

    // =========================================================
    // SHARED HELPERS
    // =========================================================

    private void validateRequiredValue(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidPaymentRequestException(
                    message
            );
        }
    }

    private PremiumPayment buildPremiumPayment(
            PremiumPaymentRequest request,
            QuoteDto quote
    ) {
        PremiumPayment payment =
                new PremiumPayment();

        payment.setPaymentId(
                UUID.randomUUID().toString()
        );

        payment.setQuoteId(
                quote.getQuoteId()
        );

        payment.setApplicationId(
                quote.getApplicationId()
        );

        payment.setPremiumAmount(
                quote.getPremiumAmount()
        );

        payment.setPaymentProvider(
                request.getPaymentProvider()
                        .trim()
                        .toUpperCase(Locale.ROOT)
        );

        payment.setTransactionReference(
                "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        payment.setStatus(
                PremiumPaymentStatus.INITIATED.name()
        );

        payment.setCreatedAt(
                LocalDateTime.now()
        );

        payment.setNew(true);

        return payment;
    }

    private record QuoteAndApplication(
            QuoteDto quote,
            ApplicationDto application
    ) {
    }
}