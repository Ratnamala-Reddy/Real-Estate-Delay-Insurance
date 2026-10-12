package org.example.verifyservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    public String generateServiceToken() {

        Date issuedAt = new Date();

        Date expiration =
                new Date(
                        issuedAt.getTime()
                                + 300000L
                );

        return Jwts.builder()
                .subject("verification-service")
                .claim("role", "ADMIN")
                .claim("userId", 0)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(
                        getSigningKey(),
                        Jwts.SIG.HS256
                )
                .compact();
    }
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Claims extractAllClaims(
            String token
    ) {
        if (token != null
                && token.startsWith("Bearer ")) {

            token = token.substring(7);
        }

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Integer extractUserId(
            String token
    ) {
        Object userId =
                extractAllClaims(token)
                        .get("userId");

        if (userId == null) {
            throw new IllegalArgumentException(
                    "userId claim is missing from JWT"
            );
        }

        if (userId instanceof Number number) {
            return number.intValue();
        }

        return Integer.valueOf(
                userId.toString()
        );
    }

    public String extractEmail(
            String token
    ) {
        return extractAllClaims(token)
                .getSubject();
    }

    public String extractRole(
            String token
    ) {
        return extractAllClaims(token)
                .get("role", String.class);
    }
}