package org.example.policyservice.config;

import org.example.policyservice.controller.PolicyController;
import org.example.policyservice.dto.CreatePolicyRequest;
import org.example.policyservice.entity.Policy;
import org.example.policyservice.enums.PolicyStatus;
import org.example.policyservice.exception.GlobalExceptionHandler;
import org.example.policyservice.security.JwtService;
import org.example.policyservice.service.PolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = PolicyController.class
)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class PolicySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PolicyService policyService;

    @MockitoBean
    private JwtService jwtService;

    private static final String POLICY_ID =
            "policy-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String PAYMENT_ID =
            "payment-101";

    private static final String APPLICATION_ID =
            "application-101";

    private static final String ADMIN_TOKEN =
            "valid-admin-token";

    private static final String BUYER_TOKEN =
            "valid-buyer-token";

    // =========================================================
    // MISSING TOKEN
    // =========================================================

    @Test
    void createPolicy_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .post()
                .uri("/api/v1/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPolicyRequest()
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(policyService, never())
                .createPolicy(
                        any(CreatePolicyRequest.class)
                );
    }

    @Test
    void getPolicies_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri("/api/v1/policies")
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(policyService, never())
                .getPolicies();
    }

    @Test
    void getPolicy_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri(
                        "/api/v1/policies/"
                                + POLICY_ID
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(policyService, never())
                .getPolicy(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // CREATE POLICY
    // =========================================================

    @Test
    void createPolicy_ShouldReturnOk_WhenRoleIsAdmin() {

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        mockAdminClaims();

        when(policyService.createPolicy(
                any(CreatePolicyRequest.class)
        )).thenReturn(
                Mono.just(policy)
        );

        webTestClient
                .post()
                .uri("/api/v1/policies")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPolicyRequest()
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.policyId")
                .isEqualTo(POLICY_ID)
                .jsonPath("$.quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$.paymentId")
                .isEqualTo(PAYMENT_ID)
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.policyStatus")
                .isEqualTo("ACTIVE");

        verify(policyService)
                .createPolicy(
                        any(CreatePolicyRequest.class)
                );
    }

    @Test
    void createPolicy_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri("/api/v1/policies")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createPolicyRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(policyService, never())
                .createPolicy(
                        any(CreatePolicyRequest.class)
                );
    }

    // =========================================================
    // GET ALL POLICIES
    // =========================================================

    @Test
    void getPolicies_ShouldReturnOk_WhenRoleIsAdmin() {

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        mockAdminClaims();

        when(policyService.getPolicies())
                .thenReturn(
                        Flux.just(policy)
                );

        webTestClient
                .get()
                .uri("/api/v1/policies")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].policyId")
                .isEqualTo(POLICY_ID)
                .jsonPath("$[0].paymentId")
                .isEqualTo(PAYMENT_ID)
                .jsonPath("$[0].policyStatus")
                .isEqualTo("ACTIVE");

        verify(policyService)
                .getPolicies();
    }

    @Test
    void getPolicies_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .get()
                .uri("/api/v1/policies")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(policyService, never())
                .getPolicies();
    }

    // =========================================================
    // GET SINGLE POLICY
    // =========================================================

    @Test
    void getPolicy_ShouldReturnOk_WhenRoleIsAdmin() {

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        mockAdminClaims();

        when(policyService.getPolicy(
                POLICY_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Mono.just(policy)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/policies/"
                                + POLICY_ID
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.policyId")
                .isEqualTo(POLICY_ID)
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.policyStatus")
                .isEqualTo("ACTIVE");

        verify(policyService)
                .getPolicy(
                        POLICY_ID,
                        1,
                        "ADMIN"
                );
    }

    @Test
    void getPolicy_ShouldReturnOk_WhenRoleIsBuyer() {

        Policy policy =
                createPolicy(
                        PolicyStatus.ACTIVE.name()
                );

        mockBuyerClaims();

        when(policyService.getPolicy(
                POLICY_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.just(policy)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/policies/"
                                + POLICY_ID
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.policyId")
                .isEqualTo(POLICY_ID)
                .jsonPath("$.quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$.paymentId")
                .isEqualTo(PAYMENT_ID);

        verify(policyService)
                .getPolicy(
                        POLICY_ID,
                        4,
                        "BUYER"
                );
    }

    // =========================================================
    // COMPLETE POLICY
    // =========================================================

    @Test
    void completePolicy_ShouldReturnOk_WhenRoleIsAdmin() {

        Policy policy =
                createPolicy(
                        PolicyStatus.COMPLETED.name()
                );

        policy.setNew(false);

        mockAdminClaims();

        when(policyService.completePolicy(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/policies/"
                                + POLICY_ID
                                + "/complete"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.policyId")
                .isEqualTo(POLICY_ID)
                .jsonPath("$.policyStatus")
                .isEqualTo("COMPLETED");

        verify(policyService)
                .completePolicy(POLICY_ID);
    }

    @Test
    void completePolicy_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/policies/"
                                + POLICY_ID
                                + "/complete"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(policyService, never())
                .completePolicy(anyString());
    }

    // =========================================================
    // CLOSE POLICY
    // =========================================================

    @Test
    void closePolicy_ShouldReturnOk_WhenRoleIsAdmin() {

        Policy policy =
                createPolicy(
                        PolicyStatus.CLOSED.name()
                );

        policy.setNew(false);

        mockAdminClaims();

        when(policyService.closePolicy(
                POLICY_ID
        )).thenReturn(
                Mono.just(policy)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/policies/"
                                + POLICY_ID
                                + "/close"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.policyId")
                .isEqualTo(POLICY_ID)
                .jsonPath("$.policyStatus")
                .isEqualTo("CLOSED");

        verify(policyService)
                .closePolicy(POLICY_ID);
    }

    @Test
    void closePolicy_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/policies/"
                                + POLICY_ID
                                + "/close"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(policyService, never())
                .closePolicy(anyString());
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Test
    void policyEndpoint_ShouldReturnUnauthorized_WhenTokenIsInvalid() {

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
                        "/api/v1/policies/"
                                + POLICY_ID
                )
                .cookie(
                        "accessToken",
                        invalidToken
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(policyService, never())
                .getPolicy(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // UNSUPPORTED ROLE
    // =========================================================

    @Test
    void getPolicy_ShouldReturnForbidden_WhenRoleIsUnsupported() {

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
                .get()
                .uri(
                        "/api/v1/policies/"
                                + POLICY_ID
                )
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(policyService, never())
                .getPolicy(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    @Test
    void createPolicy_ShouldReturnBadRequest_WhenQuoteIdIsBlank() {

        CreatePolicyRequest request =
                createPolicyRequest();

        request.setQuoteId("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri("/api/v1/policies")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(policyService, never())
                .createPolicy(
                        any(CreatePolicyRequest.class)
                );
    }

    @Test
    void createPolicy_ShouldReturnBadRequest_WhenPaymentIdIsBlank() {

        CreatePolicyRequest request =
                createPolicyRequest();

        request.setPaymentId("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri("/api/v1/policies")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(policyService, never())
                .createPolicy(
                        any(CreatePolicyRequest.class)
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

    // =========================================================
    // REQUEST HELPER
    // =========================================================

    private CreatePolicyRequest createPolicyRequest() {

        CreatePolicyRequest request =
                new CreatePolicyRequest();

        request.setQuoteId(
                QUOTE_ID
        );

        request.setPaymentId(
                PAYMENT_ID
        );

        return request;
    }

    // =========================================================
    // POLICY HELPER
    // =========================================================

    private Policy createPolicy(
            String status
    ) {
        Policy policy =
                new Policy();

        policy.setPolicyId(
                POLICY_ID
        );

        policy.setApplicationId(
                APPLICATION_ID
        );

        policy.setQuoteId(
                QUOTE_ID
        );

        policy.setPaymentId(
                PAYMENT_ID
        );

        policy.setPolicyNumber(
                "POL-ABC12345"
        );

        policy.setCoverageAmount(
                1200000.0
        );

        policy.setPremiumAmount(
                37500.0
        );

        policy.setPolicyStatus(
                status
        );

        policy.setStartDate(
                LocalDate.of(
                        2026,
                        10,
                        4
                )
        );

        policy.setEndDate(
                LocalDate.of(
                        2028,
                        10,
                        4
                )
        );

        policy.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        23,
                        59
                )
        );

        policy.setNew(
                PolicyStatus.ACTIVE.name()
                        .equals(status)
        );

        return policy;
    }
}