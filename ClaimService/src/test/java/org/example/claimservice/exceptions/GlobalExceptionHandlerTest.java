package org.example.claimservice.exceptions;

import org.example.claimservice.dto.ErrorResponse;
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
    // CLAIM NOT FOUND
    // =========================================================

    @Test
    void handleClaimNotFound_ShouldReturnNotFound() {

        ClaimNotFoundException exception =
                new ClaimNotFoundException(
                        "Claim not found with ID: claim-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleClaimNotFound(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                "CLAIM_NOT_FOUND",
                "Claim not found with ID: claim-101"
        );
    }

    // =========================================================
    // INVALID CLAIM REQUEST
    // =========================================================

    @Test
    void handleInvalidClaimRequest_ShouldReturnBadRequest() {

        InvalidClaimRequestException exception =
                new InvalidClaimRequestException(
                        "Policy ID is required"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidClaimRequest(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "INVALID_CLAIM_REQUEST",
                "Policy ID is required"
        );
    }

    @Test
    void handleInvalidClaimRequest_ShouldReturnBadRequest_ForClaimedAmount() {

        InvalidClaimRequestException exception =
                new InvalidClaimRequestException(
                        "Claimed amount must be greater than zero"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidClaimRequest(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "INVALID_CLAIM_REQUEST",
                "Claimed amount must be greater than zero"
        );
    }

    // =========================================================
    // CLAIM ALREADY EXISTS
    // =========================================================

    @Test
    void handleClaimAlreadyExists_ShouldReturnConflict() {

        ClaimAlreadyExistsException exception =
                new ClaimAlreadyExistsException(
                        "Claim already exists for policy: policy-101"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleClaimAlreadyExists(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "CLAIM_ALREADY_EXISTS",
                "Claim already exists for policy: policy-101"
        );
    }

    // =========================================================
    // CLAIM ALREADY FINALIZED
    // =========================================================

    @Test
    void handleClaimAlreadyFinalized_ShouldReturnConflict_ForApproved() {

        ClaimAlreadyFinalizedException exception =
                new ClaimAlreadyFinalizedException(
                        "Claim is already finalized as APPROVED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleClaimAlreadyFinalized(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "CLAIM_ALREADY_FINALIZED",
                "Claim is already finalized as APPROVED"
        );
    }

    @Test
    void handleClaimAlreadyFinalized_ShouldReturnConflict_ForRejected() {

        ClaimAlreadyFinalizedException exception =
                new ClaimAlreadyFinalizedException(
                        "Claim is already finalized as REJECTED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleClaimAlreadyFinalized(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "CLAIM_ALREADY_FINALIZED",
                "Claim is already finalized as REJECTED"
        );
    }

    @Test
    void handleClaimAlreadyFinalized_ShouldReturnConflict_ForClosed() {

        ClaimAlreadyFinalizedException exception =
                new ClaimAlreadyFinalizedException(
                        "Claim is already finalized as CLOSED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleClaimAlreadyFinalized(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "CLAIM_ALREADY_FINALIZED",
                "Claim is already finalized as CLOSED"
        );
    }

    // =========================================================
    // INVALID CLAIM STATE
    // =========================================================

    @Test
    void handleInvalidClaimState_ShouldReturnConflict() {

        InvalidClaimStateException exception =
                new InvalidClaimStateException(
                        "Delay must be verified before "
                                + "compensation calculation. "
                                + "Current status: DOCUMENTS_SUBMITTED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleInvalidClaimState(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "INVALID_CLAIM_STATE",
                "Delay must be verified before "
                        + "compensation calculation. "
                        + "Current status: DOCUMENTS_SUBMITTED"
        );
    }

    // =========================================================
    // POLICY NOT ELIGIBLE
    // =========================================================

    @Test
    void handlePolicyNotEligibleForClaim_ShouldReturnConflict() {

        PolicyNotEligibleForClaimException exception =
                new PolicyNotEligibleForClaimException(
                        "Policy must be ACTIVE before claim creation. "
                                + "Current status: CLOSED"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePolicyNotEligibleForClaim(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "POLICY_NOT_ELIGIBLE_FOR_CLAIM",
                "Policy must be ACTIVE before claim creation. "
                        + "Current status: CLOSED"
        );
    }

    // =========================================================
    // CLAIM NOT ELIGIBLE
    // =========================================================

    @Test
    void handleClaimNotEligible_ShouldReturnConflict() {

        ClaimNotEligibleException exception =
                new ClaimNotEligibleException(
                        "Claim is not eligible because "
                                + "there is no possession delay"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleClaimNotEligible(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "CLAIM_NOT_ELIGIBLE",
                "Claim is not eligible because "
                        + "there is no possession delay"
        );
    }

    @Test
    void handleClaimNotEligible_ShouldReturnConflict_ForPromisedDate() {

        ClaimNotEligibleException exception =
                new ClaimNotEligibleException(
                        "A delay claim can be created only after "
                                + "the promised possession date"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleClaimNotEligible(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "CLAIM_NOT_ELIGIBLE",
                "A delay claim can be created only after "
                        + "the promised possession date"
        );
    }

    @Test
    void handleClaimNotEligible_ShouldReturnConflict_ForActualDate() {

        ClaimNotEligibleException exception =
                new ClaimNotEligibleException(
                        "Actual possession date cannot be before "
                                + "the promised possession date"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handleClaimNotEligible(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "CLAIM_NOT_ELIGIBLE",
                "Actual possession date cannot be before "
                        + "the promised possession date"
        );
    }

    // =========================================================
    // POLICY SERVICE ERROR
    // =========================================================

    @Test
    void handlePolicyServiceError_ShouldReturnBadGateway() {

        PolicyServiceException exception =
                new PolicyServiceException(
                        "Unable to fetch policy "
                                + "from Policy Service"
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
                "Unable to fetch policy from Policy Service"
        );
    }

    // =========================================================
    // APPLICATION SERVICE ERROR
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
                "Unable to fetch application from Application Service"
        );
    }

    // =========================================================
    // QUOTE SERVICE ERROR
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
    // PAYMENT SERVICE ERROR
    // =========================================================

    @Test
    void handlePaymentServiceError_ShouldReturnBadGateway() {

        PaymentServiceException exception =
                new PaymentServiceException(
                        "Unable to create compensation payment"
                );

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler
                        .handlePaymentServiceError(
                                exception
                        );

        assertErrorResponse(
                response,
                HttpStatus.BAD_GATEWAY,
                "PAYMENT_SERVICE_ERROR",
                "Unable to create compensation payment"
        );
    }

    // =========================================================
    // ACCESS DENIED
    // =========================================================

    @Test
    void handleAccessDenied_ShouldReturnForbidden() {

        AccessDeniedException exception =
                new AccessDeniedException(
                        "You do not own this claim"
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
                "You do not own this claim"
        );
    }

    // =========================================================
    // ILLEGAL ARGUMENT
    // =========================================================

    @Test
    void handleIllegalArgument_ShouldReturnBadRequest() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Invalid claim value"
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
                "Invalid claim value"
        );
    }

    // =========================================================
    // GENERAL ERROR
    // =========================================================

    @Test
    void handleGeneralThrowable_ShouldReturnInternalServerError() {

        RuntimeException exception =
                new RuntimeException(
                        "Unexpected claim database failure"
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
                "Unexpected claim database failure"
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
    // EXCEPTION CAUSE PRESERVATION
    // =========================================================

    @Test
    void policyServiceException_ShouldPreserveCause() {

        RuntimeException cause =
                new RuntimeException(
                        "Connection refused"
                );

        PolicyServiceException exception =
                new PolicyServiceException(
                        "Unable to fetch policy",
                        cause
                );

        assertEquals(
                "Unable to fetch policy",
                exception.getMessage()
        );

        assertEquals(
                cause,
                exception.getCause()
        );
    }

    @Test
    void applicationServiceException_ShouldPreserveCause() {

        RuntimeException cause =
                new RuntimeException(
                        "Application Service unavailable"
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
    void paymentServiceException_ShouldPreserveCause() {

        RuntimeException cause =
                new RuntimeException(
                        "Payment Service unavailable"
                );

        PaymentServiceException exception =
                new PaymentServiceException(
                        "Unable to create compensation payment",
                        cause
                );

        assertEquals(
                "Unable to create compensation payment",
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