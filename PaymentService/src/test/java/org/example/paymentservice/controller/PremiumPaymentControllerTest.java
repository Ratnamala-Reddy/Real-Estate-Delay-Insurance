package org.example.paymentservice.controller;

import org.example.paymentservice.dto.PremiumPaymentRequest;
import org.example.paymentservice.entity.PremiumPayment;
import org.example.paymentservice.enums.PremiumPaymentStatus;
import org.example.paymentservice.exception.PaymentNotFoundException;
import org.example.paymentservice.security.AuthenticatedUser;
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
class PremiumPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private PremiumPaymentController premiumPaymentController;

    private AuthenticatedUser buyer;

    private AuthenticatedUser admin;

    private static final String PAYMENT_ID =
            "payment-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String APPLICATION_ID =
            "application-101";

    private static final Integer BUYER_USER_ID =
            4;

    @BeforeEach
    void setUp() {

        premiumPaymentController =
                new PremiumPaymentController(
                        paymentService
                );

        buyer =
                new AuthenticatedUser(
                        BUYER_USER_ID,
                        "buyer@gmail.com",
                        "BUYER"
                );

        admin =
                new AuthenticatedUser(
                        1,
                        "admin@gmail.com",
                        "ADMIN"
                );
    }

    // =========================================================
    // CREATE PREMIUM PAYMENT
    // =========================================================

    @Test
    void createPremiumPayment_ShouldReturnInitiatedPayment() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(paymentService.createPremiumPayment(
                request,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        premiumPaymentController
                                .createPremiumPayment(
                                        request,
                                        buyer
                                )
                )
                .assertNext(response -> {

                    assertEquals(
                            PAYMENT_ID,
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
                            37500.0,
                            response.getPremiumAmount()
                    );

                    assertEquals(
                            "RAZORPAY",
                            response.getPaymentProvider()
                    );

                    assertEquals(
                            PremiumPaymentStatus.INITIATED.name(),
                            response.getStatus()
                    );

                    assertNull(
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .createPremiumPayment(
                        request,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void createPremiumPayment_ShouldPassBuyerIdentityToService() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(paymentService.createPremiumPayment(
                request,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        premiumPaymentController
                                .createPremiumPayment(
                                        request,
                                        buyer
                                )
                )
                .expectNext(payment)
                .verifyComplete();

        verify(paymentService)
                .createPremiumPayment(
                        request,
                        4,
                        "BUYER"
                );
    }

    @Test
    void createPremiumPayment_ShouldPropagateServiceError() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Premium payment creation failed"
                );

        when(paymentService.createPremiumPayment(
                request,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        premiumPaymentController
                                .createPremiumPayment(
                                        request,
                                        buyer
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "Premium payment creation failed"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .createPremiumPayment(
                        request,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void createPremiumPayment_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        when(paymentService.createPremiumPayment(
                request,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        premiumPaymentController
                                .createPremiumPayment(
                                        request,
                                        buyer
                                )
                )
                .verifyComplete();

        verify(paymentService)
                .createPremiumPayment(
                        request,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    // =========================================================
    // GET PREMIUM PAYMENT AS BUYER
    // =========================================================

    @Test
    void getPremiumPayment_ShouldReturnPayment_ForBuyer() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(paymentService.getPremiumPayment(
                PAYMENT_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        premiumPaymentController
                                .getPremiumPayment(
                                        PAYMENT_ID,
                                        buyer
                                )
                )
                .assertNext(response -> {

                    assertEquals(
                            PAYMENT_ID,
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
                            PremiumPaymentStatus.INITIATED.name(),
                            response.getStatus()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .getPremiumPayment(
                        PAYMENT_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void getPremiumPayment_ShouldPassBuyerIdentityToService() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(paymentService.getPremiumPayment(
                PAYMENT_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        premiumPaymentController
                                .getPremiumPayment(
                                        PAYMENT_ID,
                                        buyer
                                )
                )
                .expectNext(payment)
                .verifyComplete();

        verify(paymentService)
                .getPremiumPayment(
                        PAYMENT_ID,
                        4,
                        "BUYER"
                );
    }

    // =========================================================
    // GET PREMIUM PAYMENT AS ADMIN
    // =========================================================

    @Test
    void getPremiumPayment_ShouldReturnPayment_ForAdmin() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.SUCCESS.name()
                );

        payment.setTransactionReference(
                "TXN-101"
        );

        when(paymentService.getPremiumPayment(
                PAYMENT_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        premiumPaymentController
                                .getPremiumPayment(
                                        PAYMENT_ID,
                                        admin
                                )
                )
                .assertNext(response -> {

                    assertEquals(
                            PAYMENT_ID,
                            response.getPaymentId()
                    );

                    assertEquals(
                            PremiumPaymentStatus.SUCCESS.name(),
                            response.getStatus()
                    );

                    assertEquals(
                            "TXN-101",
                            response.getTransactionReference()
                    );
                })
                .verifyComplete();

        verify(paymentService)
                .getPremiumPayment(
                        PAYMENT_ID,
                        1,
                        "ADMIN"
                );
    }

    @Test
    void getPremiumPayment_ShouldPassAdminIdentityToService() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(paymentService.getPremiumPayment(
                PAYMENT_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        premiumPaymentController
                                .getPremiumPayment(
                                        PAYMENT_ID,
                                        admin
                                )
                )
                .expectNext(payment)
                .verifyComplete();

        verify(paymentService)
                .getPremiumPayment(
                        PAYMENT_ID,
                        1,
                        "ADMIN"
                );
    }

    // =========================================================
    // GET PAYMENT ERRORS
    // =========================================================

    @Test
    void getPremiumPayment_ShouldPropagateNotFoundError() {

        PaymentNotFoundException exception =
                new PaymentNotFoundException(
                        "Premium payment not found with ID: "
                                + PAYMENT_ID
                );

        when(paymentService.getPremiumPayment(
                PAYMENT_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        premiumPaymentController
                                .getPremiumPayment(
                                        PAYMENT_ID,
                                        buyer
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && (
                                "Premium payment not found with ID: "
                                        + PAYMENT_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .getPremiumPayment(
                        PAYMENT_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void getPremiumPayment_ShouldPropagateOwnershipError() {

        RuntimeException exception =
                new RuntimeException(
                        "You do not own this payment"
                );

        when(paymentService.getPremiumPayment(
                PAYMENT_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        premiumPaymentController
                                .getPremiumPayment(
                                        PAYMENT_ID,
                                        buyer
                                )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && "You do not own this payment"
                                .equals(error.getMessage())
                )
                .verify();

        verify(paymentService)
                .getPremiumPayment(
                        PAYMENT_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void getPremiumPayment_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        when(paymentService.getPremiumPayment(
                PAYMENT_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        premiumPaymentController
                                .getPremiumPayment(
                                        PAYMENT_ID,
                                        buyer
                                )
                )
                .verifyComplete();

        verify(paymentService)
                .getPremiumPayment(
                        PAYMENT_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    // =========================================================
    // HELPERS
    // =========================================================

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