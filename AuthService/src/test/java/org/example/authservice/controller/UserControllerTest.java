package org.example.authservice.controller;

import org.example.authservice.dto.UpdateProfileRequest;
import org.example.authservice.dto.UserProfileResponse;
import org.example.authservice.exception.UserNotFoundException;
import org.example.authservice.security.JwtService;
import org.example.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(
                authService,
                jwtService
        );
    }

    // =========================================================
    // GET CURRENT USER TESTS
    // =========================================================

    @Test
    void getCurrentUser_ShouldReturnProfile_WhenTokenIsValid() {

        String token = "buyer-jwt-token";
        String email = "buyer@gmail.com";

        UserProfileResponse expectedResponse =
                createUserProfileResponse();

        when(jwtService.extractEmail(token))
                .thenReturn(email);

        when(authService.getCurrentUser(email))
                .thenReturn(
                        Mono.just(expectedResponse)
                );

        StepVerifier.create(
                        userController.getCurrentUser(token)
                )
                .assertNext(response -> {
                    assertEquals(
                            "1",
                            response.getUserId()
                    );

                    assertEquals(
                            "buyer@gmail.com",
                            response.getEmail()
                    );

                    assertEquals(
                            "BUYER",
                            response.getRole()
                    );

                    assertEquals(
                            "ACTIVE",
                            response.getStatus()
                    );
                })
                .verifyComplete();

        verify(jwtService).extractEmail(token);
        verify(authService).getCurrentUser(email);
    }

    @Test
    void getCurrentUser_ShouldPropagateUserNotFoundException() {

        String token = "buyer-jwt-token";
        String email = "unknown@gmail.com";

        when(jwtService.extractEmail(token))
                .thenReturn(email);

        when(authService.getCurrentUser(email))
                .thenReturn(
                        Mono.error(
                                new UserNotFoundException(
                                        "User not found with email: "
                                                + email
                                )
                        )
                );

        StepVerifier.create(
                        userController.getCurrentUser(token)
                )
                .expectErrorMatches(error ->
                        error instanceof UserNotFoundException
                                && error.getMessage().equals(
                                "User not found with email: "
                                        + email
                        )
                )
                .verify();

        verify(jwtService).extractEmail(token);
        verify(authService).getCurrentUser(email);
    }

    @Test
    void getCurrentUser_ShouldThrowException_WhenTokenIsInvalid() {

        String invalidToken = "invalid-token";

        when(jwtService.extractEmail(invalidToken))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid JWT"
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userController.getCurrentUser(
                                invalidToken
                        )
                );

        assertEquals(
                "Invalid JWT",
                exception.getMessage()
        );

        verify(jwtService).extractEmail(invalidToken);

        verify(authService, never())
                .getCurrentUser(anyString());
    }

    // =========================================================
    // UPDATE PROFILE TESTS
    // =========================================================

    @Test
    void updateProfile_ShouldReturnUpdatedProfile() {

        String token = "buyer-jwt-token";
        String email = "buyer@gmail.com";

        UpdateProfileRequest request =
                createUpdateProfileRequest();

        UserProfileResponse expectedResponse =
                createUserProfileResponse();

        expectedResponse.setFirstName("Updated");
        expectedResponse.setLastName("Buyer");
        expectedResponse.setMobileNumber("9999999999");

        when(jwtService.extractEmail(token))
                .thenReturn(email);

        when(authService.updateProfile(
                email,
                request
        )).thenReturn(
                Mono.just(expectedResponse)
        );

        StepVerifier.create(
                        userController.updateProfile(
                                token,
                                request
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            "Updated",
                            response.getFirstName()
                    );

                    assertEquals(
                            "Buyer",
                            response.getLastName()
                    );

                    assertEquals(
                            "9999999999",
                            response.getMobileNumber()
                    );

                    assertEquals(
                            "BUYER",
                            response.getRole()
                    );

                    assertEquals(
                            "ACTIVE",
                            response.getStatus()
                    );
                })
                .verifyComplete();

        verify(jwtService).extractEmail(token);

        verify(authService).updateProfile(
                email,
                request
        );
    }

    @Test
    void updateProfile_ShouldPropagateServiceException() {

        String token = "buyer-jwt-token";
        String email = "buyer@gmail.com";

        UpdateProfileRequest request =
                createUpdateProfileRequest();

        when(jwtService.extractEmail(token))
                .thenReturn(email);

        when(authService.updateProfile(
                email,
                request
        )).thenReturn(
                Mono.error(
                        new RuntimeException(
                                "Profile update failed"
                        )
                )
        );

        StepVerifier.create(
                        userController.updateProfile(
                                token,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof RuntimeException
                                && error.getMessage().equals(
                                "Profile update failed"
                        )
                )
                .verify();

        verify(jwtService).extractEmail(token);

        verify(authService).updateProfile(
                email,
                request
        );
    }

    @Test
    void updateProfile_ShouldThrowException_WhenTokenIsInvalid() {

        String invalidToken = "invalid-token";

        UpdateProfileRequest request =
                createUpdateProfileRequest();

        when(jwtService.extractEmail(invalidToken))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid JWT"
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userController.updateProfile(
                                invalidToken,
                                request
                        )
                );

        assertEquals(
                "Invalid JWT",
                exception.getMessage()
        );

        verify(jwtService).extractEmail(invalidToken);

        verify(authService, never())
                .updateProfile(
                        anyString(),
                        org.mockito.ArgumentMatchers.any(
                                UpdateProfileRequest.class
                        )
                );
    }

    // =========================================================
    // TEST DATA METHODS
    // =========================================================

    private UserProfileResponse createUserProfileResponse() {

        return UserProfileResponse.builder()
                .userId("1")
                .firstName("Test")
                .lastName("Buyer")
                .email("buyer@gmail.com")
                .mobileNumber("9876543210")
                .role("BUYER")
                .status("ACTIVE")
                .build();
    }

    private UpdateProfileRequest createUpdateProfileRequest() {

        UpdateProfileRequest request =
                new UpdateProfileRequest();

        request.setFirstName("Updated");
        request.setLastName("Buyer");
        request.setMobileNumber("9999999999");

        return request;
    }
}