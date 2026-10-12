package org.example.authservice.config;

import org.example.authservice.controller.AdminController;
import org.example.authservice.controller.UserController;
import org.example.authservice.dto.UserProfileResponse;
import org.example.authservice.security.JwtService;
import org.example.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = {
                AdminController.class,
                UserController.class
        }
)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    // =========================================================
    // NO TOKEN TESTS
    // =========================================================

    @Test
    void adminEndpoint_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient.get()
                .uri("/admin/users")
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(authService, never())
                .getAllUsers();
    }

    @Test
    void buyerEndpoint_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient.get()
                .uri("/api/v1/users/me")
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(authService, never())
                .getCurrentUser(anyString());
    }

    // =========================================================
    // ADMIN ENDPOINT TESTS
    // =========================================================

    @Test
    void adminEndpoint_ShouldReturnOk_WhenRoleIsAdmin() {

        String token = "valid-admin-token";
        String email = "admin@gmail.com";

        UserProfileResponse admin =
                createUserProfile(
                        "1",
                        "admin@gmail.com",
                        "ADMIN"
                );

        UserProfileResponse buyer =
                createUserProfile(
                        "2",
                        "buyer@gmail.com",
                        "BUYER"
                );

        when(jwtService.extractEmail(token))
                .thenReturn(email);

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        when(authService.getAllUsers())
                .thenReturn(
                        Flux.just(
                                admin,
                                buyer
                        )
                );

        webTestClient.get()
                .uri("/admin/users")
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(UserProfileResponse.class)
                .hasSize(2);

        verify(jwtService, times(2))
                .extractEmail(token);

        verify(jwtService, times(2))
                .extractRole(token);

        verify(authService)
                .getAllUsers();
    }

    @Test
    void adminEndpoint_ShouldReturnForbidden_WhenRoleIsBuyer() {

        String token = "valid-buyer-token";

        when(jwtService.extractEmail(token))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("BUYER");

        webTestClient.get()
                .uri("/admin/users")
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(jwtService)
                .extractEmail(token);

        verify(jwtService)
                .extractRole(token);

        verify(authService, never())
                .getAllUsers();
    }

    // =========================================================
    // BUYER ENDPOINT TESTS
    // =========================================================

    @Test
    void buyerEndpoint_ShouldReturnOk_WhenRoleIsBuyer() {

        String token = "valid-buyer-token";
        String email = "buyer@gmail.com";

        UserProfileResponse buyer =
                createUserProfile(
                        "1",
                        email,
                        "BUYER"
                );

        when(jwtService.extractEmail(token))
                .thenReturn(email);

        when(jwtService.extractRole(token))
                .thenReturn("BUYER");

        when(authService.getCurrentUser(email))
                .thenReturn(
                        Mono.just(buyer)
                );

        webTestClient.get()
                .uri("/api/v1/users/me")
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.userId")
                .isEqualTo("1")
                .jsonPath("$.email")
                .isEqualTo("buyer@gmail.com")
                .jsonPath("$.role")
                .isEqualTo("BUYER")
                .jsonPath("$.status")
                .isEqualTo("ACTIVE");

        /*
         * extractEmail is called twice:
         * 1. JwtAuthenticationFilter
         * 2. UserController
         */
        verify(jwtService, times(3))
                .extractEmail(token);

        verify(jwtService, times(2))
                .extractRole(token);

        verify(authService)
                .getCurrentUser(email);
    }

    @Test
    void buyerEndpoint_ShouldReturnForbidden_WhenRoleIsAdmin() {
        String token = "valid-admin-token";
        when(jwtService.extractEmail(token))
                .thenReturn("admin@gmail.com");
        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");
        webTestClient.get()
                .uri("/api/v1/users/me")
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(jwtService, times(2))
                .extractEmail(token);

        verify(jwtService, times(2))
                .extractRole(token);

        verify(authService, never())
                .getCurrentUser(anyString());
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
                .uri("/api/v1/users/me")
                .cookie(
                        "accessToken",
                        invalidToken
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(jwtService)
                .extractEmail(invalidToken);

        verify(jwtService, never())
                .extractRole(invalidToken);

        verify(authService, never())
                .getCurrentUser(anyString());
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

        webTestClient.get()
                .uri("/admin/users")
                .cookie(
                        "accessToken",
                        token
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(jwtService)
                .extractEmail(token);

        verify(jwtService)
                .extractRole(token);

        verify(authService, never())
                .getAllUsers();
    }

    // =========================================================
    // TEST DATA METHOD
    // =========================================================

    private UserProfileResponse createUserProfile(
            String userId,
            String email,
            String role
    ) {
        return UserProfileResponse.builder()
                .userId(userId)
                .firstName("Test")
                .lastName("User")
                .email(email)
                .mobileNumber("9876543210")
                .role(role)
                .status("ACTIVE")
                .build();
    }
}