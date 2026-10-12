package org.example.paymentservice.config;

import org.example.paymentservice.dto.PremiumPaymentRequest;
import org.example.paymentservice.dto.PremiumPaymentWebhookRequest;
import org.example.paymentservice.entity.PremiumPayment;
import org.example.paymentservice.enums.PremiumPaymentStatus;
import org.example.paymentservice.exception.ApplicationServiceException;
import org.example.paymentservice.exception.PolicyServiceException;
import org.example.paymentservice.exception.QuoteNotEligibleForPaymentException;
import org.example.paymentservice.exception.QuoteServiceException;
import org.example.paymentservice.repository.CompensationPaymentRepository;
import org.example.paymentservice.repository.PremiumPaymentRepository;
import org.example.paymentservice.security.JwtService;
import org.example.paymentservice.service.PaymentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceWebClientTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private PremiumPaymentRepository premiumPaymentRepository;

    @Mock
    private CompensationPaymentRepository
            compensationPaymentRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private ReactiveInsertOperation.ReactiveInsert<PremiumPayment>
            premiumPaymentInsert;

    @Mock
    private JwtService jwtService;

    private PaymentServiceImpl paymentService;

    private static final String PAYMENT_ID =
            "payment-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String APPLICATION_ID =
            "application-101";

    private static final String SERVICE_TOKEN =
            "signed-payment-service-token";

    private static final Integer OWNER_USER_ID =
            4;

    private static final String TRANSACTION_REFERENCE =
            "TXN-PREMIUM-101";

    @BeforeEach
    void setUp() {

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

        WebClient webClient =
                WebClient.builder()
                        .exchangeFunction(
                                exchangeFunction
                        )
                        .build();

        paymentService =
                new PaymentServiceImpl(
                        premiumPaymentRepository,
                        compensationPaymentRepository,
                        template,
                        webClient,
                        jwtService
                );

        ReflectionTestUtils.setField(
                paymentService,
                "applicationServiceUrl",
                "http://localhost:8082"
        );

        ReflectionTestUtils.setField(
                paymentService,
                "policyServiceUrl",
                "http://localhost:8085"
        );

        ReflectionTestUtils.setField(
                paymentService,
                "claimServiceUrl",
                "http://localhost:8087"
        );
    }

    @AfterEach
    void tearDown() {
        StepVerifier.resetDefaultTimeout();
    }

    // =========================================================
    // CREATE PAYMENT SUCCESS
    // =========================================================

    @Test
    void createPremiumPayment_ShouldCreatePayment_WhenQuoteAndOwnerAreValid() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(premiumPaymentRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(false)
        );

        List<ClientRequest> capturedRequests =
                new ArrayList<>();

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            ClientRequest clientRequest =
                    invocation.getArgument(0);

            capturedRequests.add(
                    clientRequest
            );

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                acceptedQuoteJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            ownerApplicationJson()
                    )
            );
        });

        when(template.insert(
                PremiumPayment.class
        )).thenReturn(
                premiumPaymentInsert
        );

        when(premiumPaymentInsert.using(
                any(PremiumPayment.class)
        )).thenAnswer(invocation -> {

            PremiumPayment payment =
                    invocation.getArgument(0);

            payment.setPaymentId(
                    PAYMENT_ID
            );

            return Mono.just(payment);
        });

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .assertNext(payment -> {

                    assertEquals(
                            PAYMENT_ID,
                            payment.getPaymentId()
                    );

                    assertEquals(
                            QUOTE_ID,
                            payment.getQuoteId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            payment.getApplicationId()
                    );

                    assertEquals(
                            37500.0,
                            payment.getPremiumAmount()
                    );

                    assertEquals(
                            "RAZORPAY",
                            payment.getPaymentProvider()
                    );

                    assertEquals(
                            PremiumPaymentStatus.INITIATED.name(),
                            payment.getStatus()
                    );

                    assertNull(
                            payment.getTransactionReference()
                    );

                    assertNotNull(
                            payment.getCreatedAt()
                    );
                })
                .verifyComplete();

        assertEquals(
                2,
                capturedRequests.size()
        );

        ClientRequest quoteRequest =
                capturedRequests.get(0);

        ClientRequest applicationRequest =
                capturedRequests.get(1);

        assertEquals(
                HttpMethod.GET,
                quoteRequest.method()
        );

        assertEquals(
                "http://localhost:8085/api/v1/quotes/"
                        + QUOTE_ID,
                quoteRequest.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                quoteRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        assertEquals(
                HttpMethod.GET,
                applicationRequest.method()
        );

        assertEquals(
                "http://localhost:8082/api/v1/applications/"
                        + APPLICATION_ID,
                applicationRequest.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                applicationRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        verify(jwtService)
                .generateServiceToken();

        verify(premiumPaymentInsert)
                .using(
                        any(PremiumPayment.class)
                );
    }

    // =========================================================
    // QUOTE NOT ACCEPTED
    // =========================================================

    @Test
    void createPremiumPayment_ShouldRejectGeneratedQuote() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(premiumPaymentRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(false)
        );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                generatedQuoteJson()
                        )
                )
        );

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                QuoteNotEligibleForPaymentException
                                && (
                                "Quote must be ACCEPTED "
                                        + "before premium payment. "
                                        + "Current status: GENERATED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(PremiumPayment.class);
    }

    // =========================================================
    // QUOTE EXPIRED
    // =========================================================

    @Test
    void createPremiumPayment_ShouldRejectExpiredQuote() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(premiumPaymentRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(false)
        );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                expiredAcceptedQuoteJson()
                        )
                )
        );

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                QuoteNotEligibleForPaymentException
                                && "Quote has expired"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(PremiumPayment.class);
    }

    // =========================================================
    // QUOTE NOT FOUND
    // =========================================================

    @Test
    void createPremiumPayment_ShouldMapQuoteNotFoundResponse() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(premiumPaymentRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(false)
        );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.NOT_FOUND,
                                """
                                {
                                  "code": "QUOTE_NOT_FOUND",
                                  "message": "Quote not found"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteServiceException
                                && (
                                "Quote not found: "
                                        + QUOTE_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(PremiumPayment.class);
    }

    // =========================================================
    // QUOTE ID MISMATCH
    // =========================================================

    @Test
    void createPremiumPayment_ShouldRejectQuoteIdMismatch() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(premiumPaymentRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(false)
        );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                """
                                {
                                  "quoteId": "different-quote",
                                  "applicationId": "application-101",
                                  "premiumAmount": 37500.0,
                                  "status": "ACCEPTED",
                                  "expiryDate": "2026-11-03T23:56:00"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteServiceException
                                && (
                                "Quote ID returned by Policy Service "
                                        + "does not match"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(PremiumPayment.class);
    }

    // =========================================================
    // OWNERSHIP FAILURE
    // =========================================================

    @Test
    void createPremiumPayment_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnApplication() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(premiumPaymentRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(false)
        );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                acceptedQuoteJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            """
                            {
                              "applicationId": "application-101",
                              "userId": 99
                            }
                            """
                    )
            );
        });

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && (
                                "You do not own the application "
                                        + "associated with this quote"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(PremiumPayment.class);
    }

    // =========================================================
    // APPLICATION SERVICE FAILURE
    // =========================================================

    @Test
    void createPremiumPayment_ShouldMapApplicationServiceFailure() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(premiumPaymentRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(false)
        );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                acceptedQuoteJson()
                        )
                );
            }

            return Mono.error(
                    new RuntimeException(
                            "Application Service unavailable"
                    )
            );
        });

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof ApplicationServiceException
                                && (
                                "Unable to fetch application "
                                        + "from Application Service"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(PremiumPayment.class);
    }

    // =========================================================
    // GET PAYMENT OWNERSHIP SUCCESS
    // =========================================================

    @Test
    void getPremiumPayment_ShouldReturnPayment_WhenBuyerOwnsApplication() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                ownerApplicationJson()
                        )
                )
        );

        StepVerifier.create(
                        paymentService.getPremiumPayment(
                                PAYMENT_ID,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectNext(payment)
                .verifyComplete();

        verify(jwtService)
                .generateServiceToken();
    }

    // =========================================================
    // GET PAYMENT OWNERSHIP FAILURE
    // =========================================================

    @Test
    void getPremiumPayment_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnPayment() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                """
                                {
                                  "applicationId": "application-101",
                                  "userId": 99
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        paymentService.getPremiumPayment(
                                PAYMENT_ID,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && "You do not own this payment"
                                .equals(error.getMessage())
                )
                .verify();
    }

    // =========================================================
    // SUCCESS WEBHOOK AND POLICY CREATION
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldCreatePolicy_WhenPaymentSucceeds() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        PremiumPaymentWebhookRequest request =
                createSuccessfulWebhookRequest();

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(premiumPaymentRepository
                .existsByTransactionReference(
                        TRANSACTION_REFERENCE
                )).thenReturn(
                Mono.just(false)
        );

        when(premiumPaymentRepository.save(
                payment
        )).thenAnswer(invocation ->
                Mono.just(
                        invocation.getArgument(0)
                )
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        AtomicReference<ClientRequest> capturedRequest =
                new AtomicReference<>();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            ClientRequest clientRequest =
                    invocation.getArgument(0);

            capturedRequest.set(
                    clientRequest
            );

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            """
                            {
                              "policyId": "policy-101",
                              "quoteId": "quote-101",
                              "paymentId": "payment-101",
                              "policyStatus": "ACTIVE"
                            }
                            """
                    )
            );
        });

        StepVerifier.create(
                        paymentService.processPremiumWebhook(
                                request
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            PAYMENT_ID,
                            result.getPaymentId()
                    );

                    assertEquals(
                            PremiumPaymentStatus.SUCCESS.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            TRANSACTION_REFERENCE,
                            result.getTransactionReference()
                    );
                })
                .verifyComplete();

        ClientRequest policyRequest =
                capturedRequest.get();

        assertNotNull(policyRequest);

        assertEquals(
                HttpMethod.POST,
                policyRequest.method()
        );

        assertEquals(
                "http://localhost:8085/api/v1/policies",
                policyRequest.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                policyRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        verify(premiumPaymentRepository)
                .save(payment);

        verify(jwtService)
                .generateServiceToken();

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // SUCCESS WEBHOOK POLICY SERVICE FAILURE
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldMapPolicyServiceFailure() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        PremiumPaymentWebhookRequest request =
                createSuccessfulWebhookRequest();

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(premiumPaymentRepository
                .existsByTransactionReference(
                        TRANSACTION_REFERENCE
                )).thenReturn(
                Mono.just(false)
        );

        when(premiumPaymentRepository.save(
                payment
        )).thenAnswer(invocation ->
                Mono.just(
                        invocation.getArgument(0)
                )
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                """
                                {
                                  "code": "INTERNAL_SERVER_ERROR",
                                  "message": "Policy creation failed"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        paymentService.processPremiumWebhook(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PolicyServiceException
                                && (
                                "Policy Service failed "
                                        + "to create policy"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository)
                .save(payment);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private ClientResponse jsonResponse(
            HttpStatus status,
            String body
    ) {
        return ClientResponse
                .create(status)
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .body(body)
                .build();
    }

    private String acceptedQuoteJson() {
        return """
                {
                  "quoteId": "quote-101",
                  "applicationId": "application-101",
                  "premiumAmount": 37500.0,
                  "status": "ACCEPTED",
                  "expiryDate": "2026-11-03T23:56:00"
                }
                """;
    }

    private String generatedQuoteJson() {
        return """
                {
                  "quoteId": "quote-101",
                  "applicationId": "application-101",
                  "premiumAmount": 37500.0,
                  "status": "GENERATED",
                  "expiryDate": "2026-11-03T23:56:00"
                }
                """;
    }

    private String expiredAcceptedQuoteJson() {
        return """
                {
                  "quoteId": "quote-101",
                  "applicationId": "application-101",
                  "premiumAmount": 37500.0,
                  "status": "ACCEPTED",
                  "expiryDate": "2025-01-01T00:00:00"
                }
                """;
    }

    private String ownerApplicationJson() {
        return """
                {
                  "applicationId": "application-101",
                  "userId": 4
                }
                """;
    }

    private PremiumPaymentRequest createPremiumPaymentRequest() {

        PremiumPaymentRequest request =
                new PremiumPaymentRequest();

        request.setQuoteId(
                QUOTE_ID
        );

        request.setPaymentProvider(
                "RAZORPAY"
        );

        return request;
    }

    private PremiumPaymentWebhookRequest
    createSuccessfulWebhookRequest() {

        PremiumPaymentWebhookRequest request =
                new PremiumPaymentWebhookRequest();

        request.setPaymentId(
                PAYMENT_ID
        );

        request.setTransactionReference(
                TRANSACTION_REFERENCE
        );

        request.setPaymentStatus(
                PremiumPaymentStatus.SUCCESS.name()
        );

        return request;
    }

    private PremiumPayment createPremiumPayment(
            String status
    ) {
        PremiumPayment payment =
                new PremiumPayment();

        payment.setPaymentId(
                PAYMENT_ID
        );

        payment.setQuoteId(
                QUOTE_ID
        );

        payment.setApplicationId(
                APPLICATION_ID
        );

        payment.setPremiumAmount(
                37500.0
        );

        payment.setPaymentProvider(
                "RAZORPAY"
        );

        payment.setStatus(
                status
        );

        payment.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        23,
                        56
                )
        );

        return payment;
    }
}