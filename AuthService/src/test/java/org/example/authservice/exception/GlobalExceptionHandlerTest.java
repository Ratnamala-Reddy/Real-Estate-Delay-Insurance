package org.example.authservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler =
                new GlobalExceptionHandler();
    }

    // =========================================================
    // USER ALREADY EXISTS
    // =========================================================

    @Test
    void handleUserAlreadyExists_ShouldReturnConflictResponse() {

        UserAlreadyExistsException exception =
                new UserAlreadyExistsException(
                        "Email already registered"
                );

        Map<String, Object> response =
                globalExceptionHandler
                        .handleUserAlreadyExists(exception);

        assertNotNull(response.get("timestamp"));

        assertEquals(
                409,
                response.get("status")
        );

        assertEquals(
                "Conflict",
                response.get("error")
        );

        assertEquals(
                "Email already registered",
                response.get("message")
        );
    }

    // =========================================================
    // INVALID CREDENTIALS
    // =========================================================

    @Test
    void handleInvalidCredentials_ShouldReturnUnauthorizedResponse() {

        InvalidCredentialsException exception =
                new InvalidCredentialsException(
                        "Invalid email or password"
                );

        Map<String, Object> response =
                globalExceptionHandler
                        .handleInvalidCredentials(exception);

        assertNotNull(response.get("timestamp"));

        assertEquals(
                401,
                response.get("status")
        );

        assertEquals(
                "Unauthorized",
                response.get("error")
        );

        assertEquals(
                "Invalid email or password",
                response.get("message")
        );
    }

    // =========================================================
    // USER NOT FOUND
    // =========================================================

    @Test
    void handleUserNotFound_ShouldReturnNotFoundResponse() {

        UserNotFoundException exception =
                new UserNotFoundException(
                        "User not found with userId: 99"
                );

        Map<String, Object> response =
                globalExceptionHandler
                        .handleUserNotFound(exception);

        assertNotNull(response.get("timestamp"));

        assertEquals(
                404,
                response.get("status")
        );

        assertEquals(
                "Not Found",
                response.get("error")
        );

        assertEquals(
                "User not found with userId: 99",
                response.get("message")
        );
    }

    // =========================================================
    // ACCOUNT NOT ACTIVE
    // =========================================================

    @Test
    void handleAccountNotActive_ShouldReturnForbiddenResponse() {

        AccountNotActiveException exception =
                new AccountNotActiveException(
                        "Your account is suspended"
                );

        Map<String, Object> response =
                globalExceptionHandler
                        .handleAccountNotActive(exception);

        assertNotNull(response.get("timestamp"));

        assertEquals(
                403,
                response.get("status")
        );

        assertEquals(
                "Forbidden",
                response.get("error")
        );

        assertEquals(
                "Your account is suspended",
                response.get("message")
        );
    }

    // =========================================================
    // INVALID USER STATUS
    // =========================================================

    @Test
    void handleInvalidUserStatus_ShouldReturnBadRequestResponse() {

        InvalidUserStatusException exception =
                new InvalidUserStatusException(
                        "User status is required"
                );

        Map<String, Object> response =
                globalExceptionHandler
                        .handleInvalidUserStatus(exception);

        assertNotNull(response.get("timestamp"));

        assertEquals(
                400,
                response.get("status")
        );

        assertEquals(
                "Bad Request",
                response.get("error")
        );

        assertEquals(
                "User status is required",
                response.get("message")
        );
    }

    // =========================================================
    // ILLEGAL ARGUMENT
    // =========================================================

    @Test
    void handleIllegalArgument_ShouldReturnBadRequestResponse() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Invalid request value"
                );

        Map<String, Object> response =
                globalExceptionHandler
                        .handleIllegalArgument(exception);

        assertNotNull(response.get("timestamp"));

        assertEquals(
                400,
                response.get("status")
        );

        assertEquals(
                "Bad Request",
                response.get("error")
        );

        assertEquals(
                "Invalid request value",
                response.get("message")
        );
    }

    // =========================================================
    // DATA INTEGRITY VIOLATION
    // =========================================================

    @Test
    void handleDataIntegrityViolation_ShouldReturnConflictResponse() {

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Duplicate database value"
                );

        Map<String, Object> response =
                globalExceptionHandler
                        .handleDataIntegrityViolation(exception);

        assertNotNull(response.get("timestamp"));

        assertEquals(
                409,
                response.get("status")
        );

        assertEquals(
                "Conflict",
                response.get("error")
        );

        /*
         * Use the exact message present in your
         * GlobalExceptionHandler.
         */
        assertEquals(
                "Email or mobile number already exists",
                response.get("message")
        );
    }

    // =========================================================
    // GENERAL EXCEPTION
    // =========================================================

    @Test
    void handleGeneralException_ShouldReturnInternalServerError() {

        Exception exception =
                new Exception(
                        "Database connection failed"
                );

        Map<String, Object> response =
                globalExceptionHandler
                        .handleGeneralException(exception);

        assertNotNull(response.get("timestamp"));

        assertEquals(
                500,
                response.get("status")
        );

        assertEquals(
                "Internal Server Error",
                response.get("error")
        );

        assertEquals(
                "An unexpected error occurred",
                response.get("message")
        );
    }

    // =========================================================
    // RESPONSE STRUCTURE
    // =========================================================

    @Test
    void errorResponse_ShouldContainFourRequiredFields() {

        UserNotFoundException exception =
                new UserNotFoundException(
                        "User not found"
                );

        Map<String, Object> response =
                globalExceptionHandler
                        .handleUserNotFound(exception);

        assertEquals(
                4,
                response.size()
        );

        assertNotNull(
                response.get("timestamp")
        );

        assertEquals(
                404,
                response.get("status")
        );

        assertEquals(
                "Not Found",
                response.get("error")
        );

        assertEquals(
                "User not found",
                response.get("message")
        );
    }
}