package org.example.riskservice.exceptions;

import org.example.riskservice.dto.ErrorResponse;
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
    // RISK ASSESSMENT ALREADY EXISTS
    // =========================================================

    @Test
    void handleRiskAssessmentAlreadyExists_ShouldReturnConflict() {

        RiskAssessmentAlreadyExistsException exception =
                new RiskAssessmentAlreadyExistsException(
                        "Risk assessment already exists"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleRiskAssessmentAlreadyExists(
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
                "RISK_ASSESSMENT_ALREADY_EXISTS",
                response.getBody().getCode()
        );

        assertEquals(
                "Risk assessment already exists",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // RISK ASSESSMENT NOT FOUND
    // =========================================================

    @Test
    void handleRiskAssessmentNotFound_ShouldReturnNotFound() {

        RiskAssessmentNotFoundException exception =
                new RiskAssessmentNotFoundException(
                        "Risk assessment not found: assessment-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleRiskAssessmentNotFound(
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
                "RISK_ASSESSMENT_NOT_FOUND",
                response.getBody().getCode()
        );

        assertEquals(
                "Risk assessment not found: assessment-101",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // FINALIZED ASSESSMENT
    // =========================================================

    @Test
    void handleRiskAssessmentAlreadyFinalized_ShouldReturnConflict_ForApproved() {

        RiskAssessmentAlreadyFinalizedException exception =
                new RiskAssessmentAlreadyFinalizedException(
                        "Risk assessment is already finalized as APPROVED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleRiskAssessmentAlreadyFinalized(
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
                "RISK_ASSESSMENT_ALREADY_FINALIZED",
                response.getBody().getCode()
        );

        assertEquals(
                "Risk assessment is already finalized as APPROVED",
                response.getBody().getMessage()
        );
    }

    @Test
    void handleRiskAssessmentAlreadyFinalized_ShouldReturnConflict_ForDeclined() {

        RiskAssessmentAlreadyFinalizedException exception =
                new RiskAssessmentAlreadyFinalizedException(
                        "Risk assessment is already finalized as DECLINED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleRiskAssessmentAlreadyFinalized(
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
                "RISK_ASSESSMENT_ALREADY_FINALIZED",
                response.getBody().getCode()
        );

        assertEquals(
                "Risk assessment is already finalized as DECLINED",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // INVALID RISK REQUEST
    // =========================================================

    @Test
    void handleInvalidRiskAssessmentRequest_ShouldReturnBadRequest() {

        InvalidRiskAssessmentRequestException exception =
                new InvalidRiskAssessmentRequestException(
                        "Builder score must be between 0 and 100"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidRiskAssessmentRequest(
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
                "INVALID_RISK_ASSESSMENT_REQUEST",
                response.getBody().getCode()
        );

        assertEquals(
                "Builder score must be between 0 and 100",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // APPLICATION NOT FOUND
    // =========================================================

    @Test
    void handleApplicationNotFound_ShouldReturnNotFound() {

        ApplicationNotFoundException exception =
                new ApplicationNotFoundException(
                        "Application not found: application-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationNotFound(
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
                "APPLICATION_NOT_FOUND",
                response.getBody().getCode()
        );

        assertEquals(
                "Application not found: application-101",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // APPLICATION NOT ELIGIBLE
    // =========================================================

    @Test
    void handleApplicationNotEligibleForRisk_ShouldReturnConflict() {

        ApplicationNotEligibleForRiskException exception =
                new ApplicationNotEligibleForRiskException(
                        "Application must be VERIFIED before risk assessment. "
                                + "Current status: SUBMITTED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationNotEligibleForRisk(
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
                "APPLICATION_NOT_ELIGIBLE_FOR_RISK",
                response.getBody().getCode()
        );

        assertEquals(
                "Application must be VERIFIED before risk assessment. "
                        + "Current status: SUBMITTED",
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
                        "Unable to validate application with Application Service"
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
                "Unable to validate application with Application Service",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // POLICY SERVICE ERROR
    // =========================================================

    @Test
    void handlePolicyServiceException_ShouldReturnBadGateway() {

        PolicyServiceException exception =
                new PolicyServiceException(
                        "Unable to generate quote"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePolicyServiceException(
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
                "POLICY_SERVICE_ERROR",
                response.getBody().getCode()
        );

        assertEquals(
                "Unable to generate quote",
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
                        "Required ADMIN role is missing"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleAccessDenied(
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
                "Access denied: Required ADMIN role is missing",
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
                        "Invalid score value"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleIllegalArgument(
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
                "Invalid score value",
                response.getBody().getMessage()
        );
    }

    // =========================================================
    // GENERAL ERROR
    // =========================================================

    @Test
    void handleGeneralThrowable_ShouldReturnInternalServerError() {

        RuntimeException exception =
                new RuntimeException(
                        "Unexpected database failure"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleGeneralThrowable(
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
                "Unexpected database failure",
                response.getBody().getMessage()
        );
    }

    @Test
    void handleGeneralThrowable_ShouldUseClassName_WhenMessageIsNull() {

        RuntimeException exception =
                new RuntimeException();

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleGeneralThrowable(
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