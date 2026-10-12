package org.example.applicationservice.exceptions;

import io.jsonwebtoken.JwtException;
import org.example.applicationservice.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

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
    // APPLICATION ALREADY EXISTS TEST
    // =========================================================

    @Test
    void handleApplicationAlreadyExists_ShouldReturnConflict() {

        ApplicationAlreadyExistsException exception =
                new ApplicationAlreadyExistsException(
                        "Application already exists"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationAlreadyExists(exception);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "APPLICATION_ALREADY_EXISTS",
                response.getBody().getCode()
        );

        assertEquals(
                "Application already exists",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // APPLICATION NOT FOUND TEST
    // =========================================================

    @Test
    void handleApplicationNotFound_ShouldReturnNotFound() {

        ApplicationNotFoundException exception =
                new ApplicationNotFoundException(
                        "Application not found with id: application-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationNotFound(exception);

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "APPLICATION_NOT_FOUND",
                response.getBody().getCode()
        );

        assertEquals(
                "Application not found with id: application-101",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // APPLICATION UPDATE NOT ALLOWED TEST
    // =========================================================

    @Test
    void handleApplicationCannotBeUpdated_ShouldReturnBadRequest() {

        ApplicationCannotBeUpdatedException exception =
                new ApplicationCannotBeUpdatedException(
                        "Submitted applications cannot be updated"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationCannotBeUpdated(exception);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "APPLICATION_UPDATE_NOT_ALLOWED",
                response.getBody().getCode()
        );

        assertEquals(
                "Submitted applications cannot be updated",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // APPLICATION SUBMISSION NOT ALLOWED TEST
    // =========================================================

    @Test
    void handleApplicationCannotBeSubmitted_ShouldReturnConflict() {

        ApplicationCannotBeSubmittedException exception =
                new ApplicationCannotBeSubmittedException(
                        "Upload documents before submission"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationCannotBeSubmitted(exception);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "APPLICATION_SUBMISSION_NOT_ALLOWED",
                response.getBody().getCode()
        );

        assertEquals(
                "Upload documents before submission",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // APPLICATION WITHDRAWAL NOT ALLOWED TEST
    // =========================================================

    @Test
    void handleApplicationCannotBeWithdrawn_ShouldReturnConflict() {

        ApplicationCannotBeWithdrawnException exception =
                new ApplicationCannotBeWithdrawnException(
                        "Verified applications cannot be withdrawn"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationCannotBeWithdrawn(exception);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "APPLICATION_WITHDRAWAL_NOT_ALLOWED",
                response.getBody().getCode()
        );

        assertEquals(
                "Verified applications cannot be withdrawn",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // INVALID APPLICATION STATUS TEST
    // =========================================================

    @Test
    void handleInvalidApplicationStatus_ShouldReturnBadRequest() {

        InvalidApplicationStatusException exception =
                new InvalidApplicationStatusException(
                        "Invalid application status: HELLO"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidApplicationStatus(exception);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "INVALID_APPLICATION_STATUS",
                response.getBody().getCode()
        );

        assertEquals(
                "Invalid application status: HELLO",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // ACCESS DENIED TEST
    // =========================================================

    @Test
    void handleAccessDenied_ShouldReturnForbidden() {

        AccessDeniedException exception =
                new AccessDeniedException(
                        "Access Denied: You do not own this application"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleAccessDenied(exception);

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "FORBIDDEN",
                response.getBody().getCode()
        );

        assertEquals(
                "Access Denied: You do not own this application",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // JWT EXCEPTION TEST
    // =========================================================

    @Test
    void handleJwtException_ShouldReturnUnauthorized() {

        JwtException exception =
                new JwtException(
                        "JWT signature does not match"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleJwtException(exception);

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "INVALID_TOKEN",
                response.getBody().getCode()
        );

        assertEquals(
                "Invalid or expired JWT token: "
                        + "JWT signature does not match",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // ILLEGAL ARGUMENT TEST
    // =========================================================

    @Test
    void handleIllegalArgument_ShouldReturnBadRequest() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Authenticated user ID is required"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleIllegalArgument(exception);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "BAD_REQUEST",
                response.getBody().getCode()
        );

        assertEquals(
                "Authenticated user ID is required",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // GENERAL THROWABLE TEST
    // =========================================================

    @Test
    void handleGeneralThrowable_ShouldReturnInternalServerError() {

        RuntimeException exception =
                new RuntimeException(
                        "Unexpected database failure"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleGeneralThrowable(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "INTERNAL_SERVER_ERROR",
                response.getBody().getCode()
        );

        assertEquals(
                "Unexpected database failure",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // GENERAL THROWABLE WITH NULL MESSAGE TEST
    // =========================================================

    @Test
    void handleGeneralThrowable_ShouldUseClassName_WhenMessageIsNull() {

        RuntimeException exception =
                new RuntimeException();

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleGeneralThrowable(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "INTERNAL_SERVER_ERROR",
                response.getBody().getCode()
        );

        assertEquals(
                "RuntimeException",
                response.getBody().getMessage()
        );
    }
}