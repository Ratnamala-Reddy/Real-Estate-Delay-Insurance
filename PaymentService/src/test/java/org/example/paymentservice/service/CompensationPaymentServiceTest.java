package org.example.paymentservice.service;

import org.example.paymentservice.dto.CompensationPaymentRequest;
import org.example.paymentservice.dto.CompensationWebhookRequest;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.enums.CompensationPaymentStatus;
import org.example.paymentservice.exception.InvalidPaymentRequestException;
import org.example.paymentservice.exception.InvalidPaymentStatusException;
import org.example.paymentservice.exception.PaymentAlreadyFinalizedException;
import org.example.paymentservice.exception.PaymentNotFoundException;
import org.example.paymentservice.repository.CompensationPaymentRepository;
import org.example.paymentservice.repository.PremiumPaymentRepository;
import org.example.paymentservice.security.JwtService;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensationPaymentServiceTest {

    @Mock
    private PremiumPaymentRepository premiumPaymentRepository;

    @Mock
    private CompensationPaymentRepository compensationPaymentRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private WebClient webClient;

    @Mock
    private JwtService jwtService;

    @Mock
    private ReactiveInsertOperation.ReactiveInsert<CompensationPayment>
            compensationPaymentInsert;

    private PaymentServiceImpl paymentService;

    private static final String PAYMENT_ID =
            "compensation-payment-101";

    private static final String CLAIM_ID =
            "claim-101";

    private static final String POLICY_ID =
            "policy-101";

    private static final String TRANSACTION_REFERENCE =
            "TXN-COMP-101";

    @BeforeEach
    void setUp() {

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

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
    // CREATE COMPENSATION PAYMENT VALIDATION
    // =========================================================

    @Test
    void createCompensationPayment_ShouldRejectNullRequest() {

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .createCompensationPayment(
                                                null
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Compensation payment request is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(CompensationPayment.class);
    }

    @Test
    void createCompensationPayment_ShouldRejectNullClaimId() {

        CompensationPaymentRequest request =
                createPaymentRequest();

        request.setClaimId(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .createCompensationPayment(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Claim ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(CompensationPayment.class);
    }

    @Test
    void createCompensationPayment_ShouldRejectBlankClaimId() {

        CompensationPaymentRequest request =
                createPaymentRequest();

        request.setClaimId(" ");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .createCompensationPayment(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Claim ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(CompensationPayment.class);
    }

    @Test
    void createCompensationPayment_ShouldRejectNullPolicyId() {

        CompensationPaymentRequest request =
                createPaymentRequest();

        request.setPolicyId(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .createCompensationPayment(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Policy ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(CompensationPayment.class);
    }

    @Test
    void createCompensationPayment_ShouldRejectBlankPolicyId() {

        CompensationPaymentRequest request =
                createPaymentRequest();

        request.setPolicyId("   ");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .createCompensationPayment(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Policy ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(CompensationPayment.class);
    }

    @Test
    void createCompensationPayment_ShouldRejectNullPayoutAmount() {

        CompensationPaymentRequest request =
                createPaymentRequest();

        request.setPayoutAmount(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .createCompensationPayment(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Payout amount must be greater than zero"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(CompensationPayment.class);
    }

    @Test
    void createCompensationPayment_ShouldRejectZeroPayoutAmount() {

        CompensationPaymentRequest request =
                createPaymentRequest();

        request.setPayoutAmount(0.0);

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .createCompensationPayment(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Payout amount must be greater than zero"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(CompensationPayment.class);
    }

    @Test
    void createCompensationPayment_ShouldRejectNegativePayoutAmount() {

        CompensationPaymentRequest request =
                createPaymentRequest();

        request.setPayoutAmount(-100.0);

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .createCompensationPayment(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Payout amount must be greater than zero"
                                .equals(error.getMessage())
                )
                .verify();

        verify(template, never())
                .insert(CompensationPayment.class);
    }

    // =========================================================
    // CREATE COMPENSATION PAYMENT SUCCESS
    // =========================================================

    @Test
    void createCompensationPayment_ShouldCreateInitiatedPayment() {

        CompensationPaymentRequest request =
                createPaymentRequest();

        when(template.insert(
                CompensationPayment.class
        )).thenReturn(
                compensationPaymentInsert
        );

        when(compensationPaymentInsert.using(
                any(CompensationPayment.class)
        )).thenAnswer(invocation -> {

            CompensationPayment payment =
                    invocation.getArgument(0);

            return Mono.just(payment);
        });

        StepVerifier.create(
                        paymentService
                                .createCompensationPayment(
                                        request
                                )
                )
                .assertNext(payment -> {

                    assertNotNull(
                            payment.getPaymentId()
                    );

                    assertEquals(
                            CLAIM_ID,
                            payment.getClaimId()
                    );

                    assertEquals(
                            POLICY_ID,
                            payment.getPolicyId()
                    );

                    assertEquals(
                            450000.0,
                            payment.getPayoutAmount()
                    );

                    assertEquals(
                            CompensationPaymentStatus.INITIATED.name(),
                            payment.getStatus()
                    );

                    assertNotNull(
                            payment.getCreatedAt()
                    );

                    assertEquals(
                            null,
                            payment.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(template)
                .insert(CompensationPayment.class);

        verify(compensationPaymentInsert)
                .using(
                        any(CompensationPayment.class)
                );
    }

    // =========================================================
    // GET COMPENSATION PAYMENT
    // =========================================================

    @Test
    void getCompensationPayment_ShouldReturnPayment() {

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService
                                .getCompensationPayment(
                                        PAYMENT_ID
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
                            CompensationPaymentStatus.INITIATED.name(),
                            result.getStatus()
                    );
                })
                .verifyComplete();

        verify(compensationPaymentRepository)
                .findById(PAYMENT_ID);
    }

    @Test
    void getCompensationPayment_ShouldRejectNullPaymentId() {

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .getCompensationPayment(
                                                null
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Payment ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void getCompensationPayment_ShouldRejectBlankPaymentId() {

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .getCompensationPayment(
                                                " "
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Payment ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void getCompensationPayment_ShouldThrowNotFound() {

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        paymentService
                                .getCompensationPayment(
                                        PAYMENT_ID
                                )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentNotFoundException
                                && (
                                "Compensation payment not found with ID: "
                                        + PAYMENT_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository)
                .findById(PAYMENT_ID);
    }

    // =========================================================
    // COMPENSATION WEBHOOK REQUEST VALIDATION
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldRejectNullRequest() {

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .processCompensationWebhook(
                                                null
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Compensation webhook request is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void processCompensationWebhook_ShouldRejectBlankPaymentId() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        request.setPaymentId(" ");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .processCompensationWebhook(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Payment ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void processCompensationWebhook_ShouldRejectBlankTransactionReference() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        request.setTransactionReference("");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .processCompensationWebhook(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Transaction reference is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void processCompensationWebhook_ShouldRejectBlankStatus() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        request.setPaymentStatus("");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .processCompensationWebhook(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Payment status is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // COMPENSATION WEBHOOK STATUS VALIDATION
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldRejectSuccessStatus() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        "SUCCESS"
                );

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .processCompensationWebhook(
                                                request
                                        )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentStatusException
                                && (
                                "Compensation webhook status "
                                        + "must be PAID or FAILED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void processCompensationWebhook_ShouldRejectPendingStatus() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        "PENDING"
                );

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService
                                        .processCompensationWebhook(
                                                request
                                        )
                        )
                )
                .expectError(
                        InvalidPaymentStatusException.class
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // COMPENSATION WEBHOOK LOOKUP
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldThrowNotFound() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        when(compensationPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        paymentService
                                .processCompensationWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentNotFoundException
                                && (
                                "Compensation payment not found with ID: "
                                        + PAYMENT_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository)
                .findById(PAYMENT_ID);
    }

    // =========================================================
    // FAILED WEBHOOK SUCCESS
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldSaveFailedPayment() {

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

        verify(compensationPaymentRepository)
                .save(payment);

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void processCompensationWebhook_ShouldNormalizeLowercaseFailedStatus() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        "failed"
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
                .assertNext(result ->
                        assertEquals(
                                CompensationPaymentStatus.FAILED.name(),
                                result.getStatus()
                        )
                )
                .verifyComplete();

        verify(compensationPaymentRepository)
                .save(payment);
    }

    // =========================================================
    // FINALIZED PAYMENT PROTECTION
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldRejectAlreadyPaidPayment() {

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
                                && (
                                "Compensation payment is already finalized as PAID"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .save(any(CompensationPayment.class));
    }

    @Test
    void processCompensationWebhook_ShouldRejectAlreadyFailedPayment() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.FAILED.name()
                );

        CompensationPayment payment =
                createPayment(
                        CompensationPaymentStatus.FAILED.name()
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
                                && (
                                "Compensation payment is already finalized as FAILED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .save(any(CompensationPayment.class));
    }

    @Test
    void processCompensationWebhook_ShouldRejectUnsupportedCurrentStatus() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createPayment(
                        "CANCELLED"
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
                                && (
                                "Compensation payment cannot be processed "
                                        + "from status: CANCELLED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(compensationPaymentRepository, never())
                .save(any(CompensationPayment.class));
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private CompensationPaymentRequest createPaymentRequest() {

        CompensationPaymentRequest request =
                new CompensationPaymentRequest();

        request.setClaimId(
                CLAIM_ID
        );

        request.setPolicyId(
                POLICY_ID
        );

        request.setPayoutAmount(
                450000.0
        );

        return request;
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