package org.example.policyservice.exception;

import org.example.policyservice.dto.ErrorResponse;
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
    // QUOTE NOT FOUND
    // =========================================================

    @Test
    void handleQuoteNotFound_ShouldReturnNotFound() {

        QuoteNotFoundException exception =
                new QuoteNotFoundException(
                        "Quote not found with ID: quote-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleQuoteNotFound(exception);

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                "QUOTE_NOT_FOUND",
                "Quote not found with ID: quote-101"
        );
    }

    // =========================================================
    // INVALID QUOTE REQUEST
    // =========================================================

    @Test
    void handleInvalidQuoteRequest_ShouldReturnBadRequest() {

        InvalidQuoteRequestException exception =
                new InvalidQuoteRequestException(
                        "Application ID is required"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidQuoteRequest(exception);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "INVALID_QUOTE_REQUEST",
                "Application ID is required"
        );
    }

    // =========================================================
    // QUOTES ALREADY GENERATED
    // =========================================================

    @Test
    void handleQuotesAlreadyGenerated_ShouldReturnConflict() {

        QuotesAlreadyGeneratedException exception =
                new QuotesAlreadyGeneratedException(
                        "Quotes are already generated "
                                + "for application: application-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleQuotesAlreadyGenerated(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "QUOTES_ALREADY_GENERATED",
                "Quotes are already generated "
                        + "for application: application-101"
        );
    }

    // =========================================================
    // QUOTE ALREADY FINALIZED
    // =========================================================

    @Test
    void handleQuoteAlreadyFinalized_ShouldReturnConflict_ForAccepted() {

        QuoteAlreadyFinalizedException exception =
                new QuoteAlreadyFinalizedException(
                        "Quote is already finalized as ACCEPTED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleQuoteAlreadyFinalized(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "QUOTE_ALREADY_FINALIZED",
                "Quote is already finalized as ACCEPTED"
        );
    }

    @Test
    void handleQuoteAlreadyFinalized_ShouldReturnConflict_ForDeclined() {

        QuoteAlreadyFinalizedException exception =
                new QuoteAlreadyFinalizedException(
                        "Quote is already finalized as DECLINED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleQuoteAlreadyFinalized(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "QUOTE_ALREADY_FINALIZED",
                "Quote is already finalized as DECLINED"
        );
    }

    // =========================================================
    // QUOTE EXPIRED
    // =========================================================

    @Test
    void handleQuoteExpired_ShouldReturnConflict() {

        QuoteExpiredException exception =
                new QuoteExpiredException(
                        "Quote has expired: quote-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleQuoteExpired(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "QUOTE_EXPIRED",
                "Quote has expired: quote-101"
        );
    }

    // =========================================================
    // QUOTE NOT ACCEPTED
    // =========================================================

    @Test
    void handleQuoteNotAccepted_ShouldReturnConflict() {

        QuoteNotAcceptedException exception =
                new QuoteNotAcceptedException(
                        "Quote must be ACCEPTED before policy creation. "
                                + "Current status: GENERATED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleQuoteNotAccepted(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "QUOTE_NOT_ACCEPTED",
                "Quote must be ACCEPTED before policy creation. "
                        + "Current status: GENERATED"
        );
    }

    // =========================================================
    // APPLICATION SERVICE ERROR
    // =========================================================

    @Test
    void handleApplicationServiceException_ShouldReturnBadGateway() {

        ApplicationServiceException exception =
                new ApplicationServiceException(
                        "Unable to fetch application "
                                + "from Application Service"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationServiceException(exception);

        assertErrorResponse(
                response,
                HttpStatus.BAD_GATEWAY,
                "APPLICATION_SERVICE_ERROR",
                "Unable to fetch application "
                        + "from Application Service"
        );
    }

    // =========================================================
    // APPLICATION NOT ELIGIBLE
    // =========================================================

    @Test
    void handleApplicationNotEligibleForQuote_ShouldReturnConflict() {

        ApplicationNotEligibleForQuoteException exception =
                new ApplicationNotEligibleForQuoteException(
                        "Application must be VERIFIED "
                                + "before quote generation. "
                                + "Current status: SUBMITTED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationNotEligibleForQuote(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "APPLICATION_NOT_ELIGIBLE_FOR_QUOTE",
                "Application must be VERIFIED "
                        + "before quote generation. "
                        + "Current status: SUBMITTED"
        );
    }

    // =========================================================
    // RISK SERVICE ERROR
    // =========================================================

    @Test
    void handleRiskServiceException_ShouldReturnBadGateway() {

        RiskServiceException exception =
                new RiskServiceException(
                        "Unable to fetch risk assessment "
                                + "from Risk Service"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleRiskServiceException(exception);

        assertErrorResponse(
                response,
                HttpStatus.BAD_GATEWAY,
                "RISK_SERVICE_ERROR",
                "Unable to fetch risk assessment "
                        + "from Risk Service"
        );
    }

    // =========================================================
    // RISK NOT ELIGIBLE
    // =========================================================

    @Test
    void handleRiskAssessmentNotEligible_ShouldReturnConflict() {

        RiskAssessmentNotEligibleException exception =
                new RiskAssessmentNotEligibleException(
                        "Risk assessment must be APPROVED "
                                + "before quote generation. "
                                + "Current status: PENDING"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleRiskAssessmentNotEligible(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "RISK_ASSESSMENT_NOT_ELIGIBLE",
                "Risk assessment must be APPROVED "
                        + "before quote generation. "
                        + "Current status: PENDING"
        );
    }

    // =========================================================
    // ACCESS DENIED
    // =========================================================

    @Test
    void handleAccessDenied_ShouldReturnForbidden() {

        AccessDeniedException exception =
                new AccessDeniedException(
                        "You do not own this application"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleAccessDenied(exception);

        assertErrorResponse(
                response,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "You do not own this application"
        );
    }

    // =========================================================
    // ILLEGAL ARGUMENT
    // =========================================================

    @Test
    void handleIllegalArgument_ShouldReturnBadRequest() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Invalid quote value"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleIllegalArgument(exception);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                "Invalid quote value"
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
                        .handleGeneralThrowable(exception);

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Unexpected database failure"
        );
    }

    @Test
    void handleGeneralThrowable_ShouldUseClassName_WhenMessageIsNull() {

        RuntimeException exception =
                new RuntimeException();

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleGeneralThrowable(exception);

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "RuntimeException"
        );
    }

    // =========================================================
    // ASSERTION HELPER
    // =========================================================

    private void assertErrorResponse(
            ResponseEntity<ErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage
    ) {
        assertEquals(
                expectedStatus,
                response.getStatusCode()
        );

        assertNotNull(
                response.getBody()
        );

        assertEquals(
                expectedCode,
                response.getBody().getCode()
        );

        assertEquals(
                expectedMessage,
                response.getBody().getMessage()
        );
    }
}