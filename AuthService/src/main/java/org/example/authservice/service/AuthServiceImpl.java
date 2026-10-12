package org.example.authservice.service;

import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.RegisterResponse;
import org.example.authservice.dto.UpdateProfileRequest;
import org.example.authservice.dto.UserProfileResponse;
import org.example.authservice.dto.UserStatusUpdateRequest;

import org.example.authservice.entity.User;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AuthServiceImpl {

    Mono<RegisterResponse> register(
            RegisterRequest request
    );

    Mono<LoginResponse> login(
            LoginRequest request
    );

    Mono<UserProfileResponse> getCurrentUser(
            String email
    );

    Mono<UserProfileResponse> updateProfile(
            String email,
            UpdateProfileRequest request
    );


    Flux<UserProfileResponse> getAllUsers();

    Mono<User> updateUserStatus(
            Integer userId,
            UserStatusUpdateRequest request
    );

}