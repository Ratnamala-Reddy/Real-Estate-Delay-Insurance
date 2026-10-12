package org.example.claimservice.exceptions;

import org.example.claimservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ClaimNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClaimNotFound(
            ClaimNotFoundException exception
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "CLAIM_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidClaimRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidClaimRequest(
            InvalidClaimRequestException exception
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_CLAIM_REQUEST",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ClaimAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleClaimAlreadyExists(
            ClaimAlreadyExistsException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "CLAIM_ALREADY_EXISTS",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ClaimAlreadyFinalizedException.class)
    public ResponseEntity<ErrorResponse> handleClaimAlreadyFinalized(
            ClaimAlreadyFinalizedException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "CLAIM_ALREADY_FINALIZED",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidClaimStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidClaimState(
            InvalidClaimStateException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "INVALID_CLAIM_STATE",
                exception.getMessage()
        );
    }

    @ExceptionHandler(PolicyNotEligibleForClaimException.class)
    public ResponseEntity<ErrorResponse>
    handlePolicyNotEligibleForClaim(
            PolicyNotEligibleForClaimException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "POLICY_NOT_ELIGIBLE_FOR_CLAIM",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ClaimNotEligibleException.class)
    public ResponseEntity<ErrorResponse> handleClaimNotEligible(
            ClaimNotEligibleException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "CLAIM_NOT_ELIGIBLE",
                exception.getMessage()
        );
    }

    @ExceptionHandler(PolicyServiceException.class)
    public ResponseEntity<ErrorResponse> handlePolicyServiceError(
            PolicyServiceException exception
    ) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "POLICY_SERVICE_ERROR",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ApplicationServiceException.class)
    public ResponseEntity<ErrorResponse> handleApplicationServiceError(
            ApplicationServiceException exception
    ) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "APPLICATION_SERVICE_ERROR",
                exception.getMessage()
        );
    }

    @ExceptionHandler(QuoteServiceException.class)
    public ResponseEntity<ErrorResponse> handleQuoteServiceError(
            QuoteServiceException exception
    ) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "QUOTE_SERVICE_ERROR",
                exception.getMessage()
        );
    }

    @ExceptionHandler(PaymentServiceException.class)
    public ResponseEntity<ErrorResponse> handlePaymentServiceError(
            PaymentServiceException exception
    ) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "PAYMENT_SERVICE_ERROR",
                exception.getMessage()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return response(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                exception.getMessage()
        );
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            WebExchangeBindException exception
    ) {
        String message =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getDefaultMessage()
                        )
                        .orElse(
                                "Request validation failed"
                        );

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse>
    handleMethodValidationException(
            HandlerMethodValidationException exception
    ) {
        String message =
                exception.getAllErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getDefaultMessage()
                        )
                        .orElse(
                                "Request validation failed"
                        );

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                exception.getMessage()
        );
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException exception
    ) {
        log.error("[ClaimService DB CONSTRAINT ERROR] {}", exception.getMessage(), exception);
        String msg = exception.getMessage() != null ? exception.getMessage() : "";
        String friendlyMessage;
        if (msg.contains("uk_claim_policy") || msg.contains("Duplicate entry")) {
            friendlyMessage = "A claim has already been registered for this policy ID. Each insurance policy can have only one claim submitted.";
        } else {
            friendlyMessage = "A database conflict occurred with existing records. Please review your active claims.";
        }
        return response(
                HttpStatus.CONFLICT,
                "CLAIM_ALREADY_EXISTS",
                friendlyMessage
        );
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleGeneralThrowable(
            Throwable exception
    ) {
        log.error("[ClaimService UNHANDLED ERROR] {}: {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
        String raw = exception.getMessage() != null ? exception.getMessage() : "";
        String friendlyMessage;

        if (raw.contains("Duplicate entry") || raw.contains("uk_claim_policy")) {
            return response(
                    HttpStatus.CONFLICT,
                    "CLAIM_ALREADY_EXISTS",
                    "A claim has already been registered for this policy ID. Each insurance policy can have only one claim submitted."
            );
        } else if (raw.contains("SQL [") || raw.contains("executeMany") || raw.contains("constraint") || raw.contains("DataIntegrityViolation")) {
            return response(
                    HttpStatus.CONFLICT,
                    "CLAIM_CONFLICT",
                    "A conflict occurred while recording your claim. A claim may already exist for this policy."
            );
        } else {
            friendlyMessage = "An unexpected error occurred while processing the claim. Please try again later.";
        }

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                friendlyMessage
        );
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String code,
            String message
    ) {
        if (status.is5xxServerError()) {
            log.error("[ClaimService ERROR] status={} code={} message={}", status.value(), code, message);
        } else if (status.is4xxClientError()) {
            log.warn("[ClaimService WARN] status={} code={} message={}", status.value(), code, message);
        } else {
            log.info("[ClaimService INFO] status={} code={} message={}", status.value(), code, message);
        }
        return ResponseEntity
                .status(status)
                .body(
                        new ErrorResponse(
                                code,
                                message
                        )
                );
    }
}