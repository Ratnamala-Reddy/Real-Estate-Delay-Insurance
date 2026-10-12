package org.example.policyservice.security;

public record AuthenticatedUser(
        Integer userId,
        String email,
        String role
) {
}