package org.example.paymentservice.security;

public record AuthenticatedUser(
        Integer userId,
        String email,
        String role
) {
}