package org.example.authservice.service;

import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.RegisterResponse;
import org.example.authservice.dto.UpdateProfileRequest;
import org.example.authservice.dto.UserProfileResponse;
import org.example.authservice.dto.UserStatusUpdateRequest;
import org.example.authservice.entity.User;
import org.example.authservice.enums.Role;
import org.example.authservice.enums.UserStatus;
import org.example.authservice.exception.AccountNotActiveException;
import org.example.authservice.exception.InvalidCredentialsException;
import org.example.authservice.exception.InvalidUserStatusException;
import org.example.authservice.exception.UserAlreadyExistsException;
import org.example.authservice.exception.UserNotFoundException;
import org.example.authservice.repository.UserRepository;
import org.example.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                jwtService,
                passwordEncoder,
                r2dbcEntityTemplate
        );
    }

    // =========================================================
    // REGISTER TESTS
    // =========================================================

    @Test
    void register_ShouldRegisterUser_WhenEmailAndMobileAreNew() {

        RegisterRequest request = createRegisterRequest();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Mono.empty());

        when(userRepository.findByMobileNumber(
                request.getMobileNumber()
        )).thenReturn(Mono.empty());

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setUserid(1);
                    return Mono.just(user);
                });

        Mono<RegisterResponse> result =
                authService.register(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("1", response.getUserId());

                    assertEquals(
                            "User registered successfully",
                            response.getMessage()
                    );

                })
                .verifyComplete();

        verify(userRepository)
                .findByEmail(request.getEmail());

        verify(userRepository)
                .findByMobileNumber(
                        request.getMobileNumber()
                );

        verify(passwordEncoder)
                .encode(request.getPassword());

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void register_ShouldThrowUserAlreadyExists_WhenEmailExists() {

        RegisterRequest request = createRegisterRequest();
        User existingUser = createBuyer();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Mono.just(existingUser));

        /*
         * These stubs are required because the current register()
         * method constructs its fallback publishers immediately.
         */
        when(userRepository.findByMobileNumber(
                request.getMobileNumber()
        )).thenReturn(Mono.empty());

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(authService.register(request))
                .expectErrorMatches(error ->
                        error instanceof UserAlreadyExistsException
                                && error.getMessage().equals(
                                "Email already registered"
                        )
                )
                .verify();
    }

    @Test
    void register_ShouldThrowUserAlreadyExists_WhenMobileExists() {

        RegisterRequest request = createRegisterRequest();
        User existingUser = createBuyer();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Mono.empty());

        when(userRepository.findByMobileNumber(
                request.getMobileNumber()
        )).thenReturn(Mono.just(existingUser));

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(authService.register(request))
                .expectErrorMatches(error ->
                        error instanceof UserAlreadyExistsException
                                && error.getMessage().equals(
                                "Mobile number already registered"
                        )
                )
                .verify();
    }

    // =========================================================
    // LOGIN TESTS
    // =========================================================

    @Test
    void login_ShouldReturnLoginResponse_WhenCredentialsAreValid() {

        LoginRequest request = createLoginRequest();
        User user = createBuyer();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Mono.just(user));

        when(passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )).thenReturn(true);

        when(jwtService.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getUserid()
        )).thenReturn("test-jwt-token");

        Mono<LoginResponse> result =
                authService.login(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(
                            "Login successful",
                            response.getMessage()
                    );

                    assertEquals(
                            "BUYER",
                            response.getRole()
                    );

                    assertEquals(
                            "test-jwt-token",
                            response.getToken()
                    );
                })
                .verifyComplete();

        verify(jwtService).generateToken(
                user.getEmail(),
                "BUYER",
                user.getUserid()
        );
    }

    @Test
    void login_ShouldThrowInvalidCredentials_WhenEmailDoesNotExist() {

        LoginRequest request = createLoginRequest();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Mono.empty());

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof InvalidCredentialsException
                                && error.getMessage().equals(
                                "Invalid email or password"
                        )
                )
                .verify();

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(
                        anyString(),
                        anyString(),
                        anyInt()
                );
    }

    @Test
    void login_ShouldThrowInvalidCredentials_WhenPasswordIsIncorrect() {

        LoginRequest request = createLoginRequest();
        User user = createBuyer();

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Mono.just(user));

        when(passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )).thenReturn(false);

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof InvalidCredentialsException
                                && error.getMessage().equals(
                                "Invalid email or password"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateToken(
                        anyString(),
                        anyString(),
                        anyInt()
                );
    }

    @Test
    void login_ShouldThrowAccountNotActive_WhenUserIsSuspended() {

        LoginRequest request = createLoginRequest();
        User user = createBuyer();

        user.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Mono.just(user));

        when(passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )).thenReturn(true);

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof AccountNotActiveException
                                && error.getMessage().equals(
                                "Your account is suspended"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateToken(
                        anyString(),
                        anyString(),
                        anyInt()
                );
    }

    @Test
    void login_ShouldThrowAccountNotActive_WhenUserIsLocked() {

        LoginRequest request = createLoginRequest();
        User user = createBuyer();

        user.setStatus(UserStatus.LOCKED);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Mono.just(user));

        when(passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )).thenReturn(true);

        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof AccountNotActiveException
                                && error.getMessage().equals(
                                "Your account is locked"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateToken(
                        anyString(),
                        anyString(),
                        anyInt()
                );
    }

    @Test
    void login_ShouldReturnAdminRole_WhenAdminCredentialsAreValid() {

        LoginRequest request = createLoginRequest();

        request.setEmail("admin@gmail.com");

        User admin = createBuyer();
        admin.setEmail("admin@gmail.com");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Mono.just(admin));

        when(passwordEncoder.matches(
                request.getPassword(),
                admin.getPassword()
        )).thenReturn(true);

        when(jwtService.generateToken(
                admin.getEmail(),
                admin.getRole().name(),
                admin.getUserid()
        )).thenReturn("admin-jwt-token");

        StepVerifier.create(authService.login(request))
                .assertNext(response -> {
                    assertEquals(
                            "Login successful",
                            response.getMessage()
                    );

                    assertEquals(
                            "ADMIN",
                            response.getRole()
                    );

                    assertEquals(
                            "admin-jwt-token",
                            response.getToken()
                    );
                })
                .verifyComplete();
    }

    // =========================================================
    // GET CURRENT USER TESTS
    // =========================================================

    @Test
    void getCurrentUser_ShouldReturnProfile_WhenUserExists() {

        User user = createBuyer();

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Mono.just(user));

        Mono<UserProfileResponse> result =
                authService.getCurrentUser(user.getEmail());

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(
                            String.valueOf(user.getUserid()),
                            response.getUserId()
                    );

                    assertEquals(
                            user.getFirstName(),
                            response.getFirstName()
                    );

                    assertEquals(
                            user.getLastName(),
                            response.getLastName()
                    );

                    assertEquals(
                            user.getEmail(),
                            response.getEmail()
                    );

                    assertEquals(
                            user.getMobileNumber(),
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
    }

    @Test
    void getCurrentUser_ShouldThrowUserNotFound_WhenUserDoesNotExist() {

        String email = "unknown@gmail.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        authService.getCurrentUser(email)
                )
                .expectErrorMatches(error ->
                        error instanceof UserNotFoundException
                                && error.getMessage().equals(
                                "User not found with email: "
                                        + email
                        )
                )
                .verify();
    }

    // =========================================================
    // UPDATE PROFILE TESTS
    // =========================================================

    @Test
    void updateProfile_ShouldUpdateProfile_WhenMobileIsUnchanged() {

        User user = createBuyer();

        UpdateProfileRequest request =
                createUpdateProfileRequest();

        request.setMobileNumber(
                user.getMobileNumber()
        );

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Mono.just(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        authService.updateProfile(
                                user.getEmail(),
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
                            user.getMobileNumber(),
                            response.getMobileNumber()
                    );
                })
                .verifyComplete();

        verify(userRepository, never())
                .findByMobileNumber(anyString());

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void updateProfile_ShouldUpdateProfile_WhenNewMobileIsAvailable() {

        User user = createBuyer();

        UpdateProfileRequest request =
                createUpdateProfileRequest();

        request.setMobileNumber("9999999999");

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Mono.just(user));

        when(userRepository.findByMobileNumber("9999999999"))
                .thenReturn(Mono.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        authService.updateProfile(
                                user.getEmail(),
                                request
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            "Updated",
                            response.getFirstName()
                    );

                    assertEquals(
                            "9999999999",
                            response.getMobileNumber()
                    );
                })
                .verifyComplete();

        verify(userRepository)
                .findByMobileNumber("9999999999");

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void updateProfile_ShouldThrowUserAlreadyExists_WhenMobileExists() {

        User currentUser = createBuyer();

        UpdateProfileRequest request =
                createUpdateProfileRequest();

        request.setMobileNumber("9999999999");

        User existingUser = createBuyer();
        existingUser.setUserid(2);
        existingUser.setEmail("another@gmail.com");
        existingUser.setMobileNumber("9999999999");

        when(userRepository.findByEmail(currentUser.getEmail()))
                .thenReturn(Mono.just(currentUser));

        when(userRepository.findByMobileNumber("9999999999"))
                .thenReturn(Mono.just(existingUser));

        /*
         * Required because updateAndSaveProfile is constructed
         * immediately inside switchIfEmpty in the current service.
         */
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        authService.updateProfile(
                                currentUser.getEmail(),
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof UserAlreadyExistsException
                                && error.getMessage().equals(
                                "Mobile number already registered"
                        )
                )
                .verify();
    }

    @Test
    void updateProfile_ShouldThrowUserNotFound_WhenUserDoesNotExist() {

        String email = "unknown@gmail.com";

        UpdateProfileRequest request =
                createUpdateProfileRequest();

        when(userRepository.findByEmail(email))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        authService.updateProfile(
                                email,
                                request
                        )
                )
                .expectError(UserNotFoundException.class)
                .verify();

        verify(userRepository, never())
                .findByMobileNumber(anyString());
    }

    // =========================================================
    // GET ALL USERS TESTS
    // =========================================================

    @Test
    void getAllUsers_ShouldReturnAllUsers() {

        User buyer = createBuyer();

        User admin = createBuyer();
        admin.setUserid(2);
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setEmail("admin@gmail.com");
        admin.setMobileNumber("9999999999");
        admin.setRole(Role.ADMIN);

        when(userRepository.findAll())
                .thenReturn(
                        Flux.just(
                                buyer,
                                admin
                        )
                );

        StepVerifier.create(authService.getAllUsers())
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
    }

    @Test
    void getAllUsers_ShouldReturnEmptyFlux_WhenNoUsersExist() {

        when(userRepository.findAll())
                .thenReturn(Flux.empty());

        StepVerifier.create(authService.getAllUsers())
                .verifyComplete();
    }

    // =========================================================
    // UPDATE USER STATUS TESTS
    // =========================================================

    @Test
    void updateUserStatus_ShouldUpdateStatus_WhenRequestIsValid() {

        User user = createBuyer();

        UserStatusUpdateRequest request =
                new UserStatusUpdateRequest();

        request.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(1))
                .thenReturn(Mono.just(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        authService.updateUserStatus(
                                1,
                                request
                        )
                )
                .assertNext(updatedUser -> {
                    assertEquals(
                            UserStatus.ACTIVE,
                            updatedUser.getStatus()
                    );

                    assertNotNull(
                            updatedUser.getUpdatedAt()
                    );
                })
                .verifyComplete();

        verify(userRepository).findById(1);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserStatus_ShouldSuspendUser_WhenReasonIsProvided() {

        User user = createBuyer();

        UserStatusUpdateRequest request =
                new UserStatusUpdateRequest();

        request.setStatus(UserStatus.SUSPENDED);
        request.setReason("Suspicious account activity");

        when(userRepository.findById(1))
                .thenReturn(Mono.just(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        StepVerifier.create(
                        authService.updateUserStatus(
                                1,
                                request
                        )
                )
                .assertNext(updatedUser ->
                        assertEquals(
                                UserStatus.SUSPENDED,
                                updatedUser.getStatus()
                        )
                )
                .verifyComplete();
    }

    @Test
    void updateUserStatus_ShouldThrowInvalidStatus_WhenStatusIsNull() {

        UserStatusUpdateRequest request =
                new UserStatusUpdateRequest();

        request.setStatus(null);

        StepVerifier.create(
                        authService.updateUserStatus(
                                1,
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

        verify(userRepository, never())
                .findById(anyInt());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void updateUserStatus_ShouldThrowInvalidStatus_WhenSuspensionReasonMissing() {

        UserStatusUpdateRequest request =
                new UserStatusUpdateRequest();

        request.setStatus(UserStatus.SUSPENDED);
        request.setReason("");

        StepVerifier.create(
                        authService.updateUserStatus(
                                1,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidUserStatusException
                                && error.getMessage().equals(
                                "Reason is required when suspending a user"
                        )
                )
                .verify();

        verify(userRepository, never())
                .findById(anyInt());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void updateUserStatus_ShouldThrowUserNotFound_WhenUserDoesNotExist() {

        UserStatusUpdateRequest request =
                new UserStatusUpdateRequest();

        request.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(99))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        authService.updateUserStatus(
                                99,
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

        verify(userRepository, never())
                .save(any(User.class));
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

    private UpdateProfileRequest createUpdateProfileRequest() {

        UpdateProfileRequest request =
                new UpdateProfileRequest();

        request.setFirstName("Updated");
        request.setLastName("Buyer");
        request.setMobileNumber("9876543210");

        return request;
    }

    private User createBuyer() {

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
