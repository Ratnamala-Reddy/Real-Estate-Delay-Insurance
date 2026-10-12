package org.example.verifyservice.exception;

import org.example.verifyservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(VerificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(VerificationNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("VERIFICATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", "Access denied: " + ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }
    @ExceptionHandler(
            VerificationAlreadyCompletedException.class
    )
    public ResponseEntity<ErrorResponse>
    handleVerificationAlreadyCompleted(
            VerificationAlreadyCompletedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ErrorResponse(
                                "VERIFICATION_ALREADY_COMPLETED",
                                ex.getMessage()
                        )
                );
    }
    @ExceptionHandler(
            ApplicationServiceException.class
    )
    public ResponseEntity<ErrorResponse>
    handleApplicationServiceException(
            ApplicationServiceException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(
                        new ErrorResponse(
                                "APPLICATION_SERVICE_ERROR",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            RiskServiceException.class
    )
    public ResponseEntity<ErrorResponse>
    handleRiskServiceException(
                    RiskServiceException ex
            ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(
                        new ErrorResponse(
                                "RISK_SERVICE_ERROR",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            InvalidVerificationRequestException.class
    )
    public ResponseEntity<ErrorResponse>
    handleInvalidVerificationRequest(
            InvalidVerificationRequestException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                "INVALID_VERIFICATION_REQUEST",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Throwable ex) {
        log.error("[VerifyService UNHANDLED ERROR] {}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
    }
}