package org.example.policyservice.service;

import org.example.policyservice.dto.CreatePolicyRequest;
import org.example.policyservice.entity.Policy;
import org.example.policyservice.entity.Quote;
import org.example.policyservice.enums.PolicyStatus;
import org.example.policyservice.enums.QuoteStatus;
import org.example.policyservice.exception.InvalidPolicyRequestException;
import org.example.policyservice.exception.PolicyAlreadyExistsException;
import org.example.policyservice.exception.PolicyAlreadyFinalizedException;
import org.example.policyservice.exception.PolicyNotFoundException;
import org.example.policyservice.exception.QuoteNotAcceptedException;
import org.example.policyservice.exception.QuoteNotFoundException;
import org.example.policyservice.repository.PolicyRepository;
import org.example.policyservice.repository.QuoteRepository;
import org.example.policyservice.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplPolicyTest {

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private WebClient webClient;

    @Mock
    private JwtService jwtService;

    @Mock
    private ReactiveInsertOperation.ReactiveInsert<Policy>
            policyInsert;

    private PolicyServiceImpl policyService;

    private static final String POLICY_ID =
            "policy-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String PAYMENT_ID =
            "payment-101";

    private static final String APPLICATION_ID =
            "application-101";

    @BeforeEach
    void setUp() {

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

        policyService =
                new PolicyServiceImpl(
                        quoteRepository,
                        policyRepository,
                        template,
                        webClient,
                        jwtService
                );

        ReflectionTestUtils.setField(
                policyService,
                "applicationServiceUrl",
                "http://localhost:8082"
        );

        ReflectionTestUtils.setField(
                policyService,
                "riskServiceUrl",
                "http://localhost:8084"
        );
    }

    @AfterEach
    void tearDown() {
        StepVerifier.resetDefaultTimeout();
    }

    // =========================================================
    // CREATE POLICY REQUEST VALIDATION
    // =========================================================

    @Test
    void createPolicy_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.createPolicy(null)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPolicyRequestException
                                && "Create policy request is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(quoteRepository, never())
                .findById(any(String.class));
    }

    @Test
    void createPolicy_ShouldThrowException_WhenQuoteIdIsNull() {

        CreatePolicyRequest request =
                createPolicyRequest();

        request.setQuoteId(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.createPolicy(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPolicyRequestException
                                && "Quote ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(quoteRepository, never())
                .findById(any(String.class));
    }

    @Test
    void createPolicy_ShouldThrowException_WhenQuoteIdIsBlank() {

        CreatePolicyRequest request =
                createPolicyRequest();

        request.setQuoteId("   ");

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.createPolicy(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPolicyRequestException
                                && "Quote ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(quoteRepository, never())
                .findById(any(String.class));
    }

    @Test
    void createPolicy_ShouldThrowException_WhenPaymentIdIsNull() {

        CreatePolicyRequest request =
                createPolicyRequest();

        request.setPaymentId(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.createPolicy(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPolicyRequestException
                                && "Payment ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(quoteRepository, never())
                .findById(any(String.class));
    }

    @Test
    void createPolicy_ShouldThrowException_WhenPaymentIdIsBlank() {

        CreatePolicyRequest request =
                createPolicyRequest();

        request.setPaymentId("");

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.createPolicy(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPolicyRequestException
                                && "Payment ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(quoteRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // CREATE POLICY QUOTE LOOKUP
    // =========================================================

    @Test
    void createPolicy_ShouldThrowNotFound_WhenQuoteDoesNotExist() {

        CreatePolicyRequest request =
                createPolicyRequest();

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.empty()
        );

        when(quoteRepository.findByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        policyService.createPolicy(request)
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteNotFoundException
                                && (
                                "Quote not found with ID: "
                                        + QUOTE_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(policyRepository, never())
                .existsByQuoteId(any(String.class));

        verify(template, never())
                .insert(Policy.class);
    }

    @Test
    void createPolicy_ShouldRejectGeneratedQuote() {

        CreatePolicyRequest request =
                createPolicyRequest();

        Quote quote =
                createQuote(
                        QuoteStatus.GENERATED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        policyService.createPolicy(request)
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteNotAcceptedException
                                && (
                                "Quote must be ACCEPTED "
                                        + "before policy creation. "
                                        + "Current status: GENERATED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(policyRepository, never())
                .existsByQuoteId(any(String.class));

        verify(template, never())
                .insert(Policy.class);
    }

    @Test
    void createPolicy_ShouldRejectDeclinedQuote() {

        CreatePolicyRequest request =
                createPolicyRequest();

        Quote quote =
                createQuote(
                        QuoteStatus.DECLINED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        policyService.createPolicy(request)
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteNotAcceptedException
                                && (
                                "Quote must be ACCEPTED "
                                        + "before policy creation. "
                                        + "Current status: DECLINED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(policyRepository, never())
                .existsByQuoteId(any(String.class));

        verify(template, never())
                .insert(Policy.class);
    }

    // =========================================================
    // DUPLICATE POLICY
    // =========================================================

    @Test
    void createPolicy_ShouldThrowException_WhenPolicyAlreadyExists() {

        CreatePolicyRequest request =
                createPolicyRequest();

        Quote quote =
                createQuote(
                        QuoteStatus.ACCEPTED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        when(policyRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(true)
        );

        StepVerifier.create(
                        policyService.createPolicy(request)
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyAlreadyExistsException
                                && (
                                "Policy already exists for quote: "
                                        + QUOTE_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(policyRepository)
                .existsByQuoteId(QUOTE_ID);

        verify(template, never())
                .insert(Policy.class);
    }

    // =========================================================
    // CREATE POLICY SUCCESS
    // =========================================================

    @Test
    void createPolicy_ShouldCreateActivePolicy_FromAcceptedQuote() {

        CreatePolicyRequest request =
                createPolicyRequest();

        Quote quote =
                createQuote(
                        QuoteStatus.ACCEPTED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        when(policyRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(false)
        );

        when(template.insert(
                Policy.class
        )).thenReturn(
                policyInsert
        );

        when(policyInsert.using(
                any(Policy.class)
        )).thenAnswer(invocation -> {

            Policy policy =
                    invocation.getArgument(0);

            return Mono.just(policy);
        });

        StepVerifier.create(
                        policyService.createPolicy(request)
                )
                .assertNext(policy -> {

                    assertNotNull(
                            policy.getPolicyId()
                    );

                    assertNotNull(
                            policy.getPolicyNumber()
                    );

                    assertTrue(
                            policy.getPolicyNumber()
                                    .startsWith("POL-")
                    );

                    assertEquals(
                            QUOTE_ID,
                            policy.getQuoteId()
                    );

                    assertEquals(
                            PAYMENT_ID,
                            policy.getPaymentId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            policy.getApplicationId()
                    );

                    assertEquals(
                            1200000.0,
                            policy.getCoverageAmount()
                    );

                    assertEquals(
                            37500.0,
                            policy.getPremiumAmount()
                    );

                    assertEquals(
                            PolicyStatus.ACTIVE.name(),
                            policy.getPolicyStatus()
                    );

                    assertEquals(
                            LocalDate.now(),
                            policy.getStartDate()
                    );

                    assertEquals(
                            LocalDate.now().plusYears(2),
                            policy.getEndDate()
                    );

                    assertNotNull(
                            policy.getCreatedAt()
                    );

                    assertTrue(
                            policy.isNew()
                    );
                })
                .verifyComplete();

        verify(policyRepository)
                .existsByQuoteId(QUOTE_ID);

        verify(policyInsert)
                .using(any(Policy.class));
    }

    // =========================================================
    // GET ALL POLICIES
    // =========================================================

    @Test
    void getPolicies_ShouldReturnAllPolicies() {

        Policy activePolicy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        Policy completedPolicy =
                createPolicy(
                        PolicyStatus.COMPLETED.name()
                );

        completedPolicy.setPolicyId(
                "policy-102"
        );

        when(policyRepository.findAll())
                .thenReturn(
                        Flux.just(
                                activePolicy,
                                completedPolicy
                        )
                );

        StepVerifier.create(
                        policyService.getPolicies()
                )
                .expectNext(
                        activePolicy,
                        completedPolicy
                )
                .verifyComplete();

        verify(policyRepository)
                .findAll();
    }

    @Test
    void getPolicies_ShouldCompleteEmpty_WhenNoPoliciesExist() {

        when(policyRepository.findAll())
                .thenReturn(
                        Flux.empty()
                );

        StepVerifier.create(
                        policyService.getPolicies()
                )
                .verifyComplete();

        verify(policyRepository)
                .findAll();
    }

    // =========================================================
    // GET SINGLE POLICY
    // =========================================================

    @Test
    void getPolicy_ShouldReturnPolicy_WhenCallerIsAdmin() {

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyService.getPolicy(
                                POLICY_ID,
                                1,
                                "ADMIN"
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            POLICY_ID,
                            result.getPolicyId()
                    );

                    assertEquals(
                            PAYMENT_ID,
                            result.getPaymentId()
                    );

                    assertEquals(
                            PolicyStatus.ACTIVE.name(),
                            result.getPolicyStatus()
                    );
                })
                .verifyComplete();

        verify(policyRepository)
                .findById(POLICY_ID);

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void getPolicy_ShouldThrowNotFound_WhenPolicyDoesNotExist() {

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        policyService.getPolicy(
                                POLICY_ID,
                                1,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyNotFoundException
                                && (
                                "Policy not found with ID: "
                                        + POLICY_ID
                        ).equals(error.getMessage())
                )
                .verify();
    }

    @Test
    void getPolicy_ShouldThrowException_WhenPolicyIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.getPolicy(
                                        " ",
                                        1,
                                        "ADMIN"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPolicyRequestException
                                && "Policy ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(policyRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // COMPLETE POLICY
    // =========================================================

    @Test
    void completePolicy_ShouldCompleteActivePolicy() {

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        when(template.update(policy))
                .thenReturn(
                        Mono.just(policy)
                );

        StepVerifier.create(
                        policyService.completePolicy(
                                POLICY_ID
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            PolicyStatus.COMPLETED.name(),
                            result.getPolicyStatus()
                    );

                    assertFalse(
                            result.isNew()
                    );
                })
                .verifyComplete();

        verify(template)
                .update(policy);
    }

    @Test
    void completePolicy_ShouldThrowNotFound_WhenPolicyDoesNotExist() {

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        policyService.completePolicy(
                                POLICY_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyNotFoundException
                                && (
                                "Policy not found with ID: "
                                        + POLICY_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Policy.class));
    }

    @Test
    void completePolicy_ShouldRejectAlreadyCompletedPolicy() {

        Policy policy =
                createPolicy(
                        PolicyStatus.COMPLETED.name()
                );

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyService.completePolicy(
                                POLICY_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyAlreadyFinalizedException
                                && (
                                "Only ACTIVE policies can be completed. "
                                        + "Current status: COMPLETED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Policy.class));
    }

    @Test
    void completePolicy_ShouldRejectClosedPolicy() {

        Policy policy =
                createPolicy(
                        PolicyStatus.CLOSED.name()
                );

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyService.completePolicy(
                                POLICY_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyAlreadyFinalizedException
                                && (
                                "Only ACTIVE policies can be completed. "
                                        + "Current status: CLOSED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Policy.class));
    }

    @Test
    void completePolicy_ShouldThrowException_WhenPolicyIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.completePolicy(" ")
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPolicyRequestException
                                && "Policy ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(policyRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // CLOSE POLICY
    // =========================================================

    @Test
    void closePolicy_ShouldCloseActivePolicy() {

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        when(template.update(policy))
                .thenReturn(
                        Mono.just(policy)
                );

        StepVerifier.create(
                        policyService.closePolicy(
                                POLICY_ID
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            PolicyStatus.CLOSED.name(),
                            result.getPolicyStatus()
                    );

                    assertFalse(
                            result.isNew()
                    );
                })
                .verifyComplete();

        verify(template)
                .update(policy);
    }

    @Test
    void closePolicy_ShouldCloseCompletedPolicy() {

        Policy policy =
                createPolicy(
                        PolicyStatus.COMPLETED.name()
                );

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        when(template.update(policy))
                .thenReturn(
                        Mono.just(policy)
                );

        StepVerifier.create(
                        policyService.closePolicy(
                                POLICY_ID
                        )
                )
                .assertNext(result ->
                        assertEquals(
                                PolicyStatus.CLOSED.name(),
                                result.getPolicyStatus()
                        )
                )
                .verifyComplete();

        verify(template)
                .update(policy);
    }

    @Test
    void closePolicy_ShouldRejectAlreadyClosedPolicy() {

        Policy policy =
                createPolicy(
                        PolicyStatus.CLOSED.name()
                );

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyService.closePolicy(
                                POLICY_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyAlreadyFinalizedException
                                && "Policy is already finalized as CLOSED"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Policy.class));
    }

    @Test
    void closePolicy_ShouldRejectCancelledPolicy() {

        Policy policy =
                createPolicy(
                        PolicyStatus.CANCELLED.name()
                );

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        StepVerifier.create(
                        policyService.closePolicy(
                                POLICY_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyAlreadyFinalizedException
                                && "Policy is already finalized as CANCELLED"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Policy.class));
    }

    @Test
    void closePolicy_ShouldThrowNotFound_WhenPolicyDoesNotExist() {

        when(policyRepository.findById(
                POLICY_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        policyService.closePolicy(
                                POLICY_ID
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyNotFoundException
                                && (
                                "Policy not found with ID: "
                                        + POLICY_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .update(any(Policy.class));
    }

    @Test
    void closePolicy_ShouldThrowException_WhenPolicyIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.closePolicy(" ")
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPolicyRequestException
                                && "Policy ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(policyRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private CreatePolicyRequest createPolicyRequest() {

        CreatePolicyRequest request =
                new CreatePolicyRequest();

        request.setQuoteId(
                QUOTE_ID
        );

        request.setPaymentId(
                PAYMENT_ID
        );

        return request;
    }

    private Quote createQuote(
            String status
    ) {
        Quote quote =
                new Quote();

        quote.setQuoteId(
                QUOTE_ID
        );

        quote.setApplicationId(
                APPLICATION_ID
        );

        quote.setPlanName(
                "STANDARD"
        );

        quote.setPremiumAmount(
                37500.0
        );

        quote.setMonthlyCompensation(
                50000.0
        );

        quote.setMaximumDelayMonths(
                24
        );

        quote.setMaximumPayout(
                1200000.0
        );

        quote.setGracePeriodDays(
                90
        );

        quote.setExpiryDate(
                LocalDateTime.now()
                        .plusDays(30)
        );

        quote.setStatus(
                status
        );

        quote.setCreatedAt(
                LocalDateTime.now()
        );

        quote.setNew(false);

        return quote;
    }

    private Policy createPolicy(
            String status
    ) {
        Policy policy =
                new Policy();

        policy.setPolicyId(
                POLICY_ID
        );

        policy.setApplicationId(
                APPLICATION_ID
        );

        policy.setQuoteId(
                QUOTE_ID
        );

        policy.setPaymentId(
                PAYMENT_ID
        );

        policy.setPolicyNumber(
                "POL-ABC12345"
        );

        policy.setCoverageAmount(
                1200000.0
        );

        policy.setPremiumAmount(
                37500.0
        );

        policy.setPolicyStatus(
                status
        );

        policy.setStartDate(
                LocalDate.of(
                        2026,
                        10,
                        4
                )
        );

        policy.setEndDate(
                LocalDate.of(
                        2028,
                        10,
                        4
                )
        );

        policy.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        23,
                        59
                )
        );

        policy.setNew(
                PolicyStatus.ACTIVE.name()
                        .equals(status)
        );

        return policy;
    }
}