package org.example.authservice.controller;

import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.RegisterResponse;
import org.example.authservice.exception.InvalidCredentialsException;
import org.example.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    // =========================================================
    // REGISTER TESTS
    // =========================================================

    @Test
    void register_ShouldReturnRegisterResponse_WhenRegistrationSucceeds() {

        RegisterRequest request = createRegisterRequest();

        RegisterResponse expectedResponse =
                RegisterResponse.builder()
                        .userId("1")
                        .message("User registered successfully")
                        .build();

        when(authService.register(request))
                .thenReturn(Mono.just(expectedResponse));

        StepVerifier.create(authController.register(request))
                .assertNext(response -> {
                    assertEquals(
                            "1",
                            response.getUserId()
                    );

                    assertEquals(
                            "User registered successfully",
                            response.getMessage()
                    );

                })
                .verifyComplete();

        verify(authService).register(request);
    }

    @Test
    void register_ShouldPropagateException_WhenServiceFails() {

        RegisterRequest request = createRegisterRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Registration failed"
                );

        when(authService.register(request))
                .thenReturn(Mono.error(exception));

        StepVerifier.create(authController.register(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException
                                && error.getMessage().equals(
                                "Registration failed"
                        )
                )
                .verify();

        verify(authService).register(request);
    }

    // =========================================================
    // LOGIN TESTS
    // =========================================================

    @Test
    void login_ShouldReturnOkResponseAndAccessTokenCookie() {

        LoginRequest request = createLoginRequest();

        LoginResponse loginResponse =
                LoginResponse.builder()
                        .message("Login successful")
                        .role("BUYER")
                        .token("test-jwt-token")
                        .build();

        when(authService.login(request))
                .thenReturn(Mono.just(loginResponse));

        StepVerifier.create(authController.login(request))
                .assertNext(response -> {
                    assertEquals(
                            HttpStatus.OK,
                            response.getStatusCode()
                    );

                    assertNotNull(response.getBody());

                    assertEquals(
                            "Login successful",
                            response.getBody().getMessage()
                    );

                    assertEquals(
                            "BUYER",
                            response.getBody().getRole()
                    );

                    assertEquals(
                            "test-jwt-token",
                            response.getBody().getToken()
                    );

                    String setCookieHeader =
                            response.getHeaders()
                                    .getFirst(
                                            HttpHeaders.SET_COOKIE
                                    );

                    assertNotNull(setCookieHeader);

                    assertTrue(
                            setCookieHeader.contains(
                                    "accessToken=test-jwt-token"
                            )
                    );

                    assertTrue(
                            setCookieHeader.contains(
                                    "HttpOnly"
                            )
                    );

                    assertTrue(
                            setCookieHeader.contains(
                                    "Path=/"
                            )
                    );

                    assertTrue(
                            setCookieHeader.contains(
                                    "SameSite=Lax"
                            )
                    );
                })
                .verifyComplete();

        verify(authService).login(request);
    }

    @Test
    void login_ShouldReturnAdminRole_WhenAdminLogsIn() {

        LoginRequest request = createLoginRequest();
        request.setEmail("admin@gmail.com");

        LoginResponse loginResponse =
                LoginResponse.builder()
                        .message("Login successful")
                        .role("ADMIN")
                        .token("admin-jwt-token")
                        .build();

        when(authService.login(request))
                .thenReturn(Mono.just(loginResponse));

        StepVerifier.create(authController.login(request))
                .assertNext(response -> {
                    assertEquals(
                            HttpStatus.OK,
                            response.getStatusCode()
                    );

                    assertNotNull(response.getBody());

                    assertEquals(
                            "ADMIN",
                            response.getBody().getRole()
                    );

                    String setCookieHeader =
                            response.getHeaders()
                                    .getFirst(
                                            HttpHeaders.SET_COOKIE
                                    );

                    assertNotNull(setCookieHeader);

                    assertTrue(
                            setCookieHeader.contains(
                                    "accessToken=admin-jwt-token"
                            )
                    );
                })
                .verifyComplete();

        verify(authService).login(request);
    }

    @Test
    void login_ShouldPropagateInvalidCredentialsException() {

        LoginRequest request = createLoginRequest();

        when(authService.login(request))
                .thenReturn(
                        Mono.error(
                                new InvalidCredentialsException(
                                        "Invalid email or password"
                                )
                        )
                );

        StepVerifier.create(authController.login(request))
                .expectErrorMatches(error ->
                        error instanceof InvalidCredentialsException
                                && error.getMessage().equals(
                                "Invalid email or password"
                        )
                )
                .verify();

        verify(authService).login(request);
    }

    // =========================================================
    // LOGOUT TESTS
    // =========================================================

    @Test
    void logout_ShouldReturnNoContentResponse() {

        Mono<ResponseEntity<Void>> result =
                authController.logout();

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(
                            HttpStatus.NO_CONTENT,
                            response.getStatusCode()
                    );

                    assertFalse(
                            response.hasBody()
                    );
                })
                .verifyComplete();
    }

    @Test
    void logout_ShouldExpireAccessTokenCookie() {

        StepVerifier.create(authController.logout())
                .assertNext(response -> {
                    String setCookieHeader =
                            response.getHeaders()
                                    .getFirst(
                                            HttpHeaders.SET_COOKIE
                                    );

                    assertNotNull(setCookieHeader);

                    assertTrue(
                            setCookieHeader.contains(
                                    "accessToken="
                            )
                    );

                    assertTrue(
                            setCookieHeader.contains(
                                    "Max-Age=0"
                            )
                    );

                    assertTrue(
                            setCookieHeader.contains(
                                    "HttpOnly"
                            )
                    );

                    assertTrue(
                            setCookieHeader.contains(
                                    "Path=/"
                            )
                    );

                    assertTrue(
                            setCookieHeader.contains(
                                    "SameSite=Lax"
                            )
                    );
                })
                .verifyComplete();
    }

    // =========================================================
    // TEST DATA METHODS
    // =========================================================

    private RegisterRequest createRegisterRequest() {

        RegisterRequest request = new RegisterRequest();

        request.setFirstName("Test");
        request.setLastName("Buyer");
        request.setEmail("buyer@gmail.com");
        request.setMobileNumber("9876543210");
        request.setPassword("Password@123");

        return request;
    }

    private LoginRequest createLoginRequest() {

        LoginRequest request = new LoginRequest();

        request.setEmail("buyer@gmail.com");
        request.setPassword("Password@123");

        return request;
    }
}