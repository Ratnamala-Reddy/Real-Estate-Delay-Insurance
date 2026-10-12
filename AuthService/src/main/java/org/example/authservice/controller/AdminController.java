package org.example.authservice.controller;

import org.example.authservice.dto.UserProfileResponse;
import org.example.authservice.dto.UserStatusUpdateRequest;
import org.example.authservice.entity.User;
import org.example.authservice.service.AuthService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AuthService authService;

    public AdminController(
            AuthService authService
    ) {
        this.authService = authService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public Flux<UserProfileResponse> getAllUsers() {

        return authService.getAllUsers();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{userId}/status")
    public Mono<User> updateUserStatus(
            @PathVariable Integer userId,
            @RequestBody UserStatusUpdateRequest request
    ) {

        return authService.updateUserStatus(
                userId,
                request
        );
    }

}