package org.example.paymentservice.service;

import org.example.paymentservice.dto.PremiumPaymentRequest;
import org.example.paymentservice.dto.PremiumPaymentWebhookRequest;
import org.example.paymentservice.entity.PremiumPayment;
import org.example.paymentservice.enums.PremiumPaymentStatus;
import org.example.paymentservice.exception.InvalidPaymentRequestException;
import org.example.paymentservice.exception.InvalidPaymentStatusException;
import org.example.paymentservice.exception.PaymentAlreadyFinalizedException;
import org.example.paymentservice.exception.PaymentNotFoundException;
import org.example.paymentservice.exception.PremiumPaymentAlreadyExistsException;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PremiumPaymentRepository premiumPaymentRepository;

    @Mock
    private CompensationPaymentRepository
            compensationPaymentRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private WebClient webClient;

    @Mock
    private JwtService jwtService;

    private PaymentServiceImpl paymentService;

    private static final String PAYMENT_ID =
            "payment-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String APPLICATION_ID =
            "application-101";

    private static final String TRANSACTION_REFERENCE =
            "TXN-PREMIUM-101";

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
    // CREATE PAYMENT REQUEST VALIDATION
    // =========================================================

    @Test
    void createPremiumPayment_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.createPremiumPayment(
                                        null,
                                        4,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Premium payment request is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();

        verify(premiumPaymentRepository, never())
                .existsByQuoteId(any(String.class));
    }

    @Test
    void createPremiumPayment_ShouldThrowException_WhenQuoteIdIsNull() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        request.setQuoteId(null);

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.createPremiumPayment(
                                        request,
                                        4,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Quote ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createPremiumPayment_ShouldThrowException_WhenQuoteIdIsBlank() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        request.setQuoteId("   ");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.createPremiumPayment(
                                        request,
                                        4,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Quote ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createPremiumPayment_ShouldThrowException_WhenProviderIsBlank() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        request.setPaymentProvider("");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.createPremiumPayment(
                                        request,
                                        4,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Payment provider is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    // =========================================================
    // CREATE PAYMENT AUTHORIZATION
    // =========================================================

    @Test
    void createPremiumPayment_ShouldThrowAccessDenied_WhenRoleIsAdmin() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                1,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                org.springframework.security
                                        .access.AccessDeniedException
                                && "Only BUYER can create a premium payment"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createPremiumPayment_ShouldThrowAccessDenied_WhenRoleIsUnsupported() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                10,
                                "CLAIMS_OFFICER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                org.springframework.security
                                        .access.AccessDeniedException
                                && "Only BUYER can create a premium payment"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void createPremiumPayment_ShouldThrowAccessDenied_WhenBuyerUserIdIsNull() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                null,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                org.springframework.security
                                        .access.AccessDeniedException
                                && "Caller user ID is missing"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    // =========================================================
    // DUPLICATE PAYMENT
    // =========================================================

    @Test
    void createPremiumPayment_ShouldThrowException_WhenPaymentAlreadyExists() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        when(jwtService.generateServiceToken())
                .thenReturn("service-token");

        when(premiumPaymentRepository.existsByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(true)
        );

        StepVerifier.create(
                        paymentService.createPremiumPayment(
                                request,
                                4,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                PremiumPaymentAlreadyExistsException
                                && (
                                "Premium payment already exists for quote: "
                                        + QUOTE_ID
                        ).equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository)
                .existsByQuoteId(QUOTE_ID);

        verify(template, never())
                .insert(PremiumPayment.class);
    }

    // =========================================================
    // GET PREMIUM PAYMENT
    // =========================================================

    @Test
    void getPremiumPayment_ShouldReturnPayment_WhenCallerIsAdmin() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService.getPremiumPayment(
                                PAYMENT_ID,
                                1,
                                "ADMIN"
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            PAYMENT_ID,
                            result.getPaymentId()
                    );

                    assertEquals(
                            QUOTE_ID,
                            result.getQuoteId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            result.getApplicationId()
                    );

                    assertEquals(
                            PremiumPaymentStatus.INITIATED.name(),
                            result.getStatus()
                    );
                })
                .verifyComplete();

        verify(premiumPaymentRepository)
                .findById(PAYMENT_ID);

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void getPremiumPayment_ShouldThrowNotFound_WhenPaymentDoesNotExist() {

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        paymentService.getPremiumPayment(
                                PAYMENT_ID,
                                1,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentNotFoundException
                                && (
                                "Premium payment not found with ID: "
                                        + PAYMENT_ID
                        ).equals(error.getMessage())
                )
                .verify();
    }

    @Test
    void getPremiumPayment_ShouldThrowException_WhenPaymentIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.getPremiumPayment(
                                        " ",
                                        1,
                                        "ADMIN"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Payment ID is required"
                                .equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void getPremiumPayment_ShouldThrowAccessDenied_WhenRoleIsUnsupported() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService.getPremiumPayment(
                                PAYMENT_ID,
                                10,
                                "CLAIMS_OFFICER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                org.springframework.security
                                        .access.AccessDeniedException
                                && "Unsupported caller role"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void getPremiumPayment_ShouldThrowAccessDenied_WhenBuyerUserIdIsNull() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService.getPremiumPayment(
                                PAYMENT_ID,
                                null,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                org.springframework.security
                                        .access.AccessDeniedException
                                && "Caller user ID is missing"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    // =========================================================
    // WEBHOOK REQUEST VALIDATION
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.processPremiumWebhook(
                                        null
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && (
                                "Premium payment webhook request is required"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void processPremiumWebhook_ShouldThrowException_WhenPaymentIdIsBlank() {

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.SUCCESS.name()
                );

        request.setPaymentId("");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.processPremiumWebhook(
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

        verify(premiumPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void processPremiumWebhook_ShouldThrowException_WhenTransactionReferenceIsBlank() {

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.SUCCESS.name()
                );

        request.setTransactionReference(" ");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.processPremiumWebhook(
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

        verify(premiumPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void processPremiumWebhook_ShouldThrowException_WhenStatusIsBlank() {

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.SUCCESS.name()
                );

        request.setPaymentStatus("");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.processPremiumWebhook(
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

        verify(premiumPaymentRepository, never())
                .findById(any(String.class));
    }

    @Test
    void processPremiumWebhook_ShouldRejectUnsupportedStatus() {

        PremiumPaymentWebhookRequest request =
                createWebhookRequest("PAID");

        StepVerifier.create(
                        Mono.defer(() ->
                                paymentService.processPremiumWebhook(
                                        request
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentStatusException
                                && (
                                "Premium payment webhook status "
                                        + "must be SUCCESS or FAILED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // WEBHOOK PAYMENT LOOKUP
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldThrowNotFound_WhenPaymentDoesNotExist() {

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.FAILED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        paymentService.processPremiumWebhook(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentNotFoundException
                                && (
                                "Premium payment not found with ID: "
                                        + PAYMENT_ID
                        ).equals(error.getMessage())
                )
                .verify();
    }

    // =========================================================
    // FAILED WEBHOOK
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldSaveFailedPayment() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.FAILED.name()
                );

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
        )).thenReturn(
                Mono.just(payment)
        );

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
                            TRANSACTION_REFERENCE,
                            result.getTransactionReference()
                    );

                    assertEquals(
                            PremiumPaymentStatus.FAILED.name(),
                            result.getStatus()
                    );
                })
                .verifyComplete();

        verify(premiumPaymentRepository)
                .save(payment);

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void processPremiumWebhook_ShouldNormalizeLowercaseFailedStatus() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.PENDING.name()
                );

        PremiumPaymentWebhookRequest request =
                createWebhookRequest("failed");

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
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService.processPremiumWebhook(
                                request
                        )
                )
                .assertNext(result ->
                        assertEquals(
                                PremiumPaymentStatus.FAILED.name(),
                                result.getStatus()
                        )
                )
                .verifyComplete();
    }

    // =========================================================
    // DUPLICATE TRANSACTION REFERENCE
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldRejectDuplicateTransactionReference() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.FAILED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        when(premiumPaymentRepository
                .existsByTransactionReference(
                        TRANSACTION_REFERENCE
                )).thenReturn(
                Mono.just(true)
        );

        StepVerifier.create(
                        paymentService.processPremiumWebhook(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidPaymentRequestException
                                && "Transaction reference is already in use"
                                .equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository, never())
                .save(any(PremiumPayment.class));
    }

    // =========================================================
    // FINALIZED PAYMENT PROTECTION
    // =========================================================

    @Test
    void processPremiumWebhook_ShouldRejectAlreadySuccessfulPayment() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.SUCCESS.name()
                );

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.SUCCESS.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService.processPremiumWebhook(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentAlreadyFinalizedException
                                && (
                                "Premium payment is already finalized as SUCCESS"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository, never())
                .save(any(PremiumPayment.class));
    }

    @Test
    void processPremiumWebhook_ShouldRejectAlreadyFailedPayment() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.FAILED.name()
                );

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.FAILED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService.processPremiumWebhook(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentAlreadyFinalizedException
                                && (
                                "Premium payment is already finalized as FAILED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository, never())
                .save(any(PremiumPayment.class));
    }

    @Test
    void processPremiumWebhook_ShouldRejectCancelledPayment() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.CANCELLED.name()
                );

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.FAILED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService.processPremiumWebhook(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentAlreadyFinalizedException
                                && (
                                "Premium payment is already finalized as CANCELLED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository, never())
                .save(any(PremiumPayment.class));
    }

    @Test
    void processPremiumWebhook_ShouldRejectRefundedPayment() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.REFUNDED.name()
                );

        PremiumPaymentWebhookRequest request =
                createWebhookRequest(
                        PremiumPaymentStatus.FAILED.name()
                );

        when(premiumPaymentRepository.findById(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        StepVerifier.create(
                        paymentService.processPremiumWebhook(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof PaymentAlreadyFinalizedException
                                && (
                                "Premium payment is already finalized as REFUNDED"
                        ).equals(error.getMessage())
                )
                .verify();

        verify(premiumPaymentRepository, never())
                .save(any(PremiumPayment.class));
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

    private PremiumPaymentWebhookRequest createWebhookRequest(
            String status
    ) {
        PremiumPaymentWebhookRequest request =
                new PremiumPaymentWebhookRequest();

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