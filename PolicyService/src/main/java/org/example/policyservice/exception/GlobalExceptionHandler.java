package org.example.policyservice.exception;

import org.example.policyservice.dto.ErrorResponse;
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

    // =========================================================
    // NOT FOUND
    // =========================================================

    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePolicyNotFound(
            PolicyNotFoundException exception
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "POLICY_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(QuoteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuoteNotFound(
            QuoteNotFoundException exception
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "QUOTE_NOT_FOUND",
                exception.getMessage()
        );
    }

    // =========================================================
    // QUOTE BUSINESS ERRORS
    // =========================================================

    @ExceptionHandler(InvalidQuoteRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQuoteRequest(
            InvalidQuoteRequestException exception
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_QUOTE_REQUEST",
                exception.getMessage()
        );
    }

    @ExceptionHandler(QuotesAlreadyGeneratedException.class)
    public ResponseEntity<ErrorResponse> handleQuotesAlreadyGenerated(
            QuotesAlreadyGeneratedException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "QUOTES_ALREADY_GENERATED",
                exception.getMessage()
        );
    }

    @ExceptionHandler(QuoteAlreadyFinalizedException.class)
    public ResponseEntity<ErrorResponse> handleQuoteAlreadyFinalized(
            QuoteAlreadyFinalizedException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "QUOTE_ALREADY_FINALIZED",
                exception.getMessage()
        );
    }

    @ExceptionHandler(QuoteExpiredException.class)
    public ResponseEntity<ErrorResponse> handleQuoteExpired(
            QuoteExpiredException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "QUOTE_EXPIRED",
                exception.getMessage()
        );
    }

    @ExceptionHandler(QuoteNotAcceptedException.class)
    public ResponseEntity<ErrorResponse> handleQuoteNotAccepted(
            QuoteNotAcceptedException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "QUOTE_NOT_ACCEPTED",
                exception.getMessage()
        );
    }

    // =========================================================
    // POLICY BUSINESS ERRORS
    // =========================================================

    @ExceptionHandler(InvalidPolicyRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPolicyRequest(
            InvalidPolicyRequestException exception
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_POLICY_REQUEST",
                exception.getMessage()
        );
    }

    @ExceptionHandler(PolicyAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePolicyAlreadyExists(
            PolicyAlreadyExistsException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "POLICY_ALREADY_EXISTS",
                exception.getMessage()
        );
    }

    @ExceptionHandler(PolicyAlreadyFinalizedException.class)
    public ResponseEntity<ErrorResponse> handlePolicyAlreadyFinalized(
            PolicyAlreadyFinalizedException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "POLICY_ALREADY_FINALIZED",
                exception.getMessage()
        );
    }

    // =========================================================
    // DOWNSTREAM SERVICE ERRORS
    // =========================================================

    @ExceptionHandler(ApplicationServiceException.class)
    public ResponseEntity<ErrorResponse> handleApplicationServiceException(
            ApplicationServiceException exception
    ) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "APPLICATION_SERVICE_ERROR",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ApplicationNotEligibleForQuoteException.class)
    public ResponseEntity<ErrorResponse>
    handleApplicationNotEligibleForQuote(
            ApplicationNotEligibleForQuoteException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "APPLICATION_NOT_ELIGIBLE_FOR_QUOTE",
                exception.getMessage()
        );
    }

    @ExceptionHandler(RiskServiceException.class)
    public ResponseEntity<ErrorResponse> handleRiskServiceException(
            RiskServiceException exception
    ) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "RISK_SERVICE_ERROR",
                exception.getMessage()
        );
    }

    @ExceptionHandler(RiskAssessmentNotEligibleException.class)
    public ResponseEntity<ErrorResponse>
    handleRiskAssessmentNotEligible(
            RiskAssessmentNotEligibleException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "RISK_ASSESSMENT_NOT_ELIGIBLE",
                exception.getMessage()
        );
    }

    // =========================================================
    // VALIDATION
    // =========================================================

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

    // =========================================================
    // SECURITY
    // =========================================================

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

    // =========================================================
    // GENERAL
    // =========================================================

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

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleGeneralThrowable(
            Throwable exception
    ) {
        log.error("[PolicyService UNHANDLED ERROR] {}: {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
        String message =
                exception.getMessage() != null
                        ? exception.getMessage()
                        : exception.getClass().getSimpleName();

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                message
        );
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String code,
            String message
    ) {
        if (status.is5xxServerError()) {
            log.error("[PolicyService ERROR] status={} code={} message={}", status.value(), code, message);
        } else if (status.is4xxClientError()) {
            log.warn("[PolicyService WARN] status={} code={} message={}", status.value(), code, message);
        } else {
            log.info("[PolicyService INFO] status={} code={} message={}", status.value(), code, message);
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