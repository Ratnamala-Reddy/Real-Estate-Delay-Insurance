package org.example.paymentservice.exception;

import org.example.paymentservice.dto.ErrorResponse;
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

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(
            PaymentNotFoundException exception
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentRequest(
            InvalidPaymentRequestException exception
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_PAYMENT_REQUEST",
                exception.getMessage()
        );
    }

    @ExceptionHandler(PremiumPaymentAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse>
    handlePremiumPaymentAlreadyExists(
            PremiumPaymentAlreadyExistsException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "PREMIUM_PAYMENT_ALREADY_EXISTS",
                exception.getMessage()
        );
    }

    @ExceptionHandler(PaymentAlreadyFinalizedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAlreadyFinalized(
            PaymentAlreadyFinalizedException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "PAYMENT_ALREADY_FINALIZED",
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidPaymentStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentStatus(
            InvalidPaymentStatusException exception
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_PAYMENT_STATUS",
                exception.getMessage()
        );
    }

    @ExceptionHandler(QuoteNotEligibleForPaymentException.class)
    public ResponseEntity<ErrorResponse>
    handleQuoteNotEligibleForPayment(
            QuoteNotEligibleForPaymentException exception
    ) {
        return response(
                HttpStatus.CONFLICT,
                "QUOTE_NOT_ELIGIBLE_FOR_PAYMENT",
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

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleGeneralThrowable(
            Throwable exception
    ) {
        log.error("[PaymentService UNHANDLED ERROR] {}: {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
        String message =
                exception.getMessage() != null
                        ? exception.getMessage()
                        : exception.getClass()
                        .getSimpleName();

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
            log.error("[PaymentService ERROR] status={} code={} message={}", status.value(), code, message);
        } else if (status.is4xxClientError()) {
            log.warn("[PaymentService WARN] status={} code={} message={}", status.value(), code, message);
        } else {
            log.info("[PaymentService INFO] status={} code={} message={}", status.value(), code, message);
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