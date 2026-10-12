package org.example.paymentservice.config;

import org.example.paymentservice.dto.CompensationWebhookRequest;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.enums.CompensationPaymentStatus;
import org.example.paymentservice.exception.PaymentAlreadyFinalizedException;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensationPaymentWebClientTest {

    @Mock
    private PremiumPaymentRepository premiumPaymentRepository;

    @Mock
    private CompensationPaymentRepository compensationPaymentRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private JwtService jwtService;

    private PaymentServiceImpl paymentService;

    private static final String PAYMENT_ID =
            "compensation-payment-101";

    private static final String CLAIM_ID =
            "claim-101";

    private static final String POLICY_ID =
            "policy-101";

    private static final String TRANSACTION_REFERENCE =
            "TXN-COMP-101";

    private static final String SERVICE_TOKEN =
            "signed-payment-service-token";

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
    // PAID WEBHOOK AND CLAIM CLOSURE SUCCESS
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldCloseClaim_WhenPaymentBecomesPaid() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(compensationPaymentRepository.save(
                payment
        )).thenReturn(
                Mono.just(payment)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(
                        SERVICE_TOKEN
                );

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
                              "claimId": "claim-101",
                              "status": "CLOSED",
                              "remarks": "Claim closed after compensation settlement"
                            }
                            """
                    )
            );
        });

        StepVerifier.create(
                        paymentService
                                .processCompensationWebhook(
                                        request
                                )
                )
                .assertNext(result -> {

                    assertEquals(
                            PAYMENT_ID,
                            result.getPaymentId()
                    );

                    assertEquals(
                            CLAIM_ID,
                            result.getClaimId()
                    );

                    assertEquals(
                            POLICY_ID,
                            result.getPolicyId()
                    );

                    assertEquals(
                            450000.0,
                            result.getPayoutAmount()
                    );

                    assertEquals(
                            TRANSACTION_REFERENCE,
                            result.getTransactionReference()
                    );

                    assertEquals(
                            CompensationPaymentStatus.PAID.name(),
                            result.getStatus()
                    );
                })
                .verifyComplete();

        ClientRequest claimCloseRequest =
                capturedRequest.get();

        assertNotNull(
                claimCloseRequest
        );

        assertEquals(
                HttpMethod.POST,
                claimCloseRequest.method()
        );

        assertEquals(
                "http://localhost:8087/api/v1/claims/"
                        + CLAIM_ID
                        + "/close",
                claimCloseRequest.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                claimCloseRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        verify(compensationPaymentRepository)
                .save(payment);

        verify(jwtService)
                .generateServiceToken();

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // LOWERCASE PAID STATUS
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldNormalizePaidStatus_AndCloseClaim() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        "paid"
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.PENDING.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(compensationPaymentRepository.save(
                payment
        )).thenReturn(
                Mono.just(payment)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(
                        SERVICE_TOKEN
                );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                """
                                {
                                  "claimId": "claim-101",
                                  "status": "CLOSED"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        paymentService
                                .processCompensationWebhook(
                                        request
                                )
                )
                .assertNext(result -> {

                    assertEquals(
                            CompensationPaymentStatus.PAID.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            TRANSACTION_REFERENCE,
                            result.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(jwtService)
                .generateServiceToken();

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // FAILED PAYMENT DOES NOT CLOSE CLAIM
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldNotCloseClaim_WhenPaymentFails() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.FAILED.name()
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(compensationPaymentRepository.save(
                payment
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService
                                .processCompensationWebhook(
                                        request
                                )
                )
                .assertNext(result -> {

                    assertEquals(
                            CompensationPaymentStatus.FAILED.name(),
                            result.getStatus()
                    );

                    assertEquals(
                            TRANSACTION_REFERENCE,
                            result.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(jwtService, never())
                .generateServiceToken();

        verify(exchangeFunction, never())
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // CLAIM SERVICE BAD REQUEST
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateError_WhenClaimServiceReturnsBadRequest() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(compensationPaymentRepository.save(
                payment
        )).thenReturn(
                Mono.just(payment)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(
                        SERVICE_TOKEN
                );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.BAD_REQUEST,
                                """
                                {
                                  "code": "INVALID_CLAIM_STATE",
                                  "message": "Claim cannot be closed"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        paymentService
                                .processCompensationWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error instanceof IllegalStateException
                                && "Claim Service failed to close claim"
                                .equals(error.getMessage())
                )
                .verify();

        assertEquals(
                CompensationPaymentStatus.PAID.name(),
                payment.getStatus()
        );

        assertEquals(
                TRANSACTION_REFERENCE,
                payment.getTransactionReference()
        );

        verify(compensationPaymentRepository)
                .save(payment);

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // CLAIM SERVICE INTERNAL ERROR
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateError_WhenClaimServiceReturnsServerError() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(compensationPaymentRepository.save(
                payment
        )).thenReturn(
                Mono.just(payment)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(
                        SERVICE_TOKEN
                );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                """
                                {
                                  "code": "INTERNAL_SERVER_ERROR",
                                  "message": "Claim Service unavailable"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        paymentService
                                .processCompensationWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error instanceof IllegalStateException
                                && "Claim Service failed to close claim"
                                .equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository)
                .save(payment);

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // CLAIM SERVICE UNAUTHORIZED
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateError_WhenClaimServiceRejectsToken() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(compensationPaymentRepository.save(
                payment
        )).thenReturn(
                Mono.just(payment)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(
                        SERVICE_TOKEN
                );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.UNAUTHORIZED,
                                """
                                {
                                  "code": "UNAUTHORIZED",
                                  "message": "Invalid JWT"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        paymentService
                                .processCompensationWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error instanceof IllegalStateException
                                && "Claim Service failed to close claim"
                                .equals(error.getMessage())
                )
                .verify();

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // CLAIM SERVICE CONNECTION FAILURE
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateConnectionFailure() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(compensationPaymentRepository.save(
                payment
        )).thenReturn(
                Mono.just(payment)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(
                        SERVICE_TOKEN
                );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.error(
                        new RuntimeException(
                                "Connection refused"
                        )
                )
        );

        StepVerifier.create(
                        paymentService
                                .processCompensationWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error instanceof RuntimeException
                                && "Connection refused"
                                .equals(error.getMessage())
                )
                .verify();

        assertEquals(
                CompensationPaymentStatus.PAID.name(),
                payment.getStatus()
        );

        verify(compensationPaymentRepository)
                .save(payment);

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // FINALIZED PAYMENT DOES NOT CALL CLAIM SERVICE
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldNotCallClaimService_WhenPaymentIsAlreadyPaid() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.PAID.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService
                                .processCompensationWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentAlreadyFinalizedException
                                && "Compensation payment is already finalized as PAID"
                                .equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .save(
                        any(CompensationPayment.class)
                );

        verify(jwtService, never())
                .generateServiceToken();

        verify(exchangeFunction, never())
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // CLAIM CLOSE REQUEST CONTAINS NO BODY
    // =========================================================
    @Test
    void processCompensationWebhook_ShouldSendCloseRequestWithoutBody() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(compensationPaymentRepository.save(
                payment
        )).thenReturn(
                Mono.just(payment)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(
                        SERVICE_TOKEN
                );

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
                              "claimId": "claim-101",
                              "status": "CLOSED"
                            }
                            """
                    )
            );
        });

        StepVerifier.create(
                        paymentService.processCompensationWebhook(
                                request
                        )
                )
                .expectNext(payment)
                .verifyComplete();

        ClientRequest closeRequest =
                capturedRequest.get();

        assertNotNull(
                closeRequest
        );

        assertEquals(
                HttpMethod.POST,
                closeRequest.method()
        );

        assertEquals(
                "http://localhost:8087/api/v1/claims/"
                        + CLAIM_ID
                        + "/close",
                closeRequest.url().toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                closeRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        /*
         * A value of -1 means that Content-Length was not specified.
         * This is expected because the claim-close POST request has no body.
         */
        assertEquals(
                -1L,
                closeRequest
                        .headers()
                        .getContentLength()
        );

        verify(compensationPaymentRepository)
                .findById(
                        PAYMENT_ID
                );

        verify(compensationPaymentRepository)
                .save(
                        payment
                );

        verify(jwtService)
                .generateServiceToken();

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );
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

    private CompensationWebhookRequest createWebhookRequest(
            String status
    ) {
        CompensationWebhookRequest request =
                new CompensationWebhookRequest();

        request.setPaymentId(
                PAYMENT_ID
        );

        request.setTransactionReference(
                TRANSACTION_REFERENCE
        );

        request.setPaymentStatus(
                status
        );

        return request;
    }

    private CompensationPayment createPayment(
            String status
    ) {
        CompensationPayment payment =
                new CompensationPayment();

        payment.setPaymentId(
                PAYMENT_ID
        );

        payment.setClaimId(
                CLAIM_ID
        );

        payment.setPolicyId(
                POLICY_ID
        );

        payment.setPayoutAmount(
                450000.0
        );

        payment.setStatus(
                status
        );

        payment.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        5,
                        2,
                        38
                )
        );

        return payment;
    }
}