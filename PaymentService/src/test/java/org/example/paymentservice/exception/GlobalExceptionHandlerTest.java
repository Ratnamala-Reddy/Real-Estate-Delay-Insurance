package org.example.paymentservice.exception;

import org.example.paymentservice.dto.ErrorResponse;
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
    // PAYMENT NOT FOUND
    // =========================================================

    @Test
    void handlePaymentNotFound_ShouldReturnNotFound() {

        PaymentNotFoundException exception =
                new PaymentNotFoundException(
                        "Premium payment not found with ID: payment-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePaymentNotFound(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                "Premium payment not found with ID: payment-101"
        );
    }

    // =========================================================
    // INVALID PAYMENT REQUEST
    // =========================================================

    @Test
    void handleInvalidPaymentRequest_ShouldReturnBadRequest() {

        InvalidPaymentRequestException exception =
                new InvalidPaymentRequestException(
                        "Quote ID is required"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidPaymentRequest(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "INVALID_PAYMENT_REQUEST",
                "Quote ID is required"
        );
    }

    // =========================================================
    // DUPLICATE PREMIUM PAYMENT
    // =========================================================

    @Test
    void handlePremiumPaymentAlreadyExists_ShouldReturnConflict() {

        PremiumPaymentAlreadyExistsException exception =
                new PremiumPaymentAlreadyExistsException(
                        "Premium payment already exists "
                                + "for quote: quote-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePremiumPaymentAlreadyExists(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "PREMIUM_PAYMENT_ALREADY_EXISTS",
                "Premium payment already exists for quote: quote-101"
        );
    }

    // =========================================================
    // PAYMENT ALREADY FINALIZED
    // =========================================================

    @Test
    void handlePaymentAlreadyFinalized_ShouldReturnConflict_ForSuccess() {

        PaymentAlreadyFinalizedException exception =
                new PaymentAlreadyFinalizedException(
                        "Premium payment is already finalized as SUCCESS"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePaymentAlreadyFinalized(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "PAYMENT_ALREADY_FINALIZED",
                "Premium payment is already finalized as SUCCESS"
        );
    }

    @Test
    void handlePaymentAlreadyFinalized_ShouldReturnConflict_ForFailed() {

        PaymentAlreadyFinalizedException exception =
                new PaymentAlreadyFinalizedException(
                        "Premium payment is already finalized as FAILED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePaymentAlreadyFinalized(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "PAYMENT_ALREADY_FINALIZED",
                "Premium payment is already finalized as FAILED"
        );
    }

    // =========================================================
    // INVALID PAYMENT STATUS
    // =========================================================

    @Test
    void handleInvalidPaymentStatus_ShouldReturnBadRequest() {

        InvalidPaymentStatusException exception =
                new InvalidPaymentStatusException(
                        "Premium payment webhook status "
                                + "must be SUCCESS or FAILED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidPaymentStatus(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "INVALID_PAYMENT_STATUS",
                "Premium payment webhook status "
                        + "must be SUCCESS or FAILED"
        );
    }

    // =========================================================
    // QUOTE NOT ELIGIBLE
    // =========================================================

    @Test
    void handleQuoteNotEligibleForPayment_ShouldReturnConflict() {

        QuoteNotEligibleForPaymentException exception =
                new QuoteNotEligibleForPaymentException(
                        "Quote must be ACCEPTED "
                                + "before premium payment. "
                                + "Current status: GENERATED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleQuoteNotEligibleForPayment(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "QUOTE_NOT_ELIGIBLE_FOR_PAYMENT",
                "Quote must be ACCEPTED "
                        + "before premium payment. "
                        + "Current status: GENERATED"
        );
    }

    // =========================================================
    // APPLICATION SERVICE FAILURE
    // =========================================================

    @Test
    void handleApplicationServiceError_ShouldReturnBadGateway() {

        ApplicationServiceException exception =
                new ApplicationServiceException(
                        "Unable to fetch application "
                                + "from Application Service"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleApplicationServiceError(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_GATEWAY,
                "APPLICATION_SERVICE_ERROR",
                "Unable to fetch application "
                        + "from Application Service"
        );
    }

    // =========================================================
    // QUOTE SERVICE FAILURE
    // =========================================================

    @Test
    void handleQuoteServiceError_ShouldReturnBadGateway() {

        QuoteServiceException exception =
                new QuoteServiceException(
                        "Unable to fetch quote "
                                + "from Policy Service"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleQuoteServiceError(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_GATEWAY,
                "QUOTE_SERVICE_ERROR",
                "Unable to fetch quote from Policy Service"
        );
    }

    // =========================================================
    // POLICY SERVICE FAILURE
    // =========================================================

    @Test
    void handlePolicyServiceError_ShouldReturnBadGateway() {

        PolicyServiceException exception =
                new PolicyServiceException(
                        "Unable to create policy "
                                + "after successful payment"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePolicyServiceError(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_GATEWAY,
                "POLICY_SERVICE_ERROR",
                "Unable to create policy after successful payment"
        );
    }

    // =========================================================
    // ACCESS DENIED
    // =========================================================

    @Test
    void handleAccessDenied_ShouldReturnForbidden() {

        AccessDeniedException exception =
                new AccessDeniedException(
                        "You do not own this payment"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleAccessDenied(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "You do not own this payment"
        );
    }

    // =========================================================
    // ILLEGAL ARGUMENT
    // =========================================================

    @Test
    void handleIllegalArgument_ShouldReturnBadRequest() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Invalid payment value"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleIllegalArgument(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                "Invalid payment value"
        );
    }

    // =========================================================
    // GENERAL ERROR
    // =========================================================

    @Test
    void handleGeneralThrowable_ShouldReturnInternalServerError() {

        RuntimeException exception =
                new RuntimeException(
                        "Unexpected payment database failure"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleGeneralThrowable(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Unexpected payment database failure"
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

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "RuntimeException"
        );
    }

    // =========================================================
    // CONSTRUCTOR WITH CAUSE
    // =========================================================

    @Test
    void applicationServiceException_ShouldPreserveCause() {

        RuntimeException cause =
                new RuntimeException(
                        "Connection refused"
                );

        ApplicationServiceException exception =
                new ApplicationServiceException(
                        "Unable to fetch application",
                        cause
                );

        assertEquals(
                "Unable to fetch application",
                exception.getMessage()
        );

        assertEquals(
                cause,
                exception.getCause()
        );
    }

    @Test
    void quoteServiceException_ShouldPreserveCause() {

        RuntimeException cause =
                new RuntimeException(
                        "Policy Service unavailable"
                );

        QuoteServiceException exception =
                new QuoteServiceException(
                        "Unable to fetch quote",
                        cause
                );

        assertEquals(
                "Unable to fetch quote",
                exception.getMessage()
        );

        assertEquals(
                cause,
                exception.getCause()
        );
    }

    @Test
    void policyServiceException_ShouldPreserveCause() {

        RuntimeException cause =
                new RuntimeException(
                        "Policy creation failed"
                );

        PolicyServiceException exception =
                new PolicyServiceException(
                        "Unable to create policy",
                        cause
                );

        assertEquals(
                "Unable to create policy",
                exception.getMessage()
        );

        assertEquals(
                cause,
                exception.getCause()
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