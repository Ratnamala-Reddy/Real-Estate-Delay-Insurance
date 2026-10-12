package org.example.verifyservice.exception;

import org.example.verifyservice.dto.ErrorResponse;
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
    // VERIFICATION NOT FOUND
    // =========================================================

    @Test
    void handleNotFound_ShouldReturnNotFound() {

        VerificationNotFoundException exception =
                new VerificationNotFoundException(
                        "No verification record found for application: application-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleNotFound(
                        exception
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "VERIFICATION_NOT_FOUND",
                response.getBody().getCode()
        );

        assertEquals(
                "No verification record found for application: application-101",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // VERIFICATION ALREADY COMPLETED
    // =========================================================

    @Test
    void handleVerificationAlreadyCompleted_ShouldReturnConflict() {

        VerificationAlreadyCompletedException exception =
                new VerificationAlreadyCompletedException(
                        "Verification is already completed as VERIFIED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleVerificationAlreadyCompleted(
                                exception
                        );

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "VERIFICATION_ALREADY_COMPLETED",
                response.getBody().getCode()
        );

        assertEquals(
                "Verification is already completed as VERIFIED",
                response.getBody().getMessage()
        );
    }

    @Test
    void handleVerificationAlreadyCompleted_ShouldHandleRejectedStatus() {

        VerificationAlreadyCompletedException exception =
                new VerificationAlreadyCompletedException(
                        "Verification is already completed as REJECTED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleVerificationAlreadyCompleted(
                                exception
                        );

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "VERIFICATION_ALREADY_COMPLETED",
                response.getBody().getCode()
        );

        assertEquals(
                "Verification is already completed as REJECTED",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // APPLICATION SERVICE ERROR
    // =========================================================

    @Test
    void handleApplicationServiceException_ShouldReturnBadGateway() {

        ApplicationServiceException exception =
                new ApplicationServiceException(
                        "Unable to fetch verification queue from Application Service"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationServiceException(
                                exception
                        );

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "APPLICATION_SERVICE_ERROR",
                response.getBody().getCode()
        );

        assertEquals(
                "Unable to fetch verification queue from Application Service",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // RISK SERVICE ERROR
    // =========================================================

    @Test
    void handleRiskServiceException_ShouldReturnBadGateway() {

        RiskServiceException exception =
                new RiskServiceException(
                        "Unable to create risk assessment"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleRiskServiceException(
                                exception
                        );

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "RISK_SERVICE_ERROR",
                response.getBody().getCode()
        );

        assertEquals(
                "Unable to create risk assessment",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // INVALID VERIFICATION REQUEST
    // =========================================================

    @Test
    void handleInvalidVerificationRequest_ShouldReturnBadRequest() {

        InvalidVerificationRequestException exception =
                new InvalidVerificationRequestException(
                        "Remarks are required"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidVerificationRequest(
                                exception
                        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "INVALID_VERIFICATION_REQUEST",
                response.getBody().getCode()
        );

        assertEquals(
                "Remarks are required",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // ACCESS DENIED
    // =========================================================

    @Test
    void handleAccessDenied_ShouldReturnForbidden() {

        AccessDeniedException exception =
                new AccessDeniedException(
                        "Required role is missing"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleAccessDenied(
                        exception
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "FORBIDDEN",
                response.getBody().getCode()
        );

        assertEquals(
                "Access denied: Required role is missing",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // ILLEGAL ARGUMENT
    // =========================================================

    @Test
    void handleIllegalArgument_ShouldReturnBadRequest() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Invalid request value"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleIllegalArgument(
                        exception
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "BAD_REQUEST",
                response.getBody().getCode()
        );

        assertEquals(
                "Invalid request value",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // GENERAL ERROR
    // =========================================================

    @Test
    void handleGeneral_ShouldReturnInternalServerError() {

        RuntimeException exception =
                new RuntimeException(
                        "Unexpected database error"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleGeneral(
                        exception
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                "INTERNAL_SERVER_ERROR",
                response.getBody().getCode()
        );

        assertEquals(
                "Unexpected database error",
                response.getBody().getMessage()
        );
    }

    @Test
    void handleGeneral_ShouldUseClassName_WhenMessageIsNull() {

        RuntimeException exception =
                new RuntimeException();

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleGeneral(
                        exception
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

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