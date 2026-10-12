package org.example.authservice.controller;

import org.example.authservice.dto.UpdateProfileRequest;
import org.example.authservice.dto.UserProfileResponse;
import org.example.authservice.security.JwtService;
import org.example.authservice.service.AuthService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;
    private final JwtService jwtService;

    public UserController(
            AuthService authService,
            JwtService jwtService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/me")
    public Mono<UserProfileResponse> getCurrentUser(
            @CookieValue("accessToken") String token
    ) {

        String email = jwtService.extractEmail(token);

        return authService.getCurrentUser(email);
    }

    @PreAuthorize("hasRole('BUYER')")
    @PutMapping("/me")
    public Mono<UserProfileResponse> updateProfile(
            @CookieValue("accessToken") String token,
            @RequestBody UpdateProfileRequest request
    ) {

        String email = jwtService.extractEmail(token);

        return authService.updateProfile(
                email,
                request
        );
    }




}