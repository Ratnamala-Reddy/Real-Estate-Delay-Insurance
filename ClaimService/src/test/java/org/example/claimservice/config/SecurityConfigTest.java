package org.example.claimservice.config;

import org.example.claimservice.controller.ClaimController;
import org.example.claimservice.dto.CreateClaimRequest;
import org.example.claimservice.entity.Claim;
import org.example.claimservice.enums.ClaimStatus;
import org.example.claimservice.exceptions.GlobalExceptionHandler;
import org.example.claimservice.security.JwtService;
import org.example.claimservice.service.ClaimService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
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
        controllers = ClaimController.class
)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ClaimService claimService;

    @MockitoBean
    private JwtService jwtService;

    private static final String CLAIM_ID =
            "claim-101";

    private static final String POLICY_ID =
            "policy-101";

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
    void createClaim_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .post()
                .uri("/api/v1/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createClaimRequest()
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(claimService, never())
                .createClaim(
                        any(CreateClaimRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void getClaim_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(claimService, never())
                .getClaim(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void calculateDelay_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/delay-calculation"
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(claimService, never())
                .calculateDelay(anyString());
    }

    // =========================================================
    // CREATE CLAIM
    // =========================================================

    @Test
    void createClaim_ShouldReturnOk_WhenRoleIsBuyer() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        mockBuyerClaims();

        when(claimService.createClaim(
                any(CreateClaimRequest.class),
                anyInt(),
                anyString()
        )).thenReturn(
                Mono.just(claim)
        );

        webTestClient
                .post()
                .uri("/api/v1/claims")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createClaimRequest()
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.claimId")
                .isEqualTo(CLAIM_ID)
                .jsonPath("$.policyId")
                .isEqualTo(POLICY_ID)
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.claimedAmount")
                .isEqualTo(500000.0)
                .jsonPath("$.status")
                .isEqualTo("DOCUMENTS_SUBMITTED");

        verify(claimService)
                .createClaim(
                        any(CreateClaimRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void createClaim_ShouldReturnForbidden_WhenRoleIsAdmin() {

        mockAdminClaims();

        webTestClient
                .post()
                .uri("/api/v1/claims")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createClaimRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(claimService, never())
                .createClaim(
                        any(CreateClaimRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // GET CLAIM
    // =========================================================

    @Test
    void getClaim_ShouldReturnOk_WhenRoleIsBuyer() {

        Claim claim =
                createClaim(
                        ClaimStatus.DOCUMENTS_SUBMITTED.name()
                );

        mockBuyerClaims();

        when(claimService.getClaim(
                CLAIM_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.just(claim)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.claimId")
                .isEqualTo(CLAIM_ID)
                .jsonPath("$.policyId")
                .isEqualTo(POLICY_ID)
                .jsonPath("$.status")
                .isEqualTo("DOCUMENTS_SUBMITTED");

        verify(claimService)
                .getClaim(
                        CLAIM_ID,
                        4,
                        "BUYER"
                );
    }

    @Test
    void getClaim_ShouldReturnOk_WhenRoleIsAdmin() {

        Claim claim =
                createClaim(
                        ClaimStatus.APPROVED.name()
                );

        mockAdminClaims();

        when(claimService.getClaim(
                CLAIM_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Mono.just(claim)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.claimId")
                .isEqualTo(CLAIM_ID)
                .jsonPath("$.status")
                .isEqualTo("APPROVED");

        verify(claimService)
                .getClaim(
                        CLAIM_ID,
                        1,
                        "ADMIN"
                );
    }

    // =========================================================
    // DELAY CALCULATION
    // =========================================================

    @Test
    void calculateDelay_ShouldReturnOk_WhenRoleIsAdmin() {

        Claim claim =
                createClaim(
                        ClaimStatus.DELAY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(9);
        claim.setRemarks(
                "Delay verified: 9 month(s)"
        );
        claim.setNew(false);

        mockAdminClaims();

        when(claimService.calculateDelay(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/delay-calculation"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.eligibleDelayMonths")
                .isEqualTo(9)
                .jsonPath("$.status")
                .isEqualTo("DELAY_VERIFIED");

        verify(claimService)
                .calculateDelay(CLAIM_ID);
    }

    @Test
    void calculateDelay_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/delay-calculation"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(claimService, never())
                .calculateDelay(anyString());
    }

    // =========================================================
    // COMPENSATION CALCULATION
    // =========================================================

    @Test
    void calculateCompensation_ShouldReturnOk_WhenRoleIsAdmin() {

        Claim claim =
                createClaim(
                        ClaimStatus.DELAY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(9);
        claim.setApprovedAmount(450000.0);
        claim.setRemarks(
                "Compensation calculated for 9 eligible month(s)"
        );
        claim.setNew(false);

        mockAdminClaims();

        when(claimService.calculateCompensation(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/compensation-calculation"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.approvedAmount")
                .isEqualTo(450000.0)
                .jsonPath("$.status")
                .isEqualTo("DELAY_VERIFIED");

        verify(claimService)
                .calculateCompensation(CLAIM_ID);
    }

    @Test
    void calculateCompensation_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/compensation-calculation"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(claimService, never())
                .calculateCompensation(anyString());
    }

    // =========================================================
    // INSURANCE VERIFICATION
    // =========================================================

    @Test
    void verifyInsuranceEligibility_ShouldReturnOk_WhenRoleIsAdmin() {

        Claim claim =
                createClaim(
                        ClaimStatus.ELIGIBILITY_VERIFIED.name()
                );

        claim.setEligibleDelayMonths(9);
        claim.setApprovedAmount(450000.0);
        claim.setRemarks(
                "Insurance eligibility verified"
        );
        claim.setNew(false);

        mockAdminClaims();

        when(claimService.verifyInsuranceEligibility(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/insurance-verification"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("ELIGIBILITY_VERIFIED")
                .jsonPath("$.remarks")
                .isEqualTo("Insurance eligibility verified");

        verify(claimService)
                .verifyInsuranceEligibility(CLAIM_ID);
    }

    @Test
    void verifyInsuranceEligibility_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/insurance-verification"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(claimService, never())
                .verifyInsuranceEligibility(anyString());
    }

    // =========================================================
    // APPROVE CLAIM
    // =========================================================

    @Test
    void approveClaim_ShouldReturnOk_WhenRoleIsAdmin() {

        Claim claim =
                createClaim(
                        ClaimStatus.APPROVED.name()
                );

        claim.setApprovedAmount(450000.0);
        claim.setEligibleDelayMonths(9);
        claim.setRemarks("Claim approved");
        claim.setNew(false);

        mockAdminClaims();

        when(claimService.approveClaim(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/approve"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("APPROVED")
                .jsonPath("$.approvedAmount")
                .isEqualTo(450000.0);

        verify(claimService)
                .approveClaim(CLAIM_ID);
    }

    @Test
    void approveClaim_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/approve"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(claimService, never())
                .approveClaim(anyString());
    }

    // =========================================================
    // REJECT CLAIM
    // =========================================================

    @Test
    void rejectClaim_ShouldReturnOk_WhenRoleIsAdmin() {

        Claim claim =
                createClaim(
                        ClaimStatus.REJECTED.name()
                );

        claim.setRemarks("Claim rejected");
        claim.setNew(false);

        mockAdminClaims();

        when(claimService.rejectClaim(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/reject"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("REJECTED")
                .jsonPath("$.remarks")
                .isEqualTo("Claim rejected");

        verify(claimService)
                .rejectClaim(CLAIM_ID);
    }

    @Test
    void rejectClaim_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/reject"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(claimService, never())
                .rejectClaim(anyString());
    }

    // =========================================================
    // CLOSE CLAIM
    // =========================================================

    @Test
    void closeClaim_ShouldReturnOk_WhenRoleIsAdmin() {

        Claim claim =
                createClaim(
                        ClaimStatus.CLOSED.name()
                );

        claim.setApprovedAmount(450000.0);
        claim.setRemarks(
                "Claim closed after compensation settlement"
        );
        claim.setNew(false);

        mockAdminClaims();

        when(claimService.closeClaim(
                CLAIM_ID
        )).thenReturn(
                Mono.just(claim)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
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
                .jsonPath("$.status")
                .isEqualTo("CLOSED");

        verify(claimService)
                .closeClaim(CLAIM_ID);
    }

    @Test
    void closeClaim_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/close"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(claimService, never())
                .closeClaim(anyString());
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Test
    void claimEndpoint_ShouldReturnUnauthorized_WhenTokenIsInvalid() {

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
                        "/api/v1/claims/"
                                + CLAIM_ID
                )
                .cookie(
                        "accessToken",
                        invalidToken
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(claimService, never())
                .getClaim(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // UNSUPPORTED ROLE
    // =========================================================

    @Test
    void approveClaim_ShouldReturnForbidden_WhenRoleIsUnsupported() {

        String token =
                "unsupported-role-token";

        when(jwtService.extractEmail(token))
                .thenReturn("officer@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("CLAIMS_OFFICER");

        when(jwtService.extractUserId(token))
                .thenReturn(10);

        webTestClient
                .post()
                .uri(
                        "/api/v1/claims/"
                                + CLAIM_ID
                                + "/approve"
                )
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(claimService, never())
                .approveClaim(anyString());
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    @Test
    void createClaim_ShouldReturnBadRequest_WhenPolicyIdIsBlank() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setPolicyId("");

        mockBuyerClaims();

        webTestClient
                .post()
                .uri("/api/v1/claims")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(claimService, never())
                .createClaim(
                        any(CreateClaimRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void createClaim_ShouldReturnBadRequest_WhenReasonIsBlank() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setReason("");

        mockBuyerClaims();

        webTestClient
                .post()
                .uri("/api/v1/claims")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(claimService, never())
                .createClaim(
                        any(CreateClaimRequest.class),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void createClaim_ShouldReturnBadRequest_WhenClaimedAmountIsZero() {

        CreateClaimRequest request =
                createClaimRequest();

        request.setClaimedAmount(0.0);

        mockBuyerClaims();

        webTestClient
                .post()
                .uri("/api/v1/claims")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(claimService, never())
                .createClaim(
                        any(CreateClaimRequest.class),
                        anyInt(),
                        anyString()
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
    // REQUEST HELPER
    // =========================================================

    private CreateClaimRequest createClaimRequest() {

        CreateClaimRequest request =
                new CreateClaimRequest();

        request.setPolicyId(
                POLICY_ID
        );

        request.setReason(
                "Possession of the insured property was delayed"
        );

        request.setClaimedAmount(
                500000.0
        );

        request.setActualPossessionDate(
                LocalDate.of(
                        2026,
                        10,
                        4
                )
        );

        return request;
    }

    // =========================================================
    // CLAIM HELPER
    // =========================================================

    private Claim createClaim(
            String status
    ) {
        Claim claim =
                new Claim();

        claim.setClaimId(
                CLAIM_ID
        );

        claim.setPolicyId(
                POLICY_ID
        );

        claim.setApplicationId(
                APPLICATION_ID
        );

        claim.setReason(
                "Possession of the insured property was delayed"
        );

        claim.setClaimedAmount(
                500000.0
        );

        claim.setActualPossessionDate(
                LocalDate.of(
                        2026,
                        10,
                        4
                )
        );

        claim.setStatus(
                status
        );

        claim.setRemarks(
                "Claim submitted for review"
        );

        claim.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        5,
                        1,
                        29
                )
        );

        claim.setNew(
                ClaimStatus.DOCUMENTS_SUBMITTED.name()
                        .equals(status)
        );

        return claim;
    }
}