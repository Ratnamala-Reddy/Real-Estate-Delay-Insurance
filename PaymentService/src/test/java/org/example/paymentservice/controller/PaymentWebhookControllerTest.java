package org.example.paymentservice.controller;

import org.example.paymentservice.dto.CompensationWebhookRequest;
import org.example.paymentservice.dto.PremiumPaymentWebhookRequest;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.entity.PremiumPayment;
import org.example.paymentservice.enums.CompensationPaymentStatus;
import org.example.paymentservice.enums.PremiumPaymentStatus;
import org.example.paymentservice.exception.PaymentNotFoundException;
import org.example.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerTest {

    @Mock
    private PaymentService paymentService;

    private PaymentWebhookController paymentWebhookController;

    private static final String PREMIUM_PAYMENT_ID =
            "premium-payment-101";

    private static final String COMPENSATION_PAYMENT_ID =
            "compensation-payment-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String APPLICATION_ID =
            "application-101";

    private static final String CLAIM_ID =
            "claim-101";

    private static final String POLICY_ID =
            "policy-101";

    @BeforeEach
    void setUp() {
        paymentWebhookController =
                new PaymentWebhookController(
                        paymentService
                );
    }

    // =========================================================
    // PREMIUM WEBHOOK SUCCESS
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldReturnSuccessfulPayment() {

        PremiumPaymentWebhookRequest request =
                createPremiumWebhookRequest(
                        "TXN-PREMIUM-101",
                        PremiumPaymentStatus.SUCCESS.name()
                );

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.SUCCESS.name(),
                        "TXN-PREMIUM-101"
                );

        when(paymentService.processPremiumWebhook(
                request
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processPremiumWebhook(
                                        request
                                )
                )
                .assertNext(response -> {

                    assertEquals(
                            PREMIUM_PAYMENT_ID,
                            response.getPaymentId()
                    );

                    assertEquals(
                            QUOTE_ID,
                            response.getQuoteId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            PremiumPaymentStatus.SUCCESS.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "TXN-PREMIUM-101",
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .processPremiumWebhook(
                        request
                );
    }

    // =========================================================
    // PREMIUM WEBHOOK FAILED
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldReturnFailedPayment() {

        PremiumPaymentWebhookRequest request =
                createPremiumWebhookRequest(
                        "TXN-PREMIUM-FAILED-101",
                        PremiumPaymentStatus.FAILED.name()
                );

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.FAILED.name(),
                        "TXN-PREMIUM-FAILED-101"
                );

        when(paymentService.processPremiumWebhook(
                request
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processPremiumWebhook(
                                        request
                                )
                )
                .assertNext(response -> {

                    assertEquals(
                            PREMIUM_PAYMENT_ID,
                            response.getPaymentId()
                    );

                    assertEquals(
                            PremiumPaymentStatus.FAILED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "TXN-PREMIUM-FAILED-101",
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .processPremiumWebhook(
                        request
                );
    }

    // =========================================================
    // PREMIUM WEBHOOK NOT FOUND
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldPropagateNotFoundError() {

        PremiumPaymentWebhookRequest request =
                createPremiumWebhookRequest(
                        "TXN-PREMIUM-101",
                        PremiumPaymentStatus.SUCCESS.name()
                );

        PaymentNotFoundException exception =
                new PaymentNotFoundException(
                        "Premium payment not found with ID: "
                                + PREMIUM_PAYMENT_ID
                );

        when(paymentService.processPremiumWebhook(
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processPremiumWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && (
                                "Premium payment not found with ID: "
                                        + PREMIUM_PAYMENT_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processPremiumWebhook(
                        request
                );
    }

    // =========================================================
    // PREMIUM WEBHOOK SERVICE ERROR
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldPropagateServiceError() {

        PremiumPaymentWebhookRequest request =
                createPremiumWebhookRequest(
                        "TXN-PREMIUM-101",
                        PremiumPaymentStatus.SUCCESS.name()
                );

        RuntimeException exception =
                new RuntimeException(
                        "Policy creation failed"
                );

        when(paymentService.processPremiumWebhook(
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processPremiumWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Policy creation failed"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processPremiumWebhook(
                        request
                );
    }

    // =========================================================
    // PREMIUM WEBHOOK EMPTY RESPONSE
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        PremiumPaymentWebhookRequest request =
                createPremiumWebhookRequest(
                        "TXN-PREMIUM-101",
                        PremiumPaymentStatus.SUCCESS.name()
                );

        when(paymentService.processPremiumWebhook(
                request
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processPremiumWebhook(
                                        request
                                )
                )
                .verifyComplete();

        verify(paymentService)
                .processPremiumWebhook(
                        request
                );
    }

    // =========================================================
    // COMPENSATION WEBHOOK PAID
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldReturnPaidPayment() {

        CompensationWebhookRequest request =
                createCompensationWebhookRequest(
                        "TXN-COMPENSATION-101",
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.PAID.name(),
                        "TXN-COMPENSATION-101"
                );

        when(paymentService.processCompensationWebhook(
                request
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processCompensationWebhook(
                                        request
                                )
                )
                .assertNext(response -> {

                    assertEquals(
                            COMPENSATION_PAYMENT_ID,
                            response.getPaymentId()
                    );

                    assertEquals(
                            CLAIM_ID,
                            response.getClaimId()
                    );

                    assertEquals(
                            POLICY_ID,
                            response.getPolicyId()
                    );

                    assertEquals(
                            CompensationPaymentStatus.PAID.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "TXN-COMPENSATION-101",
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // COMPENSATION WEBHOOK FAILED
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldReturnFailedPayment() {

        CompensationWebhookRequest request =
                createCompensationWebhookRequest(
                        "TXN-COMPENSATION-FAILED-101",
                        CompensationPaymentStatus.FAILED.name()
                );

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.FAILED.name(),
                        "TXN-COMPENSATION-FAILED-101"
                );

        when(paymentService.processCompensationWebhook(
                request
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processCompensationWebhook(
                                        request
                                )
                )
                .assertNext(response -> {

                    assertEquals(
                            COMPENSATION_PAYMENT_ID,
                            response.getPaymentId()
                    );

                    assertEquals(
                            CompensationPaymentStatus.FAILED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "TXN-COMPENSATION-FAILED-101",
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // COMPENSATION WEBHOOK NOT FOUND
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateNotFoundError() {

        CompensationWebhookRequest request =
                createCompensationWebhookRequest(
                        "TXN-COMPENSATION-101",
                        CompensationPaymentStatus.PAID.name()
                );

        PaymentNotFoundException exception =
                new PaymentNotFoundException(
                        "Compensation payment not found with ID: "
                                + COMPENSATION_PAYMENT_ID
                );

        when(paymentService.processCompensationWebhook(
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processCompensationWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && (
                                "Compensation payment not found with ID: "
                                        + COMPENSATION_PAYMENT_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // COMPENSATION WEBHOOK SERVICE ERROR
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateServiceError() {

        CompensationWebhookRequest request =
                createCompensationWebhookRequest(
                        "TXN-COMPENSATION-101",
                        CompensationPaymentStatus.PAID.name()
                );

        RuntimeException exception =
                new RuntimeException(
                        "Claim closure failed"
                );

        when(paymentService.processCompensationWebhook(
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processCompensationWebhook(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Claim closure failed"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // COMPENSATION WEBHOOK EMPTY RESPONSE
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        CompensationWebhookRequest request =
                createCompensationWebhookRequest(
                        "TXN-COMPENSATION-101",
                        CompensationPaymentStatus.PAID.name()
                );

        when(paymentService.processCompensationWebhook(
                request
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        paymentWebhookController
                                .processCompensationWebhook(
                                        request
                                )
                )
                .verifyComplete();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // REQUEST HELPERS
    // =========================================================

    private PremiumPaymentWebhookRequest createPremiumWebhookRequest(
            String transactionReference,
            String paymentStatus
    ) {
        PremiumPaymentWebhookRequest request =
                new PremiumPaymentWebhookRequest();

        request.setPaymentId(
                PREMIUM_PAYMENT_ID
        );

        request.setTransactionReference(
                transactionReference
        );

        request.setPaymentStatus(
                paymentStatus
        );

        return request;
    }

    private CompensationWebhookRequest
    createCompensationWebhookRequest(
            String transactionReference,
            String paymentStatus
    ) {
        CompensationWebhookRequest request =
                new CompensationWebhookRequest();

        request.setPaymentId(
                COMPENSATION_PAYMENT_ID
        );

        request.setTransactionReference(
                transactionReference
        );

        request.setPaymentStatus(
                paymentStatus
        );

        return request;
    }

    // =========================================================
    // ENTITY HELPERS
    // =========================================================

    private PremiumPayment createPremiumPayment(
            String status,
            String transactionReference
    ) {
        PremiumPayment payment =
                new PremiumPayment();

        payment.setPaymentId(
                PREMIUM_PAYMENT_ID
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

        payment.setTransactionReference(
                transactionReference
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

    private CompensationPayment createCompensationPayment(
            String status,
            String transactionReference
    ) {
        CompensationPayment payment =
                new CompensationPayment();

        payment.setPaymentId(
                COMPENSATION_PAYMENT_ID
        );

        payment.setClaimId(
                CLAIM_ID
        );

        payment.setPolicyId(
                POLICY_ID
        );

        payment.setPayoutAmount(
                500000.0
        );

        payment.setTransactionReference(
                transactionReference
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