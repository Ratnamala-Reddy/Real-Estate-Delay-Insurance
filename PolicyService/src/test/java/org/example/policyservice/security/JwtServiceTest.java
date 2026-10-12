package org.example.policyservice.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET =
            "mySecretKeyForRealEstateProjectDelayInsurancePlatform2026SecureKey";

    @BeforeEach
    void setUp() {

        jwtService =
                new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                TEST_SECRET
        );
    }

    // =========================================================
    // USER ID EXTRACTION
    // =========================================================

    @Test
    void extractUserId_ShouldReturnNumericUserId() {

        String token =
                createToken(
                        "admin@gmail.com",
                        "ADMIN",
                        1,
                        86400000L
                );

        Integer userId =
                jwtService.extractUserId(token);

        assertEquals(
                1,
                userId
        );
    }

    @Test
    void extractUserId_ShouldConvertStringUserId() {

        String token =
                createTokenWithStringUserId(
                        "buyer@gmail.com",
                        "BUYER",
                        "4",
                        86400000L
                );

        Integer userId =
                jwtService.extractUserId(token);

        assertEquals(
                4,
                userId
        );
    }

    @Test
    void extractUserId_ShouldThrowException_WhenClaimIsMissing() {

        String token =
                createTokenWithoutUserId(
                        "admin@gmail.com",
                        "ADMIN",
                        86400000L
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                jwtService.extractUserId(
                                        token
                                )
                );

        assertEquals(
                "userId claim is missing from JWT",
                exception.getMessage()
        );
    }

    @Test
    void extractUserId_ShouldThrowException_WhenClaimIsNotNumeric() {

        String token =
                createTokenWithStringUserId(
                        "admin@gmail.com",
                        "ADMIN",
                        "invalid-user-id",
                        86400000L
                );

        assertThrows(
                NumberFormatException.class,
                () ->
                        jwtService.extractUserId(
                                token
                        )
        );
    }

    // =========================================================
    // EMAIL EXTRACTION
    // =========================================================

    @Test
    void extractEmail_ShouldReturnCorrectEmail() {

        String token =
                createToken(
                        "admin@gmail.com",
                        "ADMIN",
                        1,
                        86400000L
                );

        String email =
                jwtService.extractEmail(token);

        assertEquals(
                "admin@gmail.com",
                email
        );
    }

    @Test
    void extractEmail_ShouldWorkWithBearerPrefix() {

        String token =
                createToken(
                        "admin@gmail.com",
                        "ADMIN",
                        1,
                        86400000L
                );

        String email =
                jwtService.extractEmail(
                        "Bearer " + token
                );

        assertEquals(
                "admin@gmail.com",
                email
        );
    }

    // =========================================================
    // ROLE EXTRACTION
    // =========================================================

    @Test
    void extractRole_ShouldReturnAdminRole() {

        String token =
                createToken(
                        "admin@gmail.com",
                        "ADMIN",
                        1,
                        86400000L
                );

        String role =
                jwtService.extractRole(token);

        assertEquals(
                "ADMIN",
                role
        );
    }

    @Test
    void extractRole_ShouldReturnBuyerRole() {

        String token =
                createToken(
                        "buyer@gmail.com",
                        "BUYER",
                        4,
                        86400000L
                );

        String role =
                jwtService.extractRole(token);

        assertEquals(
                "BUYER",
                role
        );
    }

    // =========================================================
    // COMPLETE CLAIMS
    // =========================================================

    @Test
    void token_ShouldContainAllRequiredClaims() {

        String token =
                createToken(
                        "buyer@gmail.com",
                        "BUYER",
                        4,
                        86400000L
                );

        assertEquals(
                "buyer@gmail.com",
                jwtService.extractEmail(token)
        );

        assertEquals(
                "BUYER",
                jwtService.extractRole(token)
        );

        assertEquals(
                4,
                jwtService.extractUserId(token)
        );
    }

    // =========================================================
    // SERVICE TOKEN
    // =========================================================

    @Test
    void generateServiceToken_ShouldReturnToken() {

        String token =
                jwtService.generateServiceToken();

        assertNotNull(token);

        assertFalse(
                token.isBlank()
        );
    }

    @Test
    void generateServiceToken_ShouldContainPolicyServiceSubject() {

        String token =
                jwtService.generateServiceToken();

        assertEquals(
                "policy-service",
                jwtService.extractEmail(token)
        );
    }

    @Test
    void generateServiceToken_ShouldContainAdminRole() {

        String token =
                jwtService.generateServiceToken();

        assertEquals(
                "ADMIN",
                jwtService.extractRole(token)
        );
    }

    @Test
    void generateServiceToken_ShouldContainServiceUserId() {

        String token =
                jwtService.generateServiceToken();

        assertEquals(
                0,
                jwtService.extractUserId(token)
        );
    }

    @Test
    void generateServiceToken_ShouldHaveThreeParts() {

        String token =
                jwtService.generateServiceToken();

        String[] parts =
                token.split("\\.");

        assertEquals(
                3,
                parts.length
        );
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsInvalid() {

        assertThrows(
                JwtException.class,
                () ->
                        jwtService.extractEmail(
                                "invalid.jwt.token"
                        )
        );
    }

    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsBlank() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                jwtService.extractEmail("")
                );

        assertEquals(
                "JWT token is required",
                exception.getMessage()
        );
    }

    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsNull() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                jwtService.extractEmail(null)
                );

        assertEquals(
                "JWT token is required",
                exception.getMessage()
        );
    }

    // =========================================================
    // MODIFIED TOKEN
    // =========================================================

    @Test
    void extractRole_ShouldThrowException_WhenTokenIsModified() {

        String token =
                createToken(
                        "admin@gmail.com",
                        "ADMIN",
                        1,
                        86400000L
                );

        String modifiedToken =
                token.substring(
                        0,
                        token.length() - 2
                ) + "xy";

        assertThrows(
                JwtException.class,
                () ->
                        jwtService.extractRole(
                                modifiedToken
                        )
        );
    }

    // =========================================================
    // EXPIRED TOKEN
    // =========================================================

    @Test
    void extractEmail_ShouldThrowExpiredJwtException_WhenTokenIsExpired() {

        String token =
                createToken(
                        "admin@gmail.com",
                        "ADMIN",
                        1,
                        -1000L
                );

        assertThrows(
                ExpiredJwtException.class,
                () ->
                        jwtService.extractEmail(
                                token
                        )
        );
    }

    // =========================================================
    // DIFFERENT SIGNING SECRET
    // =========================================================

    @Test
    void extractEmail_ShouldThrowException_WhenSigningSecretIsDifferent() {

        String token =
                createToken(
                        "admin@gmail.com",
                        "ADMIN",
                        1,
                        86400000L
                );

        JwtService differentJwtService =
                new JwtService();

        ReflectionTestUtils.setField(
                differentJwtService,
                "secret",
                "differentSecretKeyForPolicyServiceTesting2026SecureValue"
        );

        assertThrows(
                JwtException.class,
                () ->
                        differentJwtService.extractEmail(
                                token
                        )
        );
    }

    // =========================================================
    // TOKEN CREATION HELPERS
    // =========================================================

    private String createToken(
            String email,
            String role,
            Integer userId,
            long expirationDuration
    ) {
        Date issuedAt =
                new Date();

        Date expiration =
                new Date(
                        issuedAt.getTime()
                                + expirationDuration
                );

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(
                        getSigningKey(),
                        Jwts.SIG.HS256
                )
                .compact();
    }

    private String createTokenWithStringUserId(
            String email,
            String role,
            String userId,
            long expirationDuration
    ) {
        Date issuedAt =
                new Date();

        Date expiration =
                new Date(
                        issuedAt.getTime()
                                + expirationDuration
                );

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(
                        getSigningKey(),
                        Jwts.SIG.HS256
                )
                .compact();
    }

    private String createTokenWithoutUserId(
            String email,
            String role,
            long expirationDuration
    ) {
        Date issuedAt =
                new Date();

        Date expiration =
                new Date(
                        issuedAt.getTime()
                                + expirationDuration
                );

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(
                        getSigningKey(),
                        Jwts.SIG.HS256
                )
                .compact();
    }

    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                TEST_SECRET.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
}