package org.example.paymentservice.config;

import org.example.paymentservice.controller.PaymentWebhookController;
import org.example.paymentservice.controller.PremiumPaymentController;
import org.example.paymentservice.dto.PremiumPaymentRequest;
import org.example.paymentservice.dto.PremiumPaymentWebhookRequest;
import org.example.paymentservice.entity.PremiumPayment;
import org.example.paymentservice.enums.PremiumPaymentStatus;
import org.example.paymentservice.exception.GlobalExceptionHandler;
import org.example.paymentservice.security.JwtService;
import org.example.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = {
                PremiumPaymentController.class,
                PaymentWebhookController.class
        }
)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    private static final String PAYMENT_ID =
            "payment-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String APPLICATION_ID =
            "application-101";

    private static final String BUYER_TOKEN =
            "valid-buyer-token";

    private static final String ADMIN_TOKEN =
            "valid-admin-token";

    // =========================================================
    // MISSING TOKEN
    // =========================================================

    @Test
    void createPremiumPayment_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .post()
                .uri("/api/v1/premium-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPremiumPaymentRequest()
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(paymentService, never())
                .createPremiumPayment(
                        any(PremiumPaymentRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void getPremiumPayment_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri(
                        "/api/v1/premium-payments/"
                                + PAYMENT_ID
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(paymentService, never())
                .getPremiumPayment(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void premiumWebhook_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .post()
                .uri("/api/v1/payment-webhooks/premium")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPremiumWebhookRequest()
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(paymentService, never())
                .processPremiumWebhook(
                        any(PremiumPaymentWebhookRequest.class)
                );
    }

    // =========================================================
    // CREATE PREMIUM PAYMENT
    // =========================================================

    @Test
    void createPremiumPayment_ShouldReturnOk_WhenRoleIsBuyer() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        mockBuyerClaims();

        when(paymentService.createPremiumPayment(
                any(PremiumPaymentRequest.class),
                anyInt(),
                anyString()
        )).thenReturn(
                Mono.just(payment)
        );

        webTestClient
                .post()
                .uri("/api/v1/premium-payments")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPremiumPaymentRequest()
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paymentId")
                .isEqualTo(PAYMENT_ID)
                .jsonPath("$.quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.premiumAmount")
                .isEqualTo(37500.0)
                .jsonPath("$.paymentProvider")
                .isEqualTo("RAZORPAY")
                .jsonPath("$.status")
                .isEqualTo("INITIATED");

        verify(paymentService)
                .createPremiumPayment(
                        any(PremiumPaymentRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void createPremiumPayment_ShouldReturnForbidden_WhenRoleIsAdmin() {

        mockAdminClaims();

        webTestClient
                .post()
                .uri("/api/v1/premium-payments")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPremiumPaymentRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(paymentService, never())
                .createPremiumPayment(
                        any(PremiumPaymentRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // GET PREMIUM PAYMENT
    // =========================================================

    @Test
    void getPremiumPayment_ShouldReturnOk_WhenRoleIsBuyer() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.INITIATED.name()
                );

        mockBuyerClaims();

        when(paymentService.getPremiumPayment(
                PAYMENT_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.just(payment)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/premium-payments/"
                                + PAYMENT_ID
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paymentId")
                .isEqualTo(PAYMENT_ID)
                .jsonPath("$.quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$.status")
                .isEqualTo("INITIATED");

        verify(paymentService)
                .getPremiumPayment(
                        PAYMENT_ID,
                        4,
                        "BUYER"
                );
    }

    @Test
    void getPremiumPayment_ShouldReturnOk_WhenRoleIsAdmin() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.SUCCESS.name()
                );

        payment.setTransactionReference(
                "TXN-101"
        );

        mockAdminClaims();

        when(paymentService.getPremiumPayment(
                PAYMENT_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Mono.just(payment)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/premium-payments/"
                                + PAYMENT_ID
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paymentId")
                .isEqualTo(PAYMENT_ID)
                .jsonPath("$.status")
                .isEqualTo("SUCCESS")
                .jsonPath("$.transactionReference")
                .isEqualTo("TXN-101");

        verify(paymentService)
                .getPremiumPayment(
                        PAYMENT_ID,
                        1,
                        "ADMIN"
                );
    }

    // =========================================================
    // PREMIUM WEBHOOK
    // =========================================================

    @Test
    void premiumWebhook_ShouldReturnOk_WhenRoleIsAdmin() {

        PremiumPayment payment =
                createPremiumPayment(
                        PremiumPaymentStatus.SUCCESS.name()
                );

        payment.setTransactionReference(
                "TXN-PREMIUM-101"
        );

        mockAdminClaims();

        when(paymentService.processPremiumWebhook(
                any(PremiumPaymentWebhookRequest.class)
        )).thenReturn(
                Mono.just(payment)
        );

        webTestClient
                .post()
                .uri("/api/v1/payment-webhooks/premium")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPremiumWebhookRequest()
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paymentId")
                .isEqualTo(PAYMENT_ID)
                .jsonPath("$.transactionReference")
                .isEqualTo("TXN-PREMIUM-101")
                .jsonPath("$.status")
                .isEqualTo("SUCCESS");

        verify(paymentService)
                .processPremiumWebhook(
                        any(PremiumPaymentWebhookRequest.class)
                );
    }

    @Test
    void premiumWebhook_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri("/api/v1/payment-webhooks/premium")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPremiumWebhookRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(paymentService, never())
                .processPremiumWebhook(
                        any(PremiumPaymentWebhookRequest.class)
                );
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Test
    void paymentEndpoint_ShouldReturnUnauthorized_WhenTokenIsInvalid() {

        String invalidToken =
                "invalid-token";

        when(jwtService.extractEmail(invalidToken))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid JWT"
                        )
                );

        webTestClient
                .get()
                .uri(
                        "/api/v1/premium-payments/"
                                + PAYMENT_ID
                )
                .cookie(
                        "accessToken",
                        invalidToken
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(paymentService, never())
                .getPremiumPayment(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // UNSUPPORTED ROLE
    // =========================================================

    @Test
    void premiumWebhook_ShouldReturnForbidden_WhenRoleIsUnsupported() {

        String token =
                "unsupported-role-token";

        when(jwtService.extractEmail(token))
                .thenReturn(
                        "claims@gmail.com"
                );

        when(jwtService.extractRole(token))
                .thenReturn(
                        "CLAIMS_OFFICER"
                );

        when(jwtService.extractUserId(token))
                .thenReturn(
                        10
                );

        webTestClient
                .post()
                .uri("/api/v1/payment-webhooks/premium")
                .cookie(
                        "accessToken",
                        token
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPremiumWebhookRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(paymentService, never())
                .processPremiumWebhook(
                        any(PremiumPaymentWebhookRequest.class)
                );
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    @Test
    void createPremiumPayment_ShouldReturnBadRequest_WhenQuoteIdIsBlank() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        request.setQuoteId("");

        mockBuyerClaims();

        webTestClient
                .post()
                .uri("/api/v1/premium-payments")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .createPremiumPayment(
                        any(PremiumPaymentRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void createPremiumPayment_ShouldReturnBadRequest_WhenProviderIsBlank() {

        PremiumPaymentRequest request =
                createPremiumPaymentRequest();

        request.setPaymentProvider("");

        mockBuyerClaims();

        webTestClient
                .post()
                .uri("/api/v1/premium-payments")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .createPremiumPayment(
                        any(PremiumPaymentRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void premiumWebhook_ShouldReturnBadRequest_WhenPaymentIdIsBlank() {

        PremiumPaymentWebhookRequest request =
                createPremiumWebhookRequest();

        request.setPaymentId("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri("/api/v1/payment-webhooks/premium")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .processPremiumWebhook(
                        any(PremiumPaymentWebhookRequest.class)
                );
    }

    @Test
    void premiumWebhook_ShouldReturnBadRequest_WhenTransactionReferenceIsBlank() {

        PremiumPaymentWebhookRequest request =
                createPremiumWebhookRequest();

        request.setTransactionReference("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri("/api/v1/payment-webhooks/premium")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .processPremiumWebhook(
                        any(PremiumPaymentWebhookRequest.class)
                );
    }

    // =========================================================
    // JWT HELPERS
    // =========================================================

    private void mockBuyerClaims() {

        when(jwtService.extractEmail(
                BUYER_TOKEN
        )).thenReturn(
                "buyer@gmail.com"
        );

        when(jwtService.extractRole(
                BUYER_TOKEN
        )).thenReturn(
                "BUYER"
        );

        when(jwtService.extractUserId(
                BUYER_TOKEN
        )).thenReturn(
                4
        );
    }

    private void mockAdminClaims() {

        when(jwtService.extractEmail(
                ADMIN_TOKEN
        )).thenReturn(
                "admin@gmail.com"
        );

        when(jwtService.extractRole(
                ADMIN_TOKEN
        )).thenReturn(
                "ADMIN"
        );

        when(jwtService.extractUserId(
                ADMIN_TOKEN
        )).thenReturn(
                1
        );
    }

    // =========================================================
    // REQUEST HELPERS
    // =========================================================

    private PremiumPaymentRequest
    createPremiumPaymentRequest() {

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
    createPremiumWebhookRequest() {

        PremiumPaymentWebhookRequest request =
                new PremiumPaymentWebhookRequest();

        request.setPaymentId(
                PAYMENT_ID
        );

        request.setTransactionReference(
                "TXN-PREMIUM-101"
        );

        request.setPaymentStatus(
                PremiumPaymentStatus.SUCCESS.name()
        );

        return request;
    }

    // =========================================================
    // ENTITY HELPER
    // =========================================================

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