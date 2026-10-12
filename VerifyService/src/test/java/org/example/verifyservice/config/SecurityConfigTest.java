package org.example.verifyservice.config;

import org.example.verifyservice.controller.VerificationController;
import org.example.verifyservice.dto.AdditionalInfoRequest;
import org.example.verifyservice.dto.ApplicationResponse;
import org.example.verifyservice.dto.ApprovalRequest;
import org.example.verifyservice.dto.PropertyVerificationRequest;
import org.example.verifyservice.dto.RejectVerificationRequest;
import org.example.verifyservice.entity.VerificationRecord;
import org.example.verifyservice.enums.VerificationStatus;
import org.example.verifyservice.security.JwtService;
import org.example.verifyservice.service.VerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = VerificationController.class
)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private VerificationService verificationService;

    @MockitoBean
    private JwtService jwtService;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String ADMIN_TOKEN =
            "valid-admin-token";

    private static final String BUYER_TOKEN =
            "valid-buyer-token";

    // =========================================================
    // NO TOKEN TESTS
    // =========================================================

    @Test
    void verificationQueue_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri(
                        "/api/v1/verifications/applications"
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(verificationService, never())
                .getVerificationQueue();
    }

    @Test
    void verificationResult_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(verificationService, never())
                .getVerificationResult(
                        APPLICATION_ID
                );
    }

    // =========================================================
    // ADMIN QUEUE TEST
    // =========================================================

    @Test
    void verificationQueue_ShouldReturnOk_WhenRoleIsAdmin() {

        ApplicationResponse application =
                createApplicationResponse();

        mockAdminClaims();

        when(verificationService.getVerificationQueue())
                .thenReturn(
                        Flux.just(application)
                );

        webTestClient
                .get()
                .uri(
                        "/api/v1/verifications/applications"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$[0].status")
                .isEqualTo("SUBMITTED");

        verify(verificationService)
                .getVerificationQueue();
    }

    @Test
    void verificationQueue_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .get()
                .uri(
                        "/api/v1/verifications/applications"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(verificationService, never())
                .getVerificationQueue();
    }

    // =========================================================
    // PROPERTY VERIFICATION TESTS
    // =========================================================

    @Test
    void verifyProperty_ShouldReturnOk_WhenRoleIsAdmin() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        VerificationRecord record =
                createVerificationRecord(
                        VerificationStatus
                                .PROPERTY_VERIFIED
                                .name(),
                        request.getRemarks(),
                        request.getVerifiedBy()
                );

        mockAdminClaims();

        when(verificationService.verifyProperty(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(record)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                                + "/property-verification"
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
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.verificationStatus")
                .isEqualTo("PROPERTY_VERIFIED")
                .jsonPath("$.verifiedBy")
                .isEqualTo("ADMIN");

        verify(verificationService)
                .verifyProperty(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void verifyProperty_ShouldReturnForbidden_WhenRoleIsBuyer() {

        PropertyVerificationRequest request =
                createPropertyVerificationRequest();

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                                + "/property-verification"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(verificationService, never())
                .verifyProperty(
                        any(),
                        any(PropertyVerificationRequest.class)
                );
    }

    // =========================================================
    // REQUEST INFORMATION TESTS
    // =========================================================

    @Test
    void requestInformation_ShouldReturnOk_WhenRoleIsAdmin() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        VerificationRecord record =
                createVerificationRecord(
                        VerificationStatus
                                .ADDITIONAL_INFORMATION_REQUIRED
                                .name(),
                        request.getRemarks(),
                        request.getRequestedBy()
                );

        mockAdminClaims();

        when(verificationService.requestInformation(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(record)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                                + "/request-information"
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
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.verificationStatus")
                .isEqualTo(
                        "ADDITIONAL_INFORMATION_REQUIRED"
                );

        verify(verificationService)
                .requestInformation(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void requestInformation_ShouldReturnForbidden_WhenRoleIsBuyer() {

        AdditionalInfoRequest request =
                createAdditionalInfoRequest();

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                                + "/request-information"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(verificationService, never())
                .requestInformation(
                        any(),
                        any(AdditionalInfoRequest.class)
                );
    }

    // =========================================================
    // APPROVAL TESTS
    // =========================================================

    @Test
    void approveVerification_ShouldReturnOk_WhenRoleIsAdmin() {

        ApprovalRequest request =
                createApprovalRequest();

        VerificationRecord record =
                createVerificationRecord(
                        VerificationStatus.VERIFIED.name(),
                        request.getRemarks(),
                        request.getApprovedBy()
                );

        mockAdminClaims();

        when(verificationService.approveVerification(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(record)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                                + "/approve"
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
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.verificationStatus")
                .isEqualTo("VERIFIED");

        verify(verificationService)
                .approveVerification(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void approveVerification_ShouldReturnForbidden_WhenRoleIsBuyer() {

        ApprovalRequest request =
                createApprovalRequest();

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                                + "/approve"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(verificationService, never())
                .approveVerification(
                        any(),
                        any(ApprovalRequest.class)
                );
    }

    // =========================================================
    // REJECTION TESTS
    // =========================================================

    @Test
    void rejectApplication_ShouldReturnOk_WhenRoleIsAdmin() {

        RejectVerificationRequest request =
                createRejectVerificationRequest();

        VerificationRecord record =
                createVerificationRecord(
                        VerificationStatus.REJECTED.name(),
                        request.getRemarks(),
                        request.getRejectedBy()
                );

        mockAdminClaims();

        when(verificationService.rejectApplication(
                APPLICATION_ID,
                request
        )).thenReturn(
                Mono.just(record)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                                + "/reject"
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
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.verificationStatus")
                .isEqualTo("REJECTED");

        verify(verificationService)
                .rejectApplication(
                        APPLICATION_ID,
                        request
                );
    }

    @Test
    void rejectApplication_ShouldReturnForbidden_WhenRoleIsBuyer() {

        RejectVerificationRequest request =
                createRejectVerificationRequest();

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                                + "/reject"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(verificationService, never())
                .rejectApplication(
                        any(),
                        any(RejectVerificationRequest.class)
                );
    }

    // =========================================================
    // SHARED VERIFICATION RESULT TESTS
    // =========================================================

    @Test
    void getVerificationResult_ShouldReturnOk_WhenRoleIsAdmin() {

        VerificationRecord record =
                createVerificationRecord(
                        VerificationStatus.VERIFIED.name(),
                        "Verification completed",
                        "ADMIN"
                );

        mockAdminClaims();

        when(verificationService.getVerificationResult(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(record)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.verificationStatus")
                .isEqualTo("VERIFIED");

        verify(verificationService)
                .getVerificationResult(
                        APPLICATION_ID
                );
    }

    @Test
    void getVerificationResult_ShouldReturnOk_WhenRoleIsBuyer() {

        VerificationRecord record =
                createVerificationRecord(
                        VerificationStatus.VERIFIED.name(),
                        "Verification completed",
                        "ADMIN"
                );

        mockBuyerClaims();

        when(verificationService.getVerificationResult(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(record)
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/verifications/applications/"
                                + APPLICATION_ID
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
                .jsonPath("$.verificationStatus")
                .isEqualTo("VERIFIED");

        verify(verificationService)
                .getVerificationResult(
                        APPLICATION_ID
                );
    }

    // =========================================================
    // INVALID TOKEN TEST
    // =========================================================

    @Test
    void protectedEndpoint_ShouldReturnUnauthorized_WhenTokenIsInvalid() {

        String token = "invalid-token";

        when(jwtService.extractEmail(token))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid JWT"
                        )
                );

        webTestClient
                .get()
                .uri(
                        "/api/v1/verifications/applications"
                )
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(verificationService, never())
                .getVerificationQueue();
    }

    // =========================================================
    // UNKNOWN ROLE TEST
    // =========================================================

    @Test
    void adminEndpoint_ShouldReturnForbidden_WhenRoleIsUnknown() {

        String token = "unknown-role-token";

        when(jwtService.extractEmail(token))
                .thenReturn("unknown@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("RISK_OFFICER");

        when(jwtService.extractUserId(token))
                .thenReturn(10);

        webTestClient
                .get()
                .uri(
                        "/api/v1/verifications/applications"
                )
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(verificationService, never())
                .getVerificationQueue();
    }

    // =========================================================
    // JWT MOCK HELPERS
    // =========================================================

    private void mockAdminClaims() {

        when(jwtService.extractEmail(ADMIN_TOKEN))
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(ADMIN_TOKEN))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(ADMIN_TOKEN))
                .thenReturn(1);
    }

    private void mockBuyerClaims() {

        when(jwtService.extractEmail(BUYER_TOKEN))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(BUYER_TOKEN))
                .thenReturn("BUYER");

        when(jwtService.extractUserId(BUYER_TOKEN))
                .thenReturn(4);
    }

    // =========================================================
    // REQUEST HELPERS
    // =========================================================

    private PropertyVerificationRequest
    createPropertyVerificationRequest() {

        PropertyVerificationRequest request =
                new PropertyVerificationRequest();

        request.setRemarks(
                "Property documents verified"
        );

        request.setVerifiedBy(
                "ADMIN"
        );

        return request;
    }

    private AdditionalInfoRequest
    createAdditionalInfoRequest() {

        AdditionalInfoRequest request =
                new AdditionalInfoRequest();

        request.setRemarks(
                "Please upload the latest payment receipt"
        );

        request.setRequestedBy(
                "ADMIN"
        );

        return request;
    }

    private ApprovalRequest createApprovalRequest() {

        ApprovalRequest request =
                new ApprovalRequest();

        request.setRemarks(
                "All verification checks completed successfully"
        );

        request.setApprovedBy(
                "ADMIN"
        );

        return request;
    }

    private RejectVerificationRequest
    createRejectVerificationRequest() {

        RejectVerificationRequest request =
                new RejectVerificationRequest();

        request.setRemarks(
                "Property documents could not be validated"
        );

        request.setRejectedBy(
                "ADMIN"
        );

        return request;
    }

    // =========================================================
    // RESPONSE HELPERS
    // =========================================================

    private VerificationRecord createVerificationRecord(
            String status,
            String remarks,
            String verifiedBy
    ) {
        VerificationRecord record =
                new VerificationRecord();

        record.setVerificationId(
                "verification-101"
        );

        record.setApplicationId(
                APPLICATION_ID
        );

        record.setVerificationStatus(
                status
        );

        record.setRemarks(
                remarks
        );

        record.setVerifiedBy(
                verifiedBy
        );

        record.setVerifiedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        18,
                        30
                )
        );

        record.setNew(false);

        return record;
    }

    private ApplicationResponse createApplicationResponse() {

        ApplicationResponse application =
                new ApplicationResponse();

        application.setApplicationId(
                APPLICATION_ID
        );

        application.setUserId(
                4
        );

        application.setBuilderName(
                "Apexes Infrastructure"
        );

        application.setProjectName(
                "Skyline Heights"
        );

        application.setUnitNumber(
                "B-1404"
        );

        application.setPropertyValue(
                new BigDecimal("12500000.00")
        );

        application.setPurchaseDate(
                LocalDate.of(2024, 11, 10)
        );

        application.setPromisedPossessionDate(
                LocalDate.of(2026, 12, 31)
        );

        application.setStatus(
                "SUBMITTED"
        );

        return application;
    }
}