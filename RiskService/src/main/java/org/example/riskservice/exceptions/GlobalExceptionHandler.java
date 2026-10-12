package org.example.riskservice.exceptions;

import org.example.riskservice.dto.ErrorResponse;
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

    @ExceptionHandler(
            RiskAssessmentAlreadyExistsException.class
    )
    public ResponseEntity<ErrorResponse>
    handleRiskAssessmentAlreadyExists(
            RiskAssessmentAlreadyExistsException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ErrorResponse(
                                "RISK_ASSESSMENT_ALREADY_EXISTS",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            RiskAssessmentNotFoundException.class
    )
    public ResponseEntity<ErrorResponse>
    handleRiskAssessmentNotFound(
            RiskAssessmentNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        new ErrorResponse(
                                "RISK_ASSESSMENT_NOT_FOUND",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            RiskAssessmentAlreadyFinalizedException.class
    )
    public ResponseEntity<ErrorResponse>
    handleRiskAssessmentAlreadyFinalized(
            RiskAssessmentAlreadyFinalizedException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ErrorResponse(
                                "RISK_ASSESSMENT_ALREADY_FINALIZED",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            InvalidRiskAssessmentRequestException.class
    )
    public ResponseEntity<ErrorResponse>
    handleInvalidRiskAssessmentRequest(
            InvalidRiskAssessmentRequestException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                "INVALID_RISK_ASSESSMENT_REQUEST",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            ApplicationNotFoundException.class
    )
    public ResponseEntity<ErrorResponse>
    handleApplicationNotFound(
            ApplicationNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        new ErrorResponse(
                                "APPLICATION_NOT_FOUND",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            ApplicationNotEligibleForRiskException.class
    )
    public ResponseEntity<ErrorResponse>
    handleApplicationNotEligibleForRisk(
            ApplicationNotEligibleForRiskException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ErrorResponse(
                                "APPLICATION_NOT_ELIGIBLE_FOR_RISK",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            ApplicationServiceException.class
    )
    public ResponseEntity<ErrorResponse>
    handleApplicationServiceException(
            ApplicationServiceException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(
                        new ErrorResponse(
                                "APPLICATION_SERVICE_ERROR",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            PolicyServiceException.class
    )
    public ResponseEntity<ErrorResponse>
    handlePolicyServiceException(
            PolicyServiceException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(
                        new ErrorResponse(
                                "POLICY_SERVICE_ERROR",
                                exception.getMessage()
                        )
                );
    }

    /*
     * Handles @Valid errors on request DTO objects.
     */
    @ExceptionHandler(
            WebExchangeBindException.class
    )
    public ResponseEntity<ErrorResponse>
    handleValidationException(
            WebExchangeBindException exception
    ) {
        String message =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(fieldError ->
                                fieldError.getDefaultMessage()
                        )
                        .orElse(
                                "Request validation failed"
                        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                "VALIDATION_ERROR",
                                message
                        )
                );
    }

    /*
     * Handles method-level validation errors.
     */
    @ExceptionHandler(
            HandlerMethodValidationException.class
    )
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

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                "VALIDATION_ERROR",
                                message
                        )
                );
    }

    @ExceptionHandler(
            AccessDeniedException.class
    )
    public ResponseEntity<ErrorResponse>
    handleAccessDenied(
            AccessDeniedException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        new ErrorResponse(
                                "FORBIDDEN",
                                "Access denied: "
                                        + exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<ErrorResponse>
    handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                "BAD_REQUEST",
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse>
    handleGeneralThrowable(
            Throwable exception
    ) {
        log.error("[RiskService UNHANDLED ERROR] {}: {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
        String message =
                exception.getMessage() != null
                        ? exception.getMessage()
                        : exception.getClass()
                        .getSimpleName();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ErrorResponse(
                                "INTERNAL_SERVER_ERROR",
                                message
                        )
                );
    }
}