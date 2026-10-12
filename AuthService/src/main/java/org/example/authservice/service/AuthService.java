package org.example.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements AuthServiceImpl {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    // =========================================================
    // REGISTER USER
    // =========================================================

    @Override
    public Mono<RegisterResponse> register(
            RegisterRequest request
    ) {

        log.info(
                "Registration attempt for email {}",
                request.getEmail()
        );

        return userRepository.findByEmail(request.getEmail())

                .flatMap(existingUser -> {

                    log.warn(
                            "Registration failed. Email already exists: {}",
                            request.getEmail()
                    );

                    return Mono.<RegisterResponse>error(
                            new UserAlreadyExistsException(
                                    "Email already registered"
                            )
                    );
                })

                .switchIfEmpty(
                        userRepository
                                .findByMobileNumber(
                                        request.getMobileNumber()
                                )
                                .flatMap(existingUser -> {

                                    log.warn(
                                            "Registration failed. Mobile number already exists: {}",
                                            request.getMobileNumber()
                                    );

                                    return Mono.<RegisterResponse>error(
                                            new UserAlreadyExistsException(
                                                    "Mobile number already registered"
                                            )
                                    );
                                })
                                .switchIfEmpty(
                                        saveUser(request)
                                )
                );
    }

    private Mono<RegisterResponse> saveUser(
            RegisterRequest request
    ) {

        log.info(
                "Creating account for {}",
                request.getEmail()
        );

        User user = new User();

        user.setFirstName(
                request.getFirstName()
        );

        user.setLastName(
                request.getLastName()
        );

        user.setEmail(
                request.getEmail()
        );

        user.setMobileNumber(
                request.getMobileNumber()
        );

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(
                Role.BUYER
        );

        user.setStatus(
                UserStatus.PENDING_VERIFICATION
        );

        user.setCreatedAt(
                LocalDateTime.now()
        );

        user.setUpdatedAt(
                LocalDateTime.now()
        );

        return userRepository.save(user)

                .map(savedUser -> {

                    log.info(
                            "User registered successfully. UserId={}",
                            savedUser.getUserid()
                    );

                    return RegisterResponse.builder()
                            .userId(
                                    String.valueOf(
                                            savedUser.getUserid()
                                    )
                            )
                            .message(
                                    "User registered successfully"
                            )

                            .build();
                })

                .doOnError(error ->
                        log.error(
                                "User registration failed for {}: {}",
                                request.getEmail(),
                                error.getMessage()
                        )
                );
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Override
    public Mono<LoginResponse> login(
            LoginRequest request
    ) {

        log.info(
                "Login attempt for {}",
                request.getEmail()
        );

        return userRepository
                .findByEmail(request.getEmail())

                .switchIfEmpty(
                        Mono.error(
                                new InvalidCredentialsException(
                                        "Invalid email or password"
                                )
                        )
                )

                .flatMap(user -> {

                    boolean passwordMatches =
                            passwordEncoder.matches(
                                    request.getPassword(),
                                    user.getPassword()
                            );

                    if (!passwordMatches) {

                        log.warn(
                                "Invalid login attempt for {}",
                                request.getEmail()
                        );

                        return Mono.error(
                                new InvalidCredentialsException(
                                        "Invalid email or password"
                                )
                        );
                    }

                    if (user.getStatus() == UserStatus.SUSPENDED) {

                        log.warn(
                                "Suspended user attempted to log in. UserId={}",
                                user.getUserid()
                        );

                        return Mono.error(
                                new AccountNotActiveException(
                                        "Your account is suspended"
                                )
                        );
                    }

                    if (user.getStatus() == UserStatus.LOCKED) {

                        log.warn(
                                "Locked user attempted to log in. UserId={}",
                                user.getUserid()
                        );

                        return Mono.error(
                                new AccountNotActiveException(
                                        "Your account is locked"
                                )
                        );
                    }

                    String token =
                            jwtService.generateToken(
                                    user.getEmail(),
                                    user.getRole().name(),
                                    user.getUserid()
                            );

                    log.info(
                            "Login successful for {} with role {}",
                            user.getEmail(),
                            user.getRole()
                    );

                    return Mono.just(
                            LoginResponse.builder()
                                    .message(
                                            "Login successful"
                                    )
                                    .role(
                                            user.getRole().name()
                                    )
                                    .token(token)
                                    .build()
                    );
                })

                .doOnError(error ->
                        log.error(
                                "Login failed for {}: {}",
                                request.getEmail(),
                                error.getMessage()
                        )
                );
    }

    // =========================================================
    // GET CURRENT USER
    // =========================================================

    @Override
    public Mono<UserProfileResponse> getCurrentUser(
            String email
    ) {

        log.info(
                "Fetching profile for {}",
                email
        );

        return userRepository.findByEmail(email)

                .switchIfEmpty(
                        Mono.error(
                                new UserNotFoundException(
                                        "User not found with email: "
                                                + email
                                )
                        )
                )

                .map(this::buildUserProfileResponse)

                .doOnSuccess(response ->
                        log.info(
                                "Profile fetched successfully for {}",
                                email
                        )
                )

                .doOnError(error ->
                        log.error(
                                "Error fetching profile for {}: {}",
                                email,
                                error.getMessage()
                        )
                );
    }

    // =========================================================
    // UPDATE USER PROFILE
    // =========================================================

    @Override
    public Mono<UserProfileResponse> updateProfile(
            String email,
            UpdateProfileRequest request
    ) {

        log.info(
                "Profile update requested for {}",
                email
        );

        return userRepository.findByEmail(email)

                .switchIfEmpty(
                        Mono.error(
                                new UserNotFoundException(
                                        "User not found with email: "
                                                + email
                                )
                        )
                )

                .flatMap(user -> {

                    /*
                     * If the mobile number is not changing,
                     * update the profile directly.
                     */
                    if (user.getMobileNumber().equals(
                            request.getMobileNumber()
                    )) {
                        return updateAndSaveProfile(
                                user,
                                request
                        );
                    }

                    /*
                     * If the mobile number is changing,
                     * check whether another user already uses it.
                     */
                    return userRepository
                            .findByMobileNumber(
                                    request.getMobileNumber()
                            )

                            .flatMap(existingUser ->
                                    Mono.<User>error(
                                            new UserAlreadyExistsException(
                                                    "Mobile number already registered"
                                            )
                                    )
                            )

                            .switchIfEmpty(
                                    updateAndSaveProfile(
                                            user,
                                            request
                                    )
                            );
                })

                .map(this::buildUserProfileResponse)

                .doOnSuccess(response ->
                        log.info(
                                "Profile updated successfully for {}",
                                email
                        )
                )

                .doOnError(error ->
                        log.error(
                                "Error updating profile for {}: {}",
                                email,
                                error.getMessage()
                        )
                );
    }

    private Mono<User> updateAndSaveProfile(
            User user,
            UpdateProfileRequest request
    ) {

        user.setFirstName(
                request.getFirstName()
        );

        user.setLastName(
                request.getLastName()
        );

        user.setMobileNumber(
                request.getMobileNumber()
        );

        user.setUpdatedAt(
                LocalDateTime.now()
        );

        return userRepository.save(user);
    }

    // =========================================================
    // GET ALL USERS
    // =========================================================

    @Override
    public Flux<UserProfileResponse> getAllUsers() {

        log.info("Fetching all users");

        return userRepository.findAll()

                .map(this::buildUserProfileResponse)

                .doOnComplete(() ->
                        log.info(
                                "All users fetched successfully"
                        )
                )

                .doOnError(error ->
                        log.error(
                                "Error fetching users: {}",
                                error.getMessage()
                        )
                );
    }

    // =========================================================
    // UPDATE USER STATUS
    // =========================================================

    @Override
    public Mono<User> updateUserStatus(
            Integer userId,
            UserStatusUpdateRequest request
    ) {

        log.info(
                "Updating status for user {}",
                userId
        );

        /*
         * Prevent null status from being saved.
         */
        if (request.getStatus() == null) {

            return Mono.error(
                    new InvalidUserStatusException(
                            "User status is required"
                    )
            );
        }

        /*
         * A reason must be provided when the admin
         * suspends a user account.
         */
        if (request.getStatus() == UserStatus.SUSPENDED
                && (
                request.getReason() == null
                        || request.getReason().isBlank()
        )) {

            return Mono.error(
                    new InvalidUserStatusException(
                            "Reason is required when suspending a user"
                    )
            );
        }

        return userRepository.findById(userId)

                .switchIfEmpty(
                        Mono.error(
                                new UserNotFoundException(
                                        "User not found with userId: "
                                                + userId
                                )
                        )
                )

                .flatMap(user -> {

                    user.setStatus(
                            request.getStatus()
                    );

                    user.setUpdatedAt(
                            LocalDateTime.now()
                    );

                    return userRepository.save(user);
                })

                .doOnSuccess(user ->
                        log.info(
                                "User {} status changed to {}",
                                userId,
                                user.getStatus()
                        )
                )

                .doOnError(error ->
                        log.error(
                                "Error updating user {} status: {}",
                                userId,
                                error.getMessage()
                        )
                );
    }

    // =========================================================
    // COMMON USER RESPONSE MAPPER
    // =========================================================

    private UserProfileResponse buildUserProfileResponse(
            User user
    ) {

        return UserProfileResponse.builder()
                .userId(
                        String.valueOf(
                                user.getUserid()
                        )
                )
                .firstName(
                        user.getFirstName()
                )
                .lastName(
                        user.getLastName()
                )
                .email(
                        user.getEmail()
                )
                .mobileNumber(
                        user.getMobileNumber()
                )
                .role(
                        user.getRole().name()
                )
                .status(
                        user.getStatus().name()
                )
                .build();
    }
}