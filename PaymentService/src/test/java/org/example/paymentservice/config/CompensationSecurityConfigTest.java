package org.example.paymentservice.config;

import org.example.paymentservice.controller.CompensationPaymentController;
import org.example.paymentservice.controller.PaymentWebhookController;
import org.example.paymentservice.dto.CompensationPaymentRequest;
import org.example.paymentservice.dto.CompensationWebhookRequest;
import org.example.paymentservice.entity.CompensationPayment;
import org.example.paymentservice.enums.CompensationPaymentStatus;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = {
                CompensationPaymentController.class,
                PaymentWebhookController.class
        }
)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class CompensationSecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    private static final String PAYMENT_ID =
            "compensation-payment-101";

    private static final String CLAIM_ID =
            "claim-101";

    private static final String POLICY_ID =
            "policy-101";

    private static final String ADMIN_TOKEN =
            "valid-admin-token";

    private static final String BUYER_TOKEN =
            "valid-buyer-token";

    private static final String SERVICE_TOKEN =
            "valid-claim-service-token";

    private static final String TRANSACTION_REFERENCE =
            "TXN-COMP-101";

    // =========================================================
    // CREATE COMPENSATION PAYMENT
    // =========================================================

    @Test
    void createCompensationPayment_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .post()
                .uri("/api/v1/compensation-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createCompensationPaymentRequest()
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(paymentService, never())
                .createCompensationPayment(
                        any(CompensationPaymentRequest.class)
                );
    }

    @Test
    void createCompensationPayment_ShouldReturnOk_WhenRoleIsAdmin() {

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        mockAdminClaims();

        when(paymentService.createCompensationPayment(
                any(CompensationPaymentRequest.class)
        )).thenReturn(
                Mono.just(payment)
        );

        webTestClient
                .post()
                .uri("/api/v1/compensation-payments")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        createCompensationPaymentRequest()
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paymentId")
                .isEqualTo(PAYMENT_ID)
                .jsonPath("$.claimId")
                .isEqualTo(CLAIM_ID)
                .jsonPath("$.policyId")
                .isEqualTo(POLICY_ID)
                .jsonPath("$.payoutAmount")
                .isEqualTo(450000.0)
                .jsonPath("$.status")
                .isEqualTo("INITIATED");

        verify(paymentService)
                .createCompensationPayment(
                        any(CompensationPaymentRequest.class)
                );
    }

    @Test
    void createCompensationPayment_ShouldReturnOk_WhenInternalServiceRoleIsAdmin() {

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        mockServiceClaims();

        when(paymentService.createCompensationPayment(
                any(CompensationPaymentRequest.class)
        )).thenReturn(
                Mono.just(payment)
        );

        webTestClient
                .post()
                .uri("/api/v1/compensation-payments")
                .header(
                        "Authorization",
                        "Bearer " + SERVICE_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        createCompensationPaymentRequest()
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paymentId")
                .isEqualTo(PAYMENT_ID)
                .jsonPath("$.status")
                .isEqualTo("INITIATED");

        verify(paymentService)
                .createCompensationPayment(
                        any(CompensationPaymentRequest.class)
                );
    }

    @Test
    void createCompensationPayment_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri("/api/v1/compensation-payments")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        createCompensationPaymentRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(paymentService, never())
                .createCompensationPayment(
                        any(CompensationPaymentRequest.class)
                );
    }

    // =========================================================
    // GET COMPENSATION PAYMENT
    // =========================================================

    @Test
    void getCompensationPayment_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri(
                        "/api/v1/compensation-payments/"
                                + PAYMENT_ID
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(paymentService, never())
                .getCompensationPayment(
                        anyString()
                );
    }

    @Test
    void getCompensationPayment_ShouldReturnOk_WhenRoleIsAdmin() {

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.INITIATED.name()
                );

        mockAdminClaims();

        when(paymentService.getCompensationPayment(
                PAYMENT_ID
        )).thenReturn(
                Mono.just(payment)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/compensation-payments/"
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
                .jsonPath("$.claimId")
                .isEqualTo(CLAIM_ID)
                .jsonPath("$.payoutAmount")
                .isEqualTo(450000.0)
                .jsonPath("$.status")
                .isEqualTo("INITIATED");

        verify(paymentService)
                .getCompensationPayment(
                        PAYMENT_ID
                );
    }

    @Test
    void getCompensationPayment_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .get()
                .uri(
                        "/api/v1/compensation-payments/"
                                + PAYMENT_ID
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(paymentService, never())
                .getCompensationPayment(
                        anyString()
                );
    }

    // =========================================================
    // COMPENSATION WEBHOOK
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .post()
                .uri(
                        "/api/v1/payment-webhooks/compensation"
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        createCompensationWebhookRequest(
                                CompensationPaymentStatus.PAID.name()
                        )
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(paymentService, never())
                .processCompensationWebhook(
                        any(CompensationWebhookRequest.class)
                );
    }

    @Test
    void processCompensationWebhook_ShouldReturnOk_WhenRoleIsAdmin() {

        CompensationWebhookRequest request =
                createCompensationWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        CompensationPayment payment =
                createCompensationPayment(
                        CompensationPaymentStatus.PAID.name()
                );

        payment.setTransactionReference(
                TRANSACTION_REFERENCE
        );

        mockAdminClaims();

        when(paymentService.processCompensationWebhook(
                any(CompensationWebhookRequest.class)
        )).thenReturn(
                Mono.just(payment)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/payment-webhooks/compensation"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paymentId")
                .isEqualTo(PAYMENT_ID)
                .jsonPath("$.claimId")
                .isEqualTo(CLAIM_ID)
                .jsonPath("$.transactionReference")
                .isEqualTo(TRANSACTION_REFERENCE)
                .jsonPath("$.status")
                .isEqualTo("PAID");

        verify(paymentService)
                .processCompensationWebhook(
                        any(CompensationWebhookRequest.class)
                );
    }

    @Test
    void processCompensationWebhook_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/payment-webhooks/compensation"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        createCompensationWebhookRequest(
                                CompensationPaymentStatus.PAID.name()
                        )
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(paymentService, never())
                .processCompensationWebhook(
                        any(CompensationWebhookRequest.class)
                );
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Test
    void compensationEndpoint_ShouldReturnUnauthorized_WhenTokenIsInvalid() {

        String invalidToken =
                "invalid-token";

        when(jwtService.extractEmail(
                invalidToken
        )).thenThrow(
                new IllegalArgumentException(
                        "Invalid JWT"
                )
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/compensation-payments/"
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
                .getCompensationPayment(
                        anyString()
                );
    }

    // =========================================================
    // UNSUPPORTED ROLE
    // =========================================================

    @Test
    void createCompensationPayment_ShouldReturnForbidden_WhenRoleIsUnsupported() {

        String token =
                "unsupported-role-token";

        when(jwtService.extractEmail(
                token
        )).thenReturn(
                "officer@gmail.com"
        );

        when(jwtService.extractRole(
                token
        )).thenReturn(
                "PAYMENT_OFFICER"
        );

        when(jwtService.extractUserId(
                token
        )).thenReturn(
                10
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/compensation-payments"
                )
                .cookie(
                        "accessToken",
                        token
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        createCompensationPaymentRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(paymentService, never())
                .createCompensationPayment(
                        any(CompensationPaymentRequest.class)
                );
    }

    // =========================================================
    // CREATE REQUEST VALIDATION
    // =========================================================

    @Test
    void createCompensationPayment_ShouldReturnBadRequest_WhenClaimIdIsBlank() {

        CompensationPaymentRequest request =
                createCompensationPaymentRequest();

        request.setClaimId("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/compensation-payments"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .createCompensationPayment(
                        any(CompensationPaymentRequest.class)
                );
    }

    @Test
    void createCompensationPayment_ShouldReturnBadRequest_WhenPolicyIdIsBlank() {

        CompensationPaymentRequest request =
                createCompensationPaymentRequest();

        request.setPolicyId("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/compensation-payments"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .createCompensationPayment(
                        any(CompensationPaymentRequest.class)
                );
    }

    @Test
    void createCompensationPayment_ShouldReturnBadRequest_WhenPayoutAmountIsZero() {

        CompensationPaymentRequest request =
                createCompensationPaymentRequest();

        request.setPayoutAmount(
                0.0
        );

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/compensation-payments"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .createCompensationPayment(
                        any(CompensationPaymentRequest.class)
                );
    }

    @Test
    void createCompensationPayment_ShouldReturnBadRequest_WhenPayoutAmountIsNegative() {

        CompensationPaymentRequest request =
                createCompensationPaymentRequest();

        request.setPayoutAmount(
                -100.0
        );

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/compensation-payments"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .createCompensationPayment(
                        any(CompensationPaymentRequest.class)
                );
    }

    // =========================================================
    // WEBHOOK REQUEST VALIDATION
    // =========================================================

    @Test
    void processCompensationWebhook_ShouldReturnBadRequest_WhenPaymentIdIsBlank() {

        CompensationWebhookRequest request =
                createCompensationWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        request.setPaymentId("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/payment-webhooks/compensation"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .processCompensationWebhook(
                        any(CompensationWebhookRequest.class)
                );
    }

    @Test
    void processCompensationWebhook_ShouldReturnBadRequest_WhenTransactionReferenceIsBlank() {

        CompensationWebhookRequest request =
                createCompensationWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        request.setTransactionReference("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/payment-webhooks/compensation"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .processCompensationWebhook(
                        any(CompensationWebhookRequest.class)
                );
    }

    @Test
    void processCompensationWebhook_ShouldReturnBadRequest_WhenStatusIsBlank() {

        CompensationWebhookRequest request =
                createCompensationWebhookRequest(
                        CompensationPaymentStatus.PAID.name()
                );

        request.setPaymentStatus("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/payment-webhooks/compensation"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(paymentService, never())
                .processCompensationWebhook(
                        any(CompensationWebhookRequest.class)
                );
    }

    // =========================================================
    // JWT HELPERS
    // =========================================================

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

    private void mockServiceClaims() {

        when(jwtService.extractEmail(
                SERVICE_TOKEN
        )).thenReturn(
                "claim-service"
        );

        when(jwtService.extractRole(
                SERVICE_TOKEN
        )).thenReturn(
                "ADMIN"
        );

        when(jwtService.extractUserId(
                SERVICE_TOKEN
        )).thenReturn(
                0
        );
    }

    // =========================================================
    // REQUEST HELPERS
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

    private CompensationWebhookRequest
    createCompensationWebhookRequest(
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

    // =========================================================
    // PAYMENT HELPER
    // =========================================================

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