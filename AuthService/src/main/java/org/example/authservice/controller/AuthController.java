package org.example.authservice.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.RegisterResponse;
import org.example.authservice.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Mono<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request)
                .map(loginResponse -> {
                    ResponseCookie accessTokenCookie =
                            ResponseCookie.from(
                                            "accessToken",
                                            loginResponse.getToken()
                                    )
                                    .httpOnly(true)
                                    .secure(false)
                                    .path("/")
                                    .maxAge(Duration.ofDays(1))
                                    .sameSite("Lax")
                                    .build();

                    log.info(
                            "Access token cookie created for role {}",
                            loginResponse.getRole()
                    );

                    return ResponseEntity.ok()
                            .header(
                                    HttpHeaders.SET_COOKIE,
                                    accessTokenCookie.toString()
                            )
                            .body(loginResponse);
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout() {
        log.info("Logout requested");

        ResponseCookie expiredCookie =
                ResponseCookie.from("accessToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .maxAge(Duration.ZERO)
                        .sameSite("Lax")
                        .build();

        return Mono.just(
                ResponseEntity.noContent()
                        .header(
                                HttpHeaders.SET_COOKIE,
                                expiredCookie.toString()
                        )
                        .build()
        );
    }
}