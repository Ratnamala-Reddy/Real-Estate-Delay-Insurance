package org.example.riskservice.config;

import org.example.riskservice.controller.RiskController;
import org.example.riskservice.dto.ApproveRiskRequest;
import org.example.riskservice.dto.DeclineRiskRequest;
import org.example.riskservice.dto.RiskAssessmentRequest;
import org.example.riskservice.entity.RiskAssessment;
import org.example.riskservice.enums.RiskAssessmentStatus;
import org.example.riskservice.enums.RiskCategory;
import org.example.riskservice.exceptions.GlobalExceptionHandler;
import org.example.riskservice.security.JwtService;
import org.example.riskservice.service.RiskService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = RiskController.class
)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RiskService riskService;

    @MockitoBean
    private JwtService jwtService;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String ASSESSMENT_ID =
            "assessment-101";

    private static final String ADMIN_TOKEN =
            "valid-admin-token";

    private static final String BUYER_TOKEN =
            "valid-buyer-token";

    // =========================================================
    // MISSING TOKEN
    // =========================================================

    @Test
    void calculateRisk_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        webTestClient
                .post()
                .uri("/api/v1/risk-assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(riskService, never())
                .calculateRisk(
                        any(RiskAssessmentRequest.class)
                );
    }

    @Test
    void getRiskAssessment_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri(
                        "/api/v1/risk-assessments/applications/"
                                + APPLICATION_ID
                                + "/risk-assessment"
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(riskService, never())
                .getRiskAssessment(APPLICATION_ID);
    }

    // =========================================================
    // CALCULATE RISK
    // =========================================================

    @Test
    void calculateRisk_ShouldReturnOk_WhenRoleIsAdmin() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        mockAdminClaims();

        when(riskService.calculateRisk(
                any(RiskAssessmentRequest.class)
        )).thenReturn(
                Mono.just(assessment)
        );

        webTestClient
                .post()
                .uri("/api/v1/risk-assessments")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.assessmentId")
                .isEqualTo(ASSESSMENT_ID)
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.riskScore")
                .isEqualTo(80)
                .jsonPath("$.riskCategory")
                .isEqualTo("LOW")
                .jsonPath("$.status")
                .isEqualTo("PENDING");

        verify(riskService)
                .calculateRisk(
                        any(RiskAssessmentRequest.class)
                );
    }

    @Test
    void calculateRisk_ShouldReturnForbidden_WhenRoleIsBuyer() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        mockBuyerClaims();

        webTestClient
                .post()
                .uri("/api/v1/risk-assessments")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(riskService, never())
                .calculateRisk(
                        any(RiskAssessmentRequest.class)
                );
    }

    // =========================================================
    // GET RISK ASSESSMENT
    // =========================================================

    @Test
    void getRiskAssessment_ShouldReturnOk_WhenRoleIsAdmin() {

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        mockAdminClaims();

        when(riskService.getRiskAssessment(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(assessment)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/risk-assessments/applications/"
                                + APPLICATION_ID
                                + "/risk-assessment"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.assessmentId")
                .isEqualTo(ASSESSMENT_ID)
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.status")
                .isEqualTo("PENDING");

        verify(riskService)
                .getRiskAssessment(APPLICATION_ID);
    }

    @Test
    void getRiskAssessment_ShouldReturnOk_WhenRoleIsBuyer() {

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.PENDING.name()
                );

        mockBuyerClaims();

        when(riskService.getRiskAssessment(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(assessment)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/risk-assessments/applications/"
                                + APPLICATION_ID
                                + "/risk-assessment"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.status")
                .isEqualTo("PENDING");

        verify(riskService)
                .getRiskAssessment(APPLICATION_ID);
    }

    // =========================================================
    // APPROVE RISK
    // =========================================================

    @Test
    void approveRisk_ShouldReturnOk_WhenRoleIsAdmin() {

        ApproveRiskRequest request =
                createApproveRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.APPROVED.name()
                );

        assessment.setRemarks(
                request.getRemarks()
        );

        assessment.setAssessedBy(
                request.getApprovedBy()
        );

        mockAdminClaims();

        when(riskService.approveRiskAssessment(
                any(String.class),
                any(ApproveRiskRequest.class)
        )).thenReturn(
                Mono.just(assessment)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/risk-assessments/"
                                + ASSESSMENT_ID
                                + "/approve"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.assessmentId")
                .isEqualTo(ASSESSMENT_ID)
                .jsonPath("$.status")
                .isEqualTo("APPROVED")
                .jsonPath("$.remarks")
                .isEqualTo(
                        "Risk assessment reviewed and approved"
                );

        verify(riskService)
                .approveRiskAssessment(
                        any(String.class),
                        any(ApproveRiskRequest.class)
                );
    }

    @Test
    void approveRisk_ShouldReturnForbidden_WhenRoleIsBuyer() {

        ApproveRiskRequest request =
                createApproveRequest();

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/risk-assessments/"
                                + ASSESSMENT_ID
                                + "/approve"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(riskService, never())
                .approveRiskAssessment(
                        any(String.class),
                        any(ApproveRiskRequest.class)
                );
    }

    // =========================================================
    // DECLINE RISK
    // =========================================================

    @Test
    void declineRisk_ShouldReturnOk_WhenRoleIsAdmin() {

        DeclineRiskRequest request =
                createDeclineRequest();

        RiskAssessment assessment =
                createRiskAssessment(
                        RiskAssessmentStatus.DECLINED.name()
                );

        assessment.setRemarks(
                request.getRemarks()
        );

        assessment.setAssessedBy(
                request.getDeclinedBy()
        );

        mockAdminClaims();

        when(riskService.declineRiskAssessment(
                any(String.class),
                any(DeclineRiskRequest.class)
        )).thenReturn(
                Mono.just(assessment)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/risk-assessments/"
                                + ASSESSMENT_ID
                                + "/decline"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.assessmentId")
                .isEqualTo(ASSESSMENT_ID)
                .jsonPath("$.status")
                .isEqualTo("DECLINED")
                .jsonPath("$.remarks")
                .isEqualTo(
                        "Risk level is not acceptable"
                );

        verify(riskService)
                .declineRiskAssessment(
                        any(String.class),
                        any(DeclineRiskRequest.class)
                );
    }

    @Test
    void declineRisk_ShouldReturnForbidden_WhenRoleIsBuyer() {

        DeclineRiskRequest request =
                createDeclineRequest();

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/risk-assessments/"
                                + ASSESSMENT_ID
                                + "/decline"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(riskService, never())
                .declineRiskAssessment(
                        any(String.class),
                        any(DeclineRiskRequest.class)
                );
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Test
    void protectedEndpoint_ShouldReturnUnauthorized_WhenTokenIsInvalid() {

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
                        "/api/v1/risk-assessments/applications/"
                                + APPLICATION_ID
                                + "/risk-assessment"
                )
                .cookie(
                        "accessToken",
                        invalidToken
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(riskService, never())
                .getRiskAssessment(APPLICATION_ID);
    }

    // =========================================================
    // UNSUPPORTED ROLE
    // =========================================================

    @Test
    void calculateRisk_ShouldReturnForbidden_WhenRoleIsUnsupported() {

        String token =
                "unsupported-role-token";

        when(jwtService.extractEmail(token))
                .thenReturn("claims@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("CLAIMS_OFFICER");

        when(jwtService.extractUserId(token))
                .thenReturn(10);

        webTestClient
                .post()
                .uri("/api/v1/risk-assessments")
                .cookie(
                        "accessToken",
                        token
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        createRiskAssessmentRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(riskService, never())
                .calculateRisk(
                        any(RiskAssessmentRequest.class)
                );
    }

    // =========================================================
    // VALIDATION THROUGH CONTROLLER
    // =========================================================

    @Test
    void calculateRisk_ShouldReturnBadRequest_WhenApplicationIdIsBlank() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setApplicationId("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri("/api/v1/risk-assessments")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(riskService, never())
                .calculateRisk(
                        any(RiskAssessmentRequest.class)
                );
    }

    @Test
    void calculateRisk_ShouldReturnBadRequest_WhenScoreExceedsMaximum() {

        RiskAssessmentRequest request =
                createRiskAssessmentRequest();

        request.setBuilderScore(101);

        mockAdminClaims();

        webTestClient
                .post()
                .uri("/api/v1/risk-assessments")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(riskService, never())
                .calculateRisk(
                        any(RiskAssessmentRequest.class)
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
    // REQUEST HELPERS
    // =========================================================

    private RiskAssessmentRequest createRiskAssessmentRequest() {

        RiskAssessmentRequest request =
                new RiskAssessmentRequest();

        request.setApplicationId(
                APPLICATION_ID
        );

        request.setAssessedBy(
                "ADMIN"
        );

        request.setBuilderScore(
                80
        );

        request.setFinancialScore(
                70
        );

        request.setProjectProgressScore(
                90
        );

        request.setLocationScore(
                60
        );

        request.setApprovalScore(
                100
        );

        return request;
    }

    private ApproveRiskRequest createApproveRequest() {

        ApproveRiskRequest request =
                new ApproveRiskRequest();

        request.setRemarks(
                "Risk assessment reviewed and approved"
        );

        request.setApprovedBy(
                "ADMIN"
        );

        return request;
    }

    private DeclineRiskRequest createDeclineRequest() {

        DeclineRiskRequest request =
                new DeclineRiskRequest();

        request.setRemarks(
                "Risk level is not acceptable"
        );

        request.setDeclinedBy(
                "ADMIN"
        );

        return request;
    }

    // =========================================================
    // RESPONSE HELPER
    // =========================================================

    private RiskAssessment createRiskAssessment(
            String status
    ) {
        RiskAssessment assessment =
                new RiskAssessment();

        assessment.setAssessmentId(
                ASSESSMENT_ID
        );

        assessment.setApplicationId(
                APPLICATION_ID
        );

        assessment.setRiskScore(
                80
        );

        assessment.setRiskCategory(
                RiskCategory.LOW.name()
        );

        assessment.setPremiumMultiplier(
                1.0
        );

        assessment.setRemarks(
                "Risk assessment calculated"
        );

        assessment.setStatus(
                status
        );

        assessment.setAssessedBy(
                "ADMIN"
        );

        assessment.setAssessedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        20,
                        30
                )
        );

        assessment.setNew(false);

        return assessment;
    }
}