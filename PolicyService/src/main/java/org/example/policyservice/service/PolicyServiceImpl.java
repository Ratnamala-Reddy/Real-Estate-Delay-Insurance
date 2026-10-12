package org.example.policyservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.policyservice.dto.ApplicationDto;
import org.example.policyservice.dto.CreatePolicyRequest;
import org.example.policyservice.dto.GenerateQuoteRequest;
import org.example.policyservice.dto.RiskAssessmentDto;
import org.example.policyservice.entity.Policy;
import org.example.policyservice.entity.Quote;
import org.example.policyservice.enums.PolicyStatus;
import org.example.policyservice.enums.QuoteStatus;
import org.example.policyservice.exception.ApplicationNotEligibleForQuoteException;
import org.example.policyservice.exception.ApplicationServiceException;
import org.example.policyservice.exception.InvalidPolicyRequestException;
import org.example.policyservice.exception.InvalidQuoteRequestException;
import org.example.policyservice.exception.PolicyAlreadyExistsException;
import org.example.policyservice.exception.PolicyAlreadyFinalizedException;
import org.example.policyservice.exception.PolicyNotFoundException;
import org.example.policyservice.exception.QuoteAlreadyFinalizedException;
import org.example.policyservice.exception.QuoteExpiredException;
import org.example.policyservice.exception.QuoteNotAcceptedException;
import org.example.policyservice.exception.QuoteNotFoundException;
import org.example.policyservice.exception.QuotesAlreadyGeneratedException;
import org.example.policyservice.exception.RiskAssessmentNotEligibleException;
import org.example.policyservice.exception.RiskServiceException;
import org.example.policyservice.repository.PolicyRepository;
import org.example.policyservice.repository.QuoteRepository;
import org.example.policyservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

        private final QuoteRepository quoteRepository;

        private final PolicyRepository policyRepository;

        private final R2dbcEntityTemplate template;

        private final WebClient webClient;

        private final JwtService jwtService;

        @Value("${services.application-service.url:http://localhost:8082}")
        private String applicationServiceUrl;

        @Value("${services.risk-service.url:http://localhost:8084}")
        private String riskServiceUrl;

        // =========================================================
        // GENERATE THREE QUOTES
        // =========================================================

        @Override
        public Flux<Quote> generateQuotes(
                        GenerateQuoteRequest request) {
                validateGenerateQuoteRequest(request);

                String applicationId = request.getApplicationId();

                String serviceToken = jwtService.generateServiceToken();

                log.info(
                                "Starting dynamic quote generation for application {}",
                                applicationId);

                return quoteRepository
                                .findByApplicationId(applicationId)
                                .collectList()
                                .flatMapMany(existingQuotes -> {

                                        boolean hasAccepted = existingQuotes.stream()
                                                        .anyMatch(q -> "ACCEPTED".equalsIgnoreCase(q.getStatus()));

                                        if (hasAccepted) {
                                                return Flux.error(
                                                                new QuotesAlreadyGeneratedException(
                                                                                "Quote has already been ACCEPTED for application: "
                                                                                                + applicationId));
                                        }

                                        Mono<Void> cleanupMono = existingQuotes.isEmpty()
                                                        ? Mono.empty()
                                                        : quoteRepository.deleteByApplicationId(applicationId);

                                        Mono<ApplicationDto> applicationMono = fetchApplication(
                                                        applicationId,
                                                        serviceToken);

                                        Mono<RiskAssessmentDto> riskMono = fetchRiskAssessment(
                                                        applicationId,
                                                        serviceToken);

                                        return cleanupMono.thenMany(
                                                        Mono.zip(
                                                                        applicationMono,
                                                                        riskMono).flatMapMany(tuple -> {

                                                                                ApplicationDto application = tuple
                                                                                                .getT1();

                                                                                RiskAssessmentDto risk = tuple.getT2();

                                                                                validateApplicationForQuote(
                                                                                                application,
                                                                                                applicationId);

                                                                                validateRiskForQuote(
                                                                                                risk,
                                                                                                applicationId);

                                                                                List<Quote> quotes = buildQuotes(
                                                                                                request,
                                                                                                application,
                                                                                                risk);

                                                                                return Flux
                                                                                                .fromIterable(quotes)
                                                                                                .concatMap(quote -> template
                                                                                                                .insert(Quote.class)
                                                                                                                .using(quote));
                                                                        }));
                                })

                                .doOnComplete(() -> log.info(
                                                "Dynamic quote generation completed "
                                                                + "for application {}",
                                                applicationId));
        }

        // =========================================================
        // GENERATE STANDARD QUOTE
        // =========================================================

        @Override
        public Mono<Quote> generateQuote(
                        GenerateQuoteRequest request) {
                return generateQuotes(request)
                                .filter(quote -> "STANDARD".equalsIgnoreCase(
                                                quote.getPlanName()))
                                .next();
        }

        // =========================================================
        // GET QUOTES
        // =========================================================

        @Override
        public Flux<Quote> getQuotes(
                        String applicationId) {
                return getQuotes(
                                applicationId,
                                null,
                                "ADMIN");
        }

        @Override
        public Flux<Quote> getQuotes(
                        String applicationId,
                        Integer callerUserId,
                        String callerRole) {
                validateQuoteRequiredValue(
                                applicationId,
                                "Application ID is required");

                log.info(
                                "Fetching quotes for application {} "
                                                + "by caller userId={}, role={}",
                                applicationId,
                                callerUserId,
                                callerRole);

                return verifyOwnership(
                                applicationId,
                                callerUserId,
                                callerRole).thenMany(
                                                quoteRepository.findByApplicationId(
                                                                applicationId));
        }

        // =========================================================
        // GET SINGLE QUOTE
        // =========================================================

        @Override
        public Mono<Quote> getQuote(
                        String quoteId) {
                return getQuote(
                                quoteId,
                                null,
                                "ADMIN");
        }

        @Override
        public Mono<Quote> getQuote(
                        String quoteId,
                        Integer callerUserId,
                        String callerRole) {
                validateQuoteRequiredValue(
                                quoteId,
                                "Quote ID is required");

                log.info(
                                "Fetching quote {} by caller userId={}, role={}",
                                quoteId,
                                callerUserId,
                                callerRole);

                return findQuote(quoteId)
                                .flatMap(quote -> verifyOwnership(
                                                quote.getApplicationId(),
                                                callerUserId,
                                                callerRole).thenReturn(quote));
        }

        // =========================================================
        // ACCEPT QUOTE
        // =========================================================

        @Override
        public Mono<Quote> acceptQuote(
                        String quoteId) {
                return acceptQuote(
                                quoteId,
                                null,
                                "ADMIN");
        }

        @Override
        public Mono<Quote> acceptQuote(
                        String quoteId,
                        Integer callerUserId,
                        String callerRole) {
                validateQuoteRequiredValue(
                                quoteId,
                                "Quote ID is required");

                log.info(
                                "Accepting quote {} by caller userId={}, role={}",
                                quoteId,
                                callerUserId,
                                callerRole);

                return findQuote(quoteId)

                                .flatMap(chosenQuote -> verifyOwnership(
                                                chosenQuote.getApplicationId(),
                                                callerUserId,
                                                callerRole).thenReturn(chosenQuote))

                                .flatMap(chosenQuote -> {

                                        validateQuoteCanBeFinalized(
                                                        chosenQuote);

                                        if (chosenQuote.getExpiryDate() != null
                                                        && chosenQuote.getExpiryDate()
                                                                        .isBefore(LocalDateTime.now())) {

                                                return Mono.error(
                                                                new QuoteExpiredException(
                                                                                "Quote has expired: "
                                                                                                + quoteId));
                                        }

                                        chosenQuote.setStatus(
                                                        QuoteStatus.ACCEPTED.name());

                                        chosenQuote.setNew(false);

                                        String applicationId = chosenQuote.getApplicationId();

                                        return quoteRepository
                                                        .findByApplicationId(applicationId)

                                                        .filter(otherQuote -> !otherQuote
                                                                        .getQuoteId()
                                                                        .equals(
                                                                                        chosenQuote.getQuoteId()))

                                                        .filter(otherQuote -> QuoteStatus.GENERATED.name()
                                                                        .equalsIgnoreCase(
                                                                                        otherQuote.getStatus()))

                                                        .concatMap(otherQuote -> {

                                                                log.info(
                                                                                "Auto-declining quote {} "
                                                                                                + "for application {}",
                                                                                otherQuote.getQuoteId(),
                                                                                applicationId);

                                                                otherQuote.setStatus(
                                                                                QuoteStatus.DECLINED.name());

                                                                otherQuote.setNew(false);

                                                                return template.update(
                                                                                otherQuote);
                                                        })

                                                        .then(
                                                                        template.update(
                                                                                        chosenQuote));
                                })

                                .doOnSuccess(savedQuote -> {
                                        if (savedQuote != null) {
                                                log.info(
                                                                "Quote {} accepted",
                                                                quoteId);
                                        }
                                });
        }

        // =========================================================
        // DECLINE QUOTE
        // =========================================================

        @Override
        public Mono<Quote> declineQuote(
                        String quoteId) {
                return declineQuote(
                                quoteId,
                                null,
                                "ADMIN");
        }

        @Override
        public Mono<Quote> declineQuote(
                        String quoteId,
                        Integer callerUserId,
                        String callerRole) {
                validateQuoteRequiredValue(
                                quoteId,
                                "Quote ID is required");

                log.info(
                                "Declining quote {} by caller userId={}, role={}",
                                quoteId,
                                callerUserId,
                                callerRole);

                return findQuote(quoteId)

                                .flatMap(quote -> verifyOwnership(
                                                quote.getApplicationId(),
                                                callerUserId,
                                                callerRole).thenReturn(quote))

                                .flatMap(quote -> {

                                        validateQuoteCanBeFinalized(
                                                        quote);

                                        quote.setStatus(
                                                        QuoteStatus.DECLINED.name());

                                        quote.setNew(false);

                                        return template.update(
                                                        quote);
                                })

                                .doOnSuccess(savedQuote -> {
                                        if (savedQuote != null) {
                                                log.info(
                                                                "Quote {} declined",
                                                                quoteId);
                                        }
                                });
        }

        // =========================================================
        // CREATE POLICY
        // =========================================================

        @Override
        public Mono<Policy> createPolicy(
                        CreatePolicyRequest request) {
                /*
                 * This overload represents an internal ADMIN operation.
                 * The future Payment Service should invoke this method
                 * only after successful payment verification.
                 */
                return createPolicy(
                                request,
                                null,
                                "ADMIN");
        }

        @Override
        public Mono<Policy> createPolicy(
                        CreatePolicyRequest request,
                        Integer callerUserId,
                        String callerRole) {
                validateCreatePolicyRequest(
                                request);

                String quoteId = request.getQuoteId();

                log.info(
                                "Creating policy for quote {} "
                                                + "using payment {}",
                                quoteId,
                                request.getPaymentId());

                return findQuote(quoteId)

                                .flatMap(quote -> verifyOwnership(
                                                quote.getApplicationId(),
                                                callerUserId,
                                                callerRole).thenReturn(quote))

                                .flatMap(quote -> {

                                        if (!QuoteStatus.ACCEPTED.name()
                                                        .equalsIgnoreCase(
                                                                        quote.getStatus())) {

                                                return Mono.error(
                                                                new QuoteNotAcceptedException(
                                                                                "Quote must be ACCEPTED "
                                                                                                + "before policy creation. "
                                                                                                + "Current status: "
                                                                                                + quote.getStatus()));
                                        }

                                        return policyRepository
                                                        .existsByQuoteId(
                                                                        quote.getQuoteId())

                                                        .flatMap(policyExists -> {

                                                                if (Boolean.TRUE.equals(
                                                                                policyExists)) {
                                                                        return Mono.error(
                                                                                        new PolicyAlreadyExistsException(
                                                                                                        "Policy already exists "
                                                                                                                        + "for quote: "
                                                                                                                        + quote.getQuoteId()));
                                                                }

                                                                Policy policy = buildPolicy(
                                                                                quote,
                                                                                request.getPaymentId());

                                                                return template
                                                                                .insert(Policy.class)
                                                                                .using(policy);
                                                        });
                                })

                                .doOnSuccess(savedPolicy -> {
                                        if (savedPolicy != null) {
                                                log.info(
                                                                "Policy created. "
                                                                                + "PolicyId={}, PolicyNumber={}, "
                                                                                + "PaymentId={}",
                                                                savedPolicy.getPolicyId(),
                                                                savedPolicy.getPolicyNumber(),
                                                                savedPolicy.getPaymentId());
                                        }
                                });
        }

        @Override
        public Flux<Policy> getPolicies() {

                log.info(
                                "Fetching all policies");

                return policyRepository.findAll();
        }

        @Override
        public Flux<Policy> getPolicies(
                        Integer callerUserId,
                        String callerRole) {
                log.info(
                                "Fetching policies by caller userId={}, role={}",
                                callerUserId,
                                callerRole);

                if ("ADMIN".equalsIgnoreCase(callerRole)) {
                        return policyRepository.findAll();
                }

                if ("BUYER".equalsIgnoreCase(callerRole)) {
                        if (callerUserId == null) {
                                return Flux.error(
                                                new AccessDeniedException(
                                                                "Caller user ID is missing"));
                        }

                        return policyRepository.findAll()
                                        .filterWhen(policy -> verifyOwnership(
                                                        policy.getApplicationId(),
                                                        callerUserId,
                                                        callerRole).thenReturn(true)
                                                        .onErrorReturn(false));
                }

                return policyRepository.findAll();
        }

        // =========================================================
        // GET SINGLE POLICY
        // =========================================================

        @Override
        public Mono<Policy> getPolicy(
                        String policyId) {
                return getPolicy(
                                policyId,
                                null,
                                "ADMIN");
        }

        @Override
        public Mono<Policy> getPolicy(
                        String policyId,
                        Integer callerUserId,
                        String callerRole) {
                validatePolicyRequiredValue(
                                policyId,
                                "Policy ID is required");

                log.info(
                                "Fetching policy {} by caller userId={}, role={}",
                                policyId,
                                callerUserId,
                                callerRole);

                return policyRepository
                                .findById(policyId)
                                .switchIfEmpty(policyRepository.findByPolicyNumber(policyId))
                                .switchIfEmpty(
                                                Mono.error(
                                                                new PolicyNotFoundException(
                                                                                "Policy not found with ID or Policy Number: "
                                                                                                + policyId)))

                                .flatMap(policy -> verifyOwnership(
                                                policy.getApplicationId(),
                                                callerUserId,
                                                callerRole).thenReturn(policy));
        }

        // =========================================================
        // COMPLETE POLICY
        // =========================================================

        @Override
        public Mono<Policy> completePolicy(
                        String policyId) {
                validatePolicyRequiredValue(
                                policyId,
                                "Policy ID is required");

                log.info(
                                "Completing policy {}",
                                policyId);

                return policyRepository
                                .findById(policyId)

                                .switchIfEmpty(
                                                Mono.error(
                                                                new PolicyNotFoundException(
                                                                                "Policy not found with ID: "
                                                                                                + policyId)))

                                .flatMap(policy -> {

                                        if (!PolicyStatus.ACTIVE.name()
                                                        .equalsIgnoreCase(
                                                                        policy.getPolicyStatus())) {

                                                return Mono.error(
                                                                new PolicyAlreadyFinalizedException(
                                                                                "Only ACTIVE policies can "
                                                                                                + "be completed. "
                                                                                                + "Current status: "
                                                                                                + policy.getPolicyStatus()));
                                        }

                                        policy.setPolicyStatus(
                                                        PolicyStatus.COMPLETED.name());

                                        policy.setNew(false);

                                        return template.update(
                                                        policy);
                                });
        }

        // =========================================================
        // CLOSE POLICY
        // =========================================================

        @Override
        public Mono<Policy> closePolicy(
                        String policyId) {
                validatePolicyRequiredValue(
                                policyId,
                                "Policy ID is required");

                log.info(
                                "Closing policy {}",
                                policyId);

                return policyRepository
                                .findById(policyId)

                                .switchIfEmpty(
                                                Mono.error(
                                                                new PolicyNotFoundException(
                                                                                "Policy not found with ID: "
                                                                                                + policyId)))

                                .flatMap(policy -> {

                                        String currentStatus = policy.getPolicyStatus();

                                        if (PolicyStatus.CLOSED.name()
                                                        .equalsIgnoreCase(currentStatus)
                                                        || PolicyStatus.CANCELLED.name()
                                                                        .equalsIgnoreCase(currentStatus)) {

                                                return Mono.error(
                                                                new PolicyAlreadyFinalizedException(
                                                                                "Policy is already finalized as "
                                                                                                + currentStatus));
                                        }

                                        policy.setPolicyStatus(
                                                        PolicyStatus.CLOSED.name());

                                        policy.setNew(false);

                                        return template.update(
                                                        policy);
                                });
        }

        // =========================================================
        // FETCH APPLICATION SERVICE
        // =========================================================

        private Mono<ApplicationDto> fetchApplication(
                        String applicationId,
                        String serviceToken) {
                String targetUri = applicationServiceUrl
                                + "/api/v1/applications/"
                                + applicationId;

                log.info(
                                "Fetching application {} from Application Service",
                                applicationId);

                return webClient
                                .get()
                                .uri(targetUri)
                                .headers(headers -> headers.setBearerAuth(
                                                serviceToken))
                                .retrieve()

                                .onStatus(
                                                status -> status.value() == 404,
                                                response -> Mono.error(
                                                                new ApplicationServiceException(
                                                                                "Application not found: "
                                                                                                + applicationId)))

                                .onStatus(
                                                status -> status.value() == 401,
                                                response -> Mono.error(
                                                                new ApplicationServiceException(
                                                                                "Application Service rejected "
                                                                                                + "the Policy Service token")))

                                .onStatus(
                                                status -> status.value() == 403,
                                                response -> Mono.error(
                                                                new ApplicationServiceException(
                                                                                "Policy Service is not authorized "
                                                                                                + "to access Application Service")))

                                .onStatus(
                                                HttpStatusCode::isError,
                                                response -> Mono.error(
                                                                new ApplicationServiceException(
                                                                                "Application Service rejected "
                                                                                                + "the request")))

                                .bodyToMono(
                                                ApplicationDto.class)

                                .switchIfEmpty(
                                                Mono.error(
                                                                new ApplicationServiceException(
                                                                                "Application not found: "
                                                                                                + applicationId)))

                                .onErrorMap(error -> {

                                        if (error instanceof ApplicationServiceException) {

                                                return error;
                                        }

                                        log.error(
                                                        "Unable to fetch application {} "
                                                                        + "from Application Service",
                                                        applicationId,
                                                        error);

                                        return new ApplicationServiceException(
                                                        "Unable to fetch application "
                                                                        + "from Application Service",
                                                        error);
                                });
        }

        // =========================================================
        // FETCH RISK SERVICE
        // =========================================================

        private Mono<RiskAssessmentDto> fetchRiskAssessment(
                        String applicationId,
                        String serviceToken) {
                String targetUri = riskServiceUrl
                                + "/api/v1/risk-assessments/applications/"
                                + applicationId
                                + "/risk-assessment";

                log.info(
                                "Fetching risk assessment for application {}",
                                applicationId);

                return webClient
                                .get()
                                .uri(targetUri)
                                .headers(headers -> headers.setBearerAuth(
                                                serviceToken))
                                .retrieve()

                                .onStatus(
                                                status -> status.value() == 404,
                                                response -> Mono.error(
                                                                new RiskServiceException(
                                                                                "Risk assessment not found "
                                                                                                + "for application: "
                                                                                                + applicationId)))

                                .onStatus(
                                                status -> status.value() == 401,
                                                response -> Mono.error(
                                                                new RiskServiceException(
                                                                                "Risk Service rejected "
                                                                                                + "the Policy Service token")))

                                .onStatus(
                                                status -> status.value() == 403,
                                                response -> Mono.error(
                                                                new RiskServiceException(
                                                                                "Policy Service is not authorized "
                                                                                                + "to access Risk Service")))

                                .onStatus(
                                                HttpStatusCode::isError,
                                                response -> Mono.error(
                                                                new RiskServiceException(
                                                                                "Risk Service rejected "
                                                                                                + "the request")))

                                .bodyToMono(
                                                RiskAssessmentDto.class)

                                .switchIfEmpty(
                                                Mono.error(
                                                                new RiskServiceException(
                                                                                "Risk assessment not found "
                                                                                                + "for application: "
                                                                                                + applicationId)))

                                .onErrorMap(error -> {

                                        if (error instanceof RiskServiceException) {
                                                return error;
                                        }

                                        log.error(
                                                        "Unable to fetch risk assessment "
                                                                        + "for application {}",
                                                        applicationId,
                                                        error);

                                        return new RiskServiceException(
                                                        "Unable to fetch risk assessment "
                                                                        + "from Risk Service",
                                                        error);
                                });
        }

        // =========================================================
        // OWNERSHIP VALIDATION
        // =========================================================

        private Mono<Void> verifyOwnership(
                        String applicationId,
                        Integer callerUserId,
                        String callerRole) {
                if ("ADMIN".equalsIgnoreCase(
                                callerRole)) {
                        return Mono.empty();
                }

                if (!"BUYER".equalsIgnoreCase(
                                callerRole)) {
                        return Mono.error(
                                        new AccessDeniedException(
                                                        "Unsupported caller role"));
                }

                if (callerUserId == null) {
                        return Mono.error(
                                        new AccessDeniedException(
                                                        "Caller user ID is missing"));
                }

                String serviceToken = jwtService.generateServiceToken();

                return fetchApplication(
                                applicationId,
                                serviceToken).flatMap(application -> {

                                        if (application.getUserId() == null
                                                        || !application
                                                                        .getUserId()
                                                                        .equals(callerUserId)) {

                                                log.warn(
                                                                "Ownership violation: caller userId={} "
                                                                                + "attempted application {} "
                                                                                + "owned by userId={}",
                                                                callerUserId,
                                                                applicationId,
                                                                application.getUserId());

                                                return Mono.error(
                                                                new AccessDeniedException(
                                                                                "You do not own this application"));
                                        }

                                        return Mono.empty();
                                });
        }

        // =========================================================
        // QUOTE LOOKUP
        // =========================================================

        private Mono<Quote> findQuote(
                        String quoteId) {
                return quoteRepository
                                .findById(quoteId)

                                .switchIfEmpty(
                                                Mono.defer(() -> quoteRepository.findByQuoteId(
                                                                quoteId)))

                                .switchIfEmpty(
                                                Mono.error(
                                                                new QuoteNotFoundException(
                                                                                "Quote not found with ID: "
                                                                                                + quoteId)));
        }

        // =========================================================
        // REQUEST VALIDATION
        // =========================================================

        private void validateGenerateQuoteRequest(
                        GenerateQuoteRequest request) {
                if (request == null) {
                        throw new InvalidQuoteRequestException(
                                        "Generate quote request is required");
                }

                validateQuoteRequiredValue(
                                request.getApplicationId(),
                                "Application ID is required");

                if (request.getGeneratedBy() == null || request.getGeneratedBy().isBlank()) {
                        request.setGeneratedBy("Underwriter Admin");
                }

                if (request.getCoverageAmount() == null && request.getBaseCoverageAmount() != null) {
                        request.setCoverageAmount(request.getBaseCoverageAmount());
                }

                if (request.getCoverageAmount() != null
                                && request.getCoverageAmount() <= 0) {

                        throw new InvalidQuoteRequestException(
                                        "Coverage amount must be greater than zero");
                }
        }

        private void validateCreatePolicyRequest(
                        CreatePolicyRequest request) {
                if (request == null) {
                        throw new InvalidPolicyRequestException(
                                        "Create policy request is required");
                }

                validatePolicyRequiredValue(
                                request.getQuoteId(),
                                "Quote ID is required");

                validatePolicyRequiredValue(
                                request.getPaymentId(),
                                "Payment ID is required");
        }

        private void validateQuoteRequiredValue(
                        String value,
                        String message) {
                if (value == null || value.isBlank()) {
                        throw new InvalidQuoteRequestException(
                                        message);
                }
        }

        private void validatePolicyRequiredValue(
                        String value,
                        String message) {
                if (value == null || value.isBlank()) {
                        throw new InvalidPolicyRequestException(
                                        message);
                }
        }

        // =========================================================
        // APPLICATION VALIDATION
        // =========================================================

        private void validateApplicationForQuote(
                        ApplicationDto application,
                        String requestedApplicationId) {
                if (application.getApplicationId() == null
                                || !requestedApplicationId.equals(
                                                application.getApplicationId())) {
                        throw new ApplicationServiceException(
                                        "Application ID returned by "
                                                        + "Application Service does not match");
                }

                if (!"VERIFIED".equalsIgnoreCase(
                                application.getStatus())) {
                        throw new ApplicationNotEligibleForQuoteException(
                                        "Application must be VERIFIED "
                                                        + "before quote generation. "
                                                        + "Current status: "
                                                        + application.getStatus());
                }

                if (application.getPropertyValue() == null
                                || application
                                                .getPropertyValue()
                                                .doubleValue() <= 0) {

                        throw new ApplicationNotEligibleForQuoteException(
                                        "Application property value must "
                                                        + "be greater than zero");
                }
        }

        // =========================================================
        // RISK VALIDATION
        // =========================================================

        private void validateRiskForQuote(
                        RiskAssessmentDto risk,
                        String requestedApplicationId) {
                if (risk.getApplicationId() == null
                                || !requestedApplicationId.equals(
                                                risk.getApplicationId())) {
                        throw new RiskServiceException(
                                        "Application ID returned by "
                                                        + "Risk Service does not match");
                }

                if (!"APPROVED".equalsIgnoreCase(
                                risk.getStatus())) {
                        throw new RiskAssessmentNotEligibleException(
                                        "Risk assessment must be APPROVED "
                                                        + "before quote generation. "
                                                        + "Current status: "
                                                        + risk.getStatus());
                }

                if (risk.getPremiumMultiplier() == null
                                || risk.getPremiumMultiplier() <= 0) {

                        throw new RiskAssessmentNotEligibleException(
                                        "Risk premium multiplier must "
                                                        + "be greater than zero");
                }
        }

        // =========================================================
        // QUOTE STATE VALIDATION
        // =========================================================

        private void validateQuoteCanBeFinalized(
                        Quote quote) {
                String status = quote.getStatus();

                if (QuoteStatus.ACCEPTED.name()
                                .equalsIgnoreCase(status)
                                || QuoteStatus.DECLINED.name()
                                                .equalsIgnoreCase(status)
                                || QuoteStatus.EXPIRED.name()
                                                .equalsIgnoreCase(status)) {

                        throw new QuoteAlreadyFinalizedException(
                                        "Quote is already finalized as "
                                                        + status);
                }

                if (!QuoteStatus.GENERATED.name()
                                .equalsIgnoreCase(status)) {

                        throw new QuoteAlreadyFinalizedException(
                                        "Only GENERATED quotes can be changed. "
                                                        + "Current status: "
                                                        + status);
                }
        }

        // =========================================================
        // BUILD THREE QUOTE PLANS
        // =========================================================

        private List<Quote> buildQuotes(
                        GenerateQuoteRequest request,
                        ApplicationDto application,
                        RiskAssessmentDto risk) {
                double propertyValue = 5000000.0;
                if (request.getCoverageAmount() != null && request.getCoverageAmount() > 0) {
                        propertyValue = request.getCoverageAmount();
                } else if (request.getBaseCoverageAmount() != null && request.getBaseCoverageAmount() > 0) {
                        propertyValue = request.getBaseCoverageAmount();
                } else if (application.getPropertyValue() != null && application.getPropertyValue().doubleValue() > 0) {
                        propertyValue = application.getPropertyValue().doubleValue();
                }

                String riskCategory = risk.getRiskCategory() != null
                                ? risk.getRiskCategory().trim().toUpperCase()
                                : "MEDIUM";

                int riskScore = risk.getRiskScore() != null
                                ? risk.getRiskScore()
                                : ("HIGH".equals(riskCategory) ? 40 : ("LOW".equals(riskCategory) ? 85 : 60));

                // Use the risk service's premium multiplier or compute dynamic actuarial rate
                double finalRiskMultiplier;
                if (risk.getPremiumMultiplier() != null && risk.getPremiumMultiplier() > 0) {
                        finalRiskMultiplier = risk.getPremiumMultiplier();
                } else if ("LOW".equals(riskCategory) || riskScore >= 75) {
                        finalRiskMultiplier = roundMoney(0.85 + ((100.0 - Math.max(75, riskScore)) / 25.0) * 0.15);
                } else if ("HIGH".equals(riskCategory) || riskScore < 50) {
                        finalRiskMultiplier = roundMoney(1.75 + ((49.0 - Math.max(0, riskScore)) / 49.0) * 0.85);
                } else {
                        finalRiskMultiplier = roundMoney(1.15 + ((74.0 - Math.max(50, riskScore)) / 24.0) * 0.35);
                }

                int baseGraceDays;
                if ("LOW".equals(riskCategory) || riskScore >= 75) {
                        baseGraceDays = 45;
                } else if ("HIGH".equals(riskCategory) || riskScore < 50) {
                        baseGraceDays = 90;
                } else {
                        baseGraceDays = 60;
                }

                // BASIC (Base Plan): strictly 4-digit premium (₹1,000 - ₹9,999) based on property value & risk
                double basicCoverage = Math.max(100000.0, Math.round(propertyValue * 0.50));
                double rawBasic = 2500.0 + (propertyValue * 0.00035 * finalRiskMultiplier);
                double basicPremium = Math.min(9900.0, Math.max(1200.0, Math.round(rawBasic)));
                Quote basic = buildQuote(
                                request.getApplicationId(),
                                "BASIC",
                                basicCoverage,
                                basicPremium,
                                18,
                                baseGraceDays + 30);

                // STANDARD: strictly 5-digit premium (₹10,000 - ₹48,000) based on property value & risk
                double standardCoverage = Math.max(250000.0, Math.round(propertyValue * 0.80));
                double rawStandard = 10000.0 + (propertyValue * 0.0012 * finalRiskMultiplier);
                double standardPremium = Math.min(48000.0, Math.max(10500.0, Math.round(rawStandard)));
                Quote standard = buildQuote(
                                request.getApplicationId(),
                                "STANDARD",
                                standardCoverage,
                                standardPremium,
                                24,
                                baseGraceDays);

                // PREMIUM: strictly 5-digit premium (₹20,000 - ₹88,000) based on property value & risk
                double premiumCoverage = Math.max(500000.0, Math.round(propertyValue * 1.00));
                double rawPremium = 18000.0 + (propertyValue * 0.0022 * finalRiskMultiplier);
                double premiumPremium = Math.min(88000.0, Math.max(20000.0, Math.round(rawPremium)));
                Quote premium = buildQuote(
                                request.getApplicationId(),
                                "PREMIUM",
                                premiumCoverage,
                                premiumPremium,
                                36,
                                Math.max(30, baseGraceDays - 15));

                return List.of(
                                basic,
                                standard,
                                premium);
        }

        private Quote buildQuote(
                        String applicationId,
                        String planName,
                        double coverage,
                        double premiumAmount,
                        int maximumDelayMonths,
                        int gracePeriodDays) {
                Quote quote = new Quote();

                quote.setQuoteId(
                                UUID.randomUUID().toString());

                quote.setApplicationId(
                                applicationId);

                quote.setPlanName(
                                planName);

                quote.setMaximumPayout(
                                coverage);

                quote.setPremiumAmount(
                                roundMoney(premiumAmount));

                quote.setMonthlyCompensation(
                                roundMoney(
                                                coverage
                                                                / maximumDelayMonths));

                quote.setMaximumDelayMonths(
                                maximumDelayMonths);

                quote.setGracePeriodDays(
                                gracePeriodDays);

                quote.setStatus(
                                QuoteStatus.GENERATED.name());

                quote.setCreatedAt(
                                LocalDateTime.now());

                quote.setExpiryDate(
                                LocalDateTime.now()
                                                .plusDays(30));

                quote.setNew(true);

                return quote;
        }

        private double resolveCoverage(
                        Double requestedCoverage,
                        double requestedMultiplier,
                        double propertyValue,
                        double propertyMultiplier,
                        double minimumCoverage) {
                if (requestedCoverage != null
                                && requestedCoverage > 0) {

                        return Math.round(
                                        requestedCoverage
                                                        * requestedMultiplier);
                }

                return Math.max(
                                minimumCoverage,
                                Math.round(
                                                propertyValue
                                                                * propertyMultiplier));
        }

        private double roundMoney(
                        double amount) {
                return Math.round(
                                amount * 100.0) / 100.0;
        }

        // =========================================================
        // BUILD POLICY
        // =========================================================

        private Policy buildPolicy(
                        Quote quote,
                        String paymentId) {
                Policy policy = new Policy();

                policy.setPolicyId(
                                UUID.randomUUID().toString());

                policy.setApplicationId(
                                quote.getApplicationId());

                policy.setQuoteId(
                                quote.getQuoteId());

                policy.setPaymentId(
                                paymentId);

                policy.setPolicyNumber(
                                "POL-"
                                                + UUID.randomUUID()
                                                                .toString()
                                                                .substring(0, 8)
                                                                .toUpperCase());

                policy.setCoverageAmount(
                                quote.getMaximumPayout());

                policy.setPremiumAmount(
                                quote.getPremiumAmount());

                policy.setPolicyStatus(
                                PolicyStatus.ACTIVE.name());

                policy.setStartDate(
                                LocalDate.now());

                policy.setEndDate(
                                LocalDate.now()
                                                .plusYears(2));

                policy.setCreatedAt(
                                LocalDateTime.now());

                policy.setNew(true);

                return policy;
        }
}