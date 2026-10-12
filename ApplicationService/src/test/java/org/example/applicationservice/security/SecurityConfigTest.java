package org.example.applicationservice.security;

import org.example.applicationservice.Service.ApplicationServiceImpl;
import org.example.applicationservice.controller.ApplicationController;
import org.example.applicationservice.dto.CreateApplicationRequest;
import org.example.applicationservice.Entity.Application;
import org.example.applicationservice.enums.ApplicationStatus;
import org.example.applicationservice.Service.*;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = ApplicationController.class
)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ApplicationServiceImpl applicationService;

    @MockitoBean
    private JwtService jwtService;

    private static final Integer BUYER_USER_ID = 4;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String BUYER_TOKEN =
            "valid-buyer-token";

    private static final String ADMIN_TOKEN =
            "valid-admin-token";

    // =========================================================
    // NO TOKEN TESTS
    // =========================================================

    @Test
    void buyerEndpoint_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient.get()
                .uri("/api/v1/applications")
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(applicationService, never())
                .getMyApplications(anyInt());
    }

    @Test
    void adminEndpoint_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient.get()
                .uri("/api/v1/applications/all")
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(applicationService, never())
                .getAllApplications();
    }

    // =========================================================
    // BUYER ENDPOINT TESTS
    // =========================================================

    @Test
    void buyerEndpoint_ShouldReturnOk_WhenRoleIsBuyer() {

        Application application =
                createApplication(
                        BUYER_USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims();

        when(applicationService.getMyApplications(
                BUYER_USER_ID
        )).thenReturn(
                Flux.just(application)
        );

        webTestClient.get()
                .uri("/api/v1/applications")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$[0].userId")
                .isEqualTo(BUYER_USER_ID)
                .jsonPath("$[0].status")
                .isEqualTo("DRAFT");

        verify(applicationService)
                .getMyApplications(BUYER_USER_ID);
    }

    @Test
    void createApplication_ShouldReturnOk_WhenRoleIsBuyer() {

        CreateApplicationRequest request =
                createApplicationRequest();

        Application application =
                createApplication(
                        BUYER_USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims();

        when(applicationService.createApplication(
                BUYER_USER_ID,
                request
        )).thenReturn(
                Mono.just(application)
        );

        webTestClient.post()
                .uri("/api/v1/applications")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.userId")
                .isEqualTo(BUYER_USER_ID)
                .jsonPath("$.status")
                .isEqualTo("DRAFT");

        verify(applicationService)
                .createApplication(
                        BUYER_USER_ID,
                        request
                );
    }

    @Test
    void adminEndpoint_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient.get()
                .uri("/api/v1/applications/all")
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(applicationService, never())
                .getAllApplications();
    }

    @Test
    void updateStatus_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient.patch()
                .uri(
                        "/api/v1/applications/"
                                + APPLICATION_ID
                                + "/status"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "status": "VERIFIED"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(applicationService, never())
                .updateApplicationStatus(
                        any(),
                        any()
                );
    }

    // =========================================================
    // ADMIN ENDPOINT TESTS
    // =========================================================

    @Test
    void adminEndpoint_ShouldReturnOk_WhenRoleIsAdmin() {

        Application first =
                createApplication(
                        BUYER_USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        Application second =
                createApplication(
                        5,
                        ApplicationStatus.SUBMITTED.name()
                );

        second.setApplicationId("application-102");

        mockAdminClaims();

        when(applicationService.getAllApplications())
                .thenReturn(
                        Flux.just(first, second)
                );

        webTestClient.get()
                .uri("/api/v1/applications/all")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(2)
                .jsonPath("$[0].applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$[1].applicationId")
                .isEqualTo("application-102");

        verify(applicationService)
                .getAllApplications();
    }

    @Test
    void updateStatus_ShouldReturnOk_WhenRoleIsAdmin() {

        Application application =
                createApplication(
                        BUYER_USER_ID,
                        ApplicationStatus.VERIFICATION_IN_PROGRESS.name()
                );

        mockAdminClaims();

        when(applicationService.updateApplicationStatus(
                APPLICATION_ID,
                "VERIFICATION_IN_PROGRESS"
        )).thenReturn(
                Mono.just(application)
        );

        webTestClient.patch()
                .uri(
                        "/api/v1/applications/"
                                + APPLICATION_ID
                                + "/status"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "status": "VERIFICATION_IN_PROGRESS"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$.status")
                .isEqualTo("VERIFICATION_IN_PROGRESS");

        verify(applicationService)
                .updateApplicationStatus(
                        APPLICATION_ID,
                        "VERIFICATION_IN_PROGRESS"
                );
    }

    @Test
    void createApplication_ShouldReturnForbidden_WhenRoleIsAdmin() {

        CreateApplicationRequest request =
                createApplicationRequest();

        mockAdminClaims();

        webTestClient.post()
                .uri("/api/v1/applications")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(applicationService, never())
                .createApplication(
                        anyInt(),
                        any(CreateApplicationRequest.class)
                );
    }

    @Test
    void buyerListEndpoint_ShouldReturnForbidden_WhenRoleIsAdmin() {

        mockAdminClaims();

        webTestClient.get()
                .uri("/api/v1/applications")
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(applicationService, never())
                .getMyApplications(anyInt());
    }

    // =========================================================
    // SHARED BUYER OR ADMIN ENDPOINT TESTS
    // =========================================================

    @Test
    void getApplication_ShouldReturnOk_WhenRoleIsBuyer() {

        Application application =
                createApplication(
                        BUYER_USER_ID,
                        ApplicationStatus.DRAFT.name()
                );

        mockBuyerClaims();

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        webTestClient.get()
                .uri(
                        "/api/v1/applications/"
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
                .jsonPath("$.userId")
                .isEqualTo(BUYER_USER_ID);

        verify(applicationService)
                .getApplication(APPLICATION_ID);
    }

    @Test
    void getApplication_ShouldReturnOk_WhenRoleIsAdmin() {

        Application application =
                createApplication(
                        BUYER_USER_ID,
                        ApplicationStatus.SUBMITTED.name()
                );

        mockAdminClaims();

        when(applicationService.getApplication(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(application)
        );

        webTestClient.get()
                .uri(
                        "/api/v1/applications/"
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
                .isEqualTo(APPLICATION_ID);

        verify(applicationService)
                .getApplication(APPLICATION_ID);
    }

    // =========================================================
    // INVALID TOKEN TEST
    // =========================================================

    @Test
    void protectedEndpoint_ShouldReturnUnauthorized_WhenTokenIsInvalid() {

        String invalidToken = "invalid-token";

        when(jwtService.extractEmail(invalidToken))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid JWT"
                        )
                );

        webTestClient.get()
                .uri("/api/v1/applications")
                .cookie(
                        "accessToken",
                        invalidToken
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(applicationService, never())
                .getMyApplications(anyInt());
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
                .thenReturn("UNKNOWN");

        when(jwtService.extractUserId(token))
                .thenReturn(99);

        webTestClient.get()
                .uri("/api/v1/applications/all")
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(applicationService, never())
                .getAllApplications();
    }

    // =========================================================
    // TEST MOCK METHODS
    // =========================================================

    private void mockBuyerClaims() {

        when(jwtService.extractEmail(BUYER_TOKEN))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(BUYER_TOKEN))
                .thenReturn("BUYER");

        when(jwtService.extractUserId(BUYER_TOKEN))
                .thenReturn(BUYER_USER_ID);
    }

    private void mockAdminClaims() {

        when(jwtService.extractEmail(ADMIN_TOKEN))
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(ADMIN_TOKEN))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(ADMIN_TOKEN))
                .thenReturn(1);
    }

    // =========================================================
    // TEST DATA METHODS
    // =========================================================

    private CreateApplicationRequest createApplicationRequest() {

        CreateApplicationRequest request =
                new CreateApplicationRequest();

        request.setBuilderName(
                "Apexes Infrastructure"
        );

        request.setProjectName(
                "Skyline Heights"
        );

        request.setUnitNumber(
                "B-1404"
        );

        request.setPropertyValue(
                new BigDecimal("12500000.00")
        );

        request.setPurchaseDate(
                LocalDate.of(2024, 11, 10)
        );

        request.setPromisedPossessionDate(
                LocalDate.of(2026, 12, 31)
        );

        return request;
    }

    private Application createApplication(
            Integer userId,
            String status
    ) {

        Application application =
                new Application();

        application.setApplicationId(
                APPLICATION_ID
        );

        application.setUserId(userId);

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

        application.setStatus(status);

        return application;
    }
}