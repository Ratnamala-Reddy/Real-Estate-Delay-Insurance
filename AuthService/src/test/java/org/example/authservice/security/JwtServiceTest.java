package org.example.authservice.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET =
            "this-is-a-secure-test-secret-key-with-more-than-32-characters";

    @BeforeEach
    void setUp() {

        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                TEST_SECRET
        );

        ReflectionTestUtils.setField(
                jwtService,
                "expiration",
                86400000L
        );
    }

    // =========================================================
    // TOKEN GENERATION TEST
    // =========================================================

    @Test
    void generateToken_ShouldGenerateValidToken() {

        String token = jwtService.generateToken(
                "buyer@gmail.com",
                "BUYER",
                1
        );

        assertNotNull(token);
        assertTrue(token.length() > 20);
    }

    // =========================================================
    // EMAIL EXTRACTION TEST
    // =========================================================

    @Test
    void extractEmail_ShouldReturnCorrectEmail() {

        String token = jwtService.generateToken(
                "buyer@gmail.com",
                "BUYER",
                1
        );

        String extractedEmail =
                jwtService.extractEmail(token);

        assertEquals(
                "buyer@gmail.com",
                extractedEmail
        );
    }

    // =========================================================
    // ROLE EXTRACTION TESTS
    // =========================================================

    @Test
    void extractRole_ShouldReturnBuyerRole() {

        String token = jwtService.generateToken(
                "buyer@gmail.com",
                "BUYER",
                1
        );

        String extractedRole =
                jwtService.extractRole(token);

        assertEquals(
                "BUYER",
                extractedRole
        );
    }

    @Test
    void extractRole_ShouldReturnAdminRole() {

        String token = jwtService.generateToken(
                "admin@gmail.com",
                "ADMIN",
                2
        );

        String extractedRole =
                jwtService.extractRole(token);

        assertEquals(
                "ADMIN",
                extractedRole
        );
    }

    // =========================================================
    // USER ID EXTRACTION TEST
    // =========================================================

    @Test
    void extractUserId_ShouldReturnCorrectUserId() {

        String token = jwtService.generateToken(
                "buyer@gmail.com",
                "BUYER",
                25
        );

        Integer extractedUserId =
                jwtService.extractUserId(token);

        assertEquals(
                25,
                extractedUserId
        );
    }

    // =========================================================
    // COMPLETE JWT CLAIMS TEST
    // =========================================================

    @Test
    void generatedToken_ShouldContainCorrectClaims() {

        String token = jwtService.generateToken(
                "admin@gmail.com",
                "ADMIN",
                10
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
                10,
                jwtService.extractUserId(token)
        );
    }

    // =========================================================
    // INVALID TOKEN TEST
    // =========================================================

    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsInvalid() {

        String invalidToken = "invalid.jwt.token";

        assertThrows(
                JwtException.class,
                () -> jwtService.extractEmail(invalidToken)
        );
    }

    // =========================================================
    // MODIFIED TOKEN TEST
    // =========================================================

    @Test
    void extractRole_ShouldThrowException_WhenTokenIsModified() {

        String validToken = jwtService.generateToken(
                "buyer@gmail.com",
                "BUYER",
                1
        );

        String modifiedToken =
                validToken.substring(
                        0,
                        validToken.length() - 2
                ) + "ab";

        assertThrows(
                JwtException.class,
                () -> jwtService.extractRole(modifiedToken)
        );
    }

    // =========================================================
    // EMPTY TOKEN TEST
    // =========================================================

    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsEmpty() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jwtService.extractEmail("")
                );

        assertEquals(
                "CharSequence cannot be null or empty.",
                exception.getMessage()
        );
    }

    // =========================================================
    // NULL TOKEN TEST
    // =========================================================

    @Test
    void extractEmail_ShouldThrowException_WhenTokenIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.extractEmail(null)
        );
    }

    // =========================================================
    // EXPIRED TOKEN TEST
    // =========================================================

    @Test
    void extractEmail_ShouldThrowExpiredJwtException_WhenTokenIsExpired() {

        ReflectionTestUtils.setField(
                jwtService,
                "expiration",
                -1000L
        );

        String expiredToken = jwtService.generateToken(
                "buyer@gmail.com",
                "BUYER",
                1
        );

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.extractEmail(expiredToken)
        );
    }

    // =========================================================
    // DIFFERENT SIGNING SECRET TEST
    // =========================================================

    @Test
    void extractEmail_ShouldThrowException_WhenSecretIsDifferent() {

        String token = jwtService.generateToken(
                "buyer@gmail.com",
                "BUYER",
                1
        );

        JwtService anotherJwtService =
                new JwtService();

        ReflectionTestUtils.setField(
                anotherJwtService,
                "secret",
                "a-completely-different-secret-key-with-more-than-32-characters"
        );

        ReflectionTestUtils.setField(
                anotherJwtService,
                "expiration",
                86400000L
        );

        assertThrows(
                JwtException.class,
                () -> anotherJwtService.extractEmail(token)
        );
    }
}