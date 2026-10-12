package org.example.policyservice.exception;

import org.example.policyservice.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PolicyExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {

        globalExceptionHandler =
                new GlobalExceptionHandler();
    }

    // =========================================================
    // POLICY NOT FOUND
    // =========================================================

    @Test
    void handlePolicyNotFound_ShouldReturnNotFound() {

        PolicyNotFoundException exception =
                new PolicyNotFoundException(
                        "Policy not found with ID: policy-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePolicyNotFound(exception);

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                "POLICY_NOT_FOUND",
                "Policy not found with ID: policy-101"
        );
    }

    // =========================================================
    // INVALID POLICY REQUEST
    // =========================================================

    @Test
    void handleInvalidPolicyRequest_ShouldReturnBadRequest() {

        InvalidPolicyRequestException exception =
                new InvalidPolicyRequestException(
                        "Quote ID is required"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidPolicyRequest(exception);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "INVALID_POLICY_REQUEST",
                "Quote ID is required"
        );
    }

    @Test
    void handleInvalidPolicyRequest_ShouldReturnBadRequest_ForPaymentId() {

        InvalidPolicyRequestException exception =
                new InvalidPolicyRequestException(
                        "Payment ID is required"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidPolicyRequest(exception);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "INVALID_POLICY_REQUEST",
                "Payment ID is required"
        );
    }

    // =========================================================
    // POLICY ALREADY EXISTS
    // =========================================================

    @Test
    void handlePolicyAlreadyExists_ShouldReturnConflict() {

        PolicyAlreadyExistsException exception =
                new PolicyAlreadyExistsException(
                        "Policy already exists for quote: quote-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePolicyAlreadyExists(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "POLICY_ALREADY_EXISTS",
                "Policy already exists for quote: quote-101"
        );
    }

    // =========================================================
    // POLICY ALREADY FINALIZED
    // =========================================================

    @Test
    void handlePolicyAlreadyFinalized_ShouldReturnConflict_ForCompleted() {

        PolicyAlreadyFinalizedException exception =
                new PolicyAlreadyFinalizedException(
                        "Only ACTIVE policies can be completed. "
                                + "Current status: COMPLETED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePolicyAlreadyFinalized(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "POLICY_ALREADY_FINALIZED",
                "Only ACTIVE policies can be completed. "
                        + "Current status: COMPLETED"
        );
    }

    @Test
    void handlePolicyAlreadyFinalized_ShouldReturnConflict_ForClosed() {

        PolicyAlreadyFinalizedException exception =
                new PolicyAlreadyFinalizedException(
                        "Policy is already finalized as CLOSED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePolicyAlreadyFinalized(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "POLICY_ALREADY_FINALIZED",
                "Policy is already finalized as CLOSED"
        );
    }

    @Test
    void handlePolicyAlreadyFinalized_ShouldReturnConflict_ForCancelled() {

        PolicyAlreadyFinalizedException exception =
                new PolicyAlreadyFinalizedException(
                        "Policy is already finalized as CANCELLED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePolicyAlreadyFinalized(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "POLICY_ALREADY_FINALIZED",
                "Policy is already finalized as CANCELLED"
        );
    }

    // =========================================================
    // QUOTE NOT ACCEPTED
    // =========================================================

    @Test
    void handleQuoteNotAccepted_ShouldReturnConflict_ForGeneratedQuote() {

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

    @Test
    void handleQuoteNotAccepted_ShouldReturnConflict_ForDeclinedQuote() {

        QuoteNotAcceptedException exception =
                new QuoteNotAcceptedException(
                        "Quote must be ACCEPTED before policy creation. "
                                + "Current status: DECLINED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleQuoteNotAccepted(exception);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "QUOTE_NOT_ACCEPTED",
                "Quote must be ACCEPTED before policy creation. "
                        + "Current status: DECLINED"
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