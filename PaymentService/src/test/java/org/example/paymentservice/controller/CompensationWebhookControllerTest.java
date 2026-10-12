package org.example.paymentservice.controller;

import org.example.paymentservice.dto.CompensationWebhookRequest;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.enums.CompensationPaymentStatus;
import org.example.paymentservice.exception.InvalidPaymentRequestException;
import org.example.paymentservice.exception.InvalidPaymentStatusException;
import org.example.paymentservice.exception.PaymentAlreadyFinalizedException;
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
class CompensationWebhookControllerTest {

    @Mock
    private PaymentService paymentService;

    private PaymentWebhookController paymentWebhookController;

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
        paymentWebhookController =
                new PaymentWebhookController(
                        paymentService
                );
    }

    // =========================================================
    // PAID WEBHOOK
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldReturnPaidPayment() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.PAID.name()
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
                            PAYMENT_ID,
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
                            450000.0,
                            response.getPayoutAmount()
                    );

                    assertEquals(
                            TRANSACTION_REFERENCE,
                            response.getTransactionReference()
                    );

                    assertEquals(
                            CompensationPaymentStatus.PAID.name(),
                            response.getStatus()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    @Test
    void processCompensationWebhook_ShouldAcceptLowercasePaidStatus() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        "paid"
                );

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.PAID.name()
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
                .assertNext(response ->
                        assertEquals(
                                CompensationPaymentStatus.PAID.name(),
                                response.getStatus()
                        )
                )
                .verifyComplete();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // FAILED WEBHOOK
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldReturnFailedPayment() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.FAILED.name()
                );

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.FAILED.name()
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
                            PAYMENT_ID,
                            response.getPaymentId()
                    );

                    assertEquals(
                            TRANSACTION_REFERENCE,
                            response.getTransactionReference()
                    );

                    assertEquals(
                            CompensationPaymentStatus.FAILED.name(),
                            response.getStatus()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    @Test
    void processCompensationWebhook_ShouldAcceptLowercaseFailedStatus() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        "failed"
                );

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.FAILED.name()
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
                .assertNext(response ->
                        assertEquals(
                                CompensationPaymentStatus.FAILED.name(),
                                response.getStatus()
                        )
                )
                .verifyComplete();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // PAYMENT NOT FOUND
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateNotFoundError() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        PaymentNotFoundException exception =
                new PaymentNotFoundException(
                        "Compensation payment not found with ID: "
                                + PAYMENT_ID
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
                                        + PAYMENT_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // INVALID REQUEST
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateMissingPaymentIdError() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        request.setPaymentId("");

        InvalidPaymentRequestException exception =
                new InvalidPaymentRequestException(
                        "Payment ID is required"
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
                                && "Payment ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    @Test
    void processCompensationWebhook_ShouldPropagateMissingTransactionReferenceError() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        request.setTransactionReference("");

        InvalidPaymentRequestException exception =
                new InvalidPaymentRequestException(
                        "Transaction reference is required"
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
                                && "Transaction reference is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    @Test
    void processCompensationWebhook_ShouldPropagateMissingStatusError() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        request.setPaymentStatus("");

        InvalidPaymentRequestException exception =
                new InvalidPaymentRequestException(
                        "Payment status is required"
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
                                && "Payment status is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // INVALID STATUS
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateInvalidStatusError() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        "SUCCESS"
                );

        InvalidPaymentStatusException exception =
                new InvalidPaymentStatusException(
                        "Compensation webhook status "
                                + "must be PAID or FAILED"
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
                                "Compensation webhook status "
                                        + "must be PAID or FAILED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // FINALIZED PAYMENT
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateAlreadyPaidError() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        PaymentAlreadyFinalizedException exception =
                new PaymentAlreadyFinalizedException(
                        "Compensation payment is already finalized as PAID"
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
                                && "Compensation payment is already finalized as PAID"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    @Test
    void processCompensationWebhook_ShouldPropagateAlreadyFailedError() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.FAILED.name()
                );

        PaymentAlreadyFinalizedException exception =
                new PaymentAlreadyFinalizedException(
                        "Compensation payment is already finalized as FAILED"
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
                                && "Compensation payment is already finalized as FAILED"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // CLAIM SERVICE FAILURE
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldPropagateClaimClosureError() {

        CompensationWebhookRequest request =
                createWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        IllegalStateException exception =
                new IllegalStateException(
                        "Claim Service failed to close claim"
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
                                && "Claim Service failed to close claim"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .processCompensationWebhook(
                        request
                );
    }

    // =========================================================
    // EMPTY RESPONSE
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        CompensationWebhookRequest request =
                createWebhookRequest(
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
    // HELPERS
    // =========================================================

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

    private CompensationPayment createCompensationPayment(
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

        payment.setTransactionReference(
                TRANSACTION_REFERENCE
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