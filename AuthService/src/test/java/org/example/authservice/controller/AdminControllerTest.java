package org.example.authservice.controller;

import org.example.authservice.dto.UserProfileResponse;
import org.example.authservice.dto.UserStatusUpdateRequest;
import org.example.authservice.entity.User;
import org.example.authservice.enums.Role;
import org.example.authservice.enums.UserStatus;
import org.example.authservice.exception.InvalidUserStatusException;
import org.example.authservice.exception.UserNotFoundException;
import org.example.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AuthService authService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminController =
                new AdminController(authService);
    }

    // =========================================================
    // GET ALL USERS TESTS
    // =========================================================

    @Test
    void getAllUsers_ShouldReturnAllUsers() {

        UserProfileResponse buyer =
                createUserProfileResponse(
                        "1",
                        "buyer@gmail.com",
                        "BUYER"
                );

        UserProfileResponse admin =
                createUserProfileResponse(
                        "2",
                        "admin@gmail.com",
                        "ADMIN"
                );

        when(authService.getAllUsers())
                .thenReturn(
                        Flux.just(
                                buyer,
                                admin
                        )
                );

        StepVerifier.create(
                        adminController.getAllUsers()
                )
                .assertNext(response -> {
                    assertEquals(
                            "buyer@gmail.com",
                            response.getEmail()
                    );

                    assertEquals(
                            "BUYER",
                            response.getRole()
                    );
                })
                .assertNext(response -> {
                    assertEquals(
                            "admin@gmail.com",
                            response.getEmail()
                    );

                    assertEquals(
                            "ADMIN",
                            response.getRole()
                    );
                })
                .verifyComplete();

        verify(authService).getAllUsers();
    }

    @Test
    void getAllUsers_ShouldReturnEmptyFlux_WhenNoUsersExist() {

        when(authService.getAllUsers())
                .thenReturn(Flux.empty());

        StepVerifier.create(
                        adminController.getAllUsers()
                )
                .verifyComplete();

        verify(authService).getAllUsers();
    }

    @Test
    void getAllUsers_ShouldPropagateServiceException() {

        when(authService.getAllUsers())
                .thenReturn(
                        Flux.error(
                                new RuntimeException(
                                        "Unable to fetch users"
                                )
                        )
                );

        StepVerifier.create(
                        adminController.getAllUsers()
                )
                .expectErrorMatches(error ->
                        error instanceof RuntimeException
                                && error.getMessage().equals(
                                "Unable to fetch users"
                        )
                )
                .verify();
    }

    // =========================================================
    // UPDATE USER STATUS TESTS
    // =========================================================

    @Test
    void updateUserStatus_ShouldReturnUpdatedUser() {

        Integer userId = 1;

        UserStatusUpdateRequest request =
                new UserStatusUpdateRequest();

        request.setStatus(UserStatus.ACTIVE);

        User updatedUser = createUser();
        updatedUser.setStatus(UserStatus.ACTIVE);

        when(authService.updateUserStatus(
                userId,
                request
        )).thenReturn(
                Mono.just(updatedUser)
        );

        StepVerifier.create(
                        adminController.updateUserStatus(
                                userId,
                                request
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            userId,
                            response.getUserid()
                    );

                    assertEquals(
                            UserStatus.ACTIVE,
                            response.getStatus()
                    );

                    assertNotNull(
                            response.getUpdatedAt()
                    );
                })
                .verifyComplete();

        verify(authService).updateUserStatus(
                userId,
                request
        );
    }

    @Test
    void updateUserStatus_ShouldReturnSuspendedUser() {

        Integer userId = 1;

        UserStatusUpdateRequest request =
                new UserStatusUpdateRequest();

        request.setStatus(UserStatus.SUSPENDED);
        request.setReason("Policy violation");

        User updatedUser = createUser();
        updatedUser.setStatus(UserStatus.SUSPENDED);

        when(authService.updateUserStatus(
                userId,
                request
        )).thenReturn(
                Mono.just(updatedUser)
        );

        StepVerifier.create(
                        adminController.updateUserStatus(
                                userId,
                                request
                        )
                )
                .assertNext(response ->
                        assertEquals(
                                UserStatus.SUSPENDED,
                                response.getStatus()
                        )
                )
                .verifyComplete();
    }

    @Test
    void updateUserStatus_ShouldPropagateUserNotFoundException() {

        Integer userId = 99;

        UserStatusUpdateRequest request =
                new UserStatusUpdateRequest();

        request.setStatus(UserStatus.ACTIVE);

        when(authService.updateUserStatus(
                userId,
                request
        )).thenReturn(
                Mono.error(
                        new UserNotFoundException(
                                "User not found with userId: 99"
                        )
                )
        );

        StepVerifier.create(
                        adminController.updateUserStatus(
                                userId,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof UserNotFoundException
                                && error.getMessage().equals(
                                "User not found with userId: 99"
                        )
                )
                .verify();
    }

    @Test
    void updateUserStatus_ShouldPropagateInvalidStatusException() {

        Integer userId = 1;

        UserStatusUpdateRequest request =
                new UserStatusUpdateRequest();

        request.setStatus(null);

        when(authService.updateUserStatus(
                userId,
                request
        )).thenReturn(
                Mono.error(
                        new InvalidUserStatusException(
                                "User status is required"
                        )
                )
        );

        StepVerifier.create(
                        adminController.updateUserStatus(
                                userId,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidUserStatusException
                                && error.getMessage().equals(
                                "User status is required"
                        )
                )
                .verify();
    }

    // =========================================================
    // TEST DATA METHODS
    // =========================================================

    private UserProfileResponse createUserProfileResponse(
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

    private User createUser() {

        User user = new User();

        user.setUserid(1);
        user.setFirstName("Test");
        user.setLastName("Buyer");
        user.setEmail("buyer@gmail.com");
        user.setMobileNumber("9876543210");
        user.setPassword("encoded-password");
        user.setRole(Role.BUYER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return user;
    }
}