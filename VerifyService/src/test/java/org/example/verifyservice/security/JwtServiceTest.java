package org.example.verifyservice.security;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET =
            "mySecretKeyForRealEstateProjectDelayInsurancePlatform2026SecureKey";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                TEST_SECRET
        );
    }

    // =========================================================
    // USER ID EXTRACTION TESTS
    // =========================================================

    @Test
    void extractUserId_ShouldReturnCorrectUserId() {

        String token = createToken(
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
    void extractUserId_ShouldHandleStringUserId() {

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
                        () -> jwtService.extractUserId(token)
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
                        "not-a-number",
                        86400000L
                );

        assertThrows(
                NumberFormatException.class,
                () -> jwtService.extractUserId(token)
        );
    }

    // =========================================================
    // EMAIL EXTRACTION TESTS
    // =========================================================

    @Test
    void extractEmail_ShouldReturnCorrectEmail() {

        String token = createToken(
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

        String token = createToken(
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
    // ROLE EXTRACTION TESTS
    // =========================================================

    @Test
    void extractRole_ShouldReturnAdminRole() {

        String token = createToken(
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

        String token = createToken(
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
    // COMPLETE CLAIM TEST
    // =========================================================

    @Test
    void token_ShouldContainAllRequiredClaims() {

        String token = createToken(
                "admin@gmail.com",
                "ADMIN",
                1,
                86400000L
        );

        assertEquals(
                "admin@gmail.com",
                jwtService.extractEmail(token)
        );

        assertEquals(
                "ADMIN",
                jwtService.extractRole(token)
        );

        assertEquals(
                1,
                jwtService.extractUserId(token)
        );
    }

    // =========================================================
    // SERVICE TOKEN TESTS
    // =========================================================

    @Test
    void generateServiceToken_ShouldGenerateValidAdminToken() {

        String token =
                jwtService.generateServiceToken();

        assertNotNull(token);

        assertTrue(
                !token.isBlank()
        );

        assertEquals(
                "verification-service",
                jwtService.extractEmail(token)
        );

        assertEquals(
                "ADMIN",
                jwtService.extractRole(token)
        );

        assertEquals(
                0,
                jwtService.extractUserId(token)
        );
    }

    @Test
    void generateServiceToken_ShouldGenerateSignedJwtWithThreeParts() {

        String token =
                jwtService.generateServiceToken();

        String[] parts =
                token.split("\\.");

        assertEquals(
                3,
                parts.length
        );
    }

    @Test
    void generateServiceToken_ShouldGenerateDifferentTokensOverTime()
            throws InterruptedException {

        String firstToken =
                jwtService.generateServiceToken();

        Thread.sleep(1100L);

        String secondToken =
                jwtService.generateServiceToken();

        assertTrue(
                !firstToken.equals(secondToken)
        );
    }

    // =========================================================
    // INVALID TOKEN TESTS
    // =========================================================

    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsInvalid() {

        assertThrows(
                JwtException.class,
                () -> jwtService.extractEmail(
                        "invalid.jwt.token"
                )
        );
    }

    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsEmpty() {

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.extractEmail("")
        );
    }

    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.extractEmail(null)
        );
    }

    // =========================================================
    // MODIFIED TOKEN TEST
    // =========================================================

    @Test
    void extractRole_ShouldThrowException_WhenTokenIsModified() {

        String token = createToken(
                "admin@gmail.com",
                "ADMIN",
                1,
                86400000L
        );

        String modifiedToken =
                token.substring(
                        0,
                        token.length() - 2
                ) + "ab";

        assertThrows(
                JwtException.class,
                () -> jwtService.extractRole(
                        modifiedToken
                )
        );
    }

    // =========================================================
    // EXPIRED TOKEN TEST
    // =========================================================

    @Test
    void extractEmail_ShouldThrowExpiredJwtException_WhenTokenIsExpired() {

        String token = createToken(
                "admin@gmail.com",
                "ADMIN",
                1,
                -1000L
        );

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.extractEmail(token)
        );
    }

    // =========================================================
    // DIFFERENT SECRET TEST
    // =========================================================

    @Test
    void extractEmail_ShouldThrowException_WhenSecretIsDifferent() {

        String token = createToken(
                "admin@gmail.com",
                "ADMIN",
                1,
                86400000L
        );

        JwtService anotherJwtService =
                new JwtService();

        ReflectionTestUtils.setField(
                anotherJwtService,
                "secret",
                "this-is-another-different-secure-secret-key-for-testing-2026"
        );

        assertThrows(
                JwtException.class,
                () -> anotherJwtService.extractEmail(token)
        );
    }

    // =========================================================
    // TEST TOKEN CREATION METHODS
    // =========================================================

    private String createToken(
            String email,
            String role,
            Integer userId,
            long expiration
    ) {
        Date issuedAt =
                new Date();

        Date expirationTime =
                new Date(
                        issuedAt.getTime()
                                + expiration
                );

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(issuedAt)
                .expiration(expirationTime)
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
            long expiration
    ) {
        Date issuedAt =
                new Date();

        Date expirationTime =
                new Date(
                        issuedAt.getTime()
                                + expiration
                );

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(issuedAt)
                .expiration(expirationTime)
                .signWith(
                        getSigningKey(),
                        Jwts.SIG.HS256
                )
                .compact();
    }

    private String createTokenWithoutUserId(
            String email,
            String role,
            long expiration
    ) {
        Date issuedAt =
                new Date();

        Date expirationTime =
                new Date(
                        issuedAt.getTime()
                                + expiration
                );

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expirationTime)
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