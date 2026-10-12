package org.example.paymentservice.controller;

import org.example.paymentservice.dto.CompensationPaymentRequest;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.enums.CompensationPaymentStatus;
import org.example.paymentservice.exception.InvalidPaymentRequestException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensationPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private CompensationPaymentController compensationPaymentController;

    private static final String PAYMENT_ID =
            "compensation-payment-101";

    private static final String CLAIM_ID =
            "claim-101";

    private static final String POLICY_ID =
            "policy-101";

    @BeforeEach
    void setUp() {
        compensationPaymentController =
                new CompensationPaymentController(
                        paymentService
                );
    }

    // =========================================================
    // CREATE COMPENSATION PAYMENT
    // =========================================================

    @Test
    void createCompensationPayment_ShouldReturnInitiatedPayment() {

        CompensationPaymentRequest request =
                createCompensationPaymentRequest();

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(paymentService.createCompensationPayment(
                request
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        compensationPaymentController
                                .createCompensationPayment(
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
                            CompensationPaymentStatus.INITIATED.name(),
                            response.getStatus()
                    );

                    assertNull(
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .createCompensationPayment(
                        request
                );
    }

    @Test
    void createCompensationPayment_ShouldPropagateInvalidRequestError() {

        CompensationPaymentRequest request =
                createCompensationPaymentRequest();

        InvalidPaymentRequestException exception =
                new InvalidPaymentRequestException(
                        "Payout amount must be greater than zero"
                );

        when(paymentService.createCompensationPayment(
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        compensationPaymentController
                                .createCompensationPayment(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Payout amount must be greater than zero"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .createCompensationPayment(
                        request
                );
    }

    @Test
    void createCompensationPayment_ShouldPropagateDuplicateError() {

        CompensationPaymentRequest request =
                createCompensationPaymentRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Compensation payment already exists "
                                + "for claim: "
                                + CLAIM_ID
                );

        when(paymentService.createCompensationPayment(
                request
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        compensationPaymentController
                                .createCompensationPayment(
                                        request
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && (
                                "Compensation payment already exists "
                                        + "for claim: "
                                        + CLAIM_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .createCompensationPayment(
                        request
                );
    }

    @Test
    void createCompensationPayment_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        CompensationPaymentRequest request =
                createCompensationPaymentRequest();

        when(paymentService.createCompensationPayment(
                request
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        compensationPaymentController
                                .createCompensationPayment(
                                        request
                                )
                )
                .verifyComplete();

        verify(paymentService)
                .createCompensationPayment(
                        request
                );
    }

    // =========================================================
    // GET COMPENSATION PAYMENT
    // =========================================================

    @Test
    void getCompensationPayment_ShouldReturnInitiatedPayment() {

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        when(paymentService.getCompensationPayment(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        compensationPaymentController
                                .getCompensationPayment(
                                        PAYMENT_ID
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
                            CompensationPaymentStatus.INITIATED.name(),
                            response.getStatus()
                    );

                    assertNull(
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .getCompensationPayment(
                        PAYMENT_ID
                );
    }

    @Test
    void getCompensationPayment_ShouldReturnPaidPayment() {

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.PAID.name()
                );

        payment.setTransactionReference(
                "TXN-COMP-101"
        );

        when(paymentService.getCompensationPayment(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        compensationPaymentController
                                .getCompensationPayment(
                                        PAYMENT_ID
                                )
                )
                .assertNext(response -> {

                    assertEquals(
                            PAYMENT_ID,
                            response.getPaymentId()
                    );

                    assertEquals(
                            CompensationPaymentStatus.PAID.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "TXN-COMP-101",
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .getCompensationPayment(
                        PAYMENT_ID
                );
    }

    @Test
    void getCompensationPayment_ShouldReturnFailedPayment() {

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.FAILED.name()
                );

        payment.setTransactionReference(
                "TXN-COMP-FAILED-101"
        );

        when(paymentService.getCompensationPayment(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        compensationPaymentController
                                .getCompensationPayment(
                                        PAYMENT_ID
                                )
                )
                .assertNext(response -> {

                    assertEquals(
                            PAYMENT_ID,
                            response.getPaymentId()
                    );

                    assertEquals(
                            CompensationPaymentStatus.FAILED.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "TXN-COMP-FAILED-101",
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .getCompensationPayment(
                        PAYMENT_ID
                );
    }

    @Test
    void getCompensationPayment_ShouldPropagateNotFoundError() {

        PaymentNotFoundException exception =
                new PaymentNotFoundException(
                        "Compensation payment not found with ID: "
                                + PAYMENT_ID
                );

        when(paymentService.getCompensationPayment(
                PAYMENT_ID
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        compensationPaymentController
                                .getCompensationPayment(
                                        PAYMENT_ID
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
                .getCompensationPayment(
                        PAYMENT_ID
                );
    }

    @Test
    void getCompensationPayment_ShouldPropagateInvalidPaymentIdError() {

        InvalidPaymentRequestException exception =
                new InvalidPaymentRequestException(
                        "Payment ID is required"
                );

        when(paymentService.getCompensationPayment(
                " "
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        compensationPaymentController
                                .getCompensationPayment(
                                        " "
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Payment ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .getCompensationPayment(
                        " "
                );
    }

    @Test
    void getCompensationPayment_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        when(paymentService.getCompensationPayment(
                PAYMENT_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        compensationPaymentController
                                .getCompensationPayment(
                                        PAYMENT_ID
                                )
                )
                .verifyComplete();

        verify(paymentService)
                .getCompensationPayment(
                        PAYMENT_ID
                );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private CompensationPaymentRequest
    createCompensationPaymentRequest() {

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