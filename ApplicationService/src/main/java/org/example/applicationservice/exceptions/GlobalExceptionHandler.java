package org.example.applicationservice.exceptions;

import io.jsonwebtoken.JwtException;
import org.example.applicationservice.dto.ErrorResponse;
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

    @ExceptionHandler(ApplicationAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleApplicationAlreadyExists(
            ApplicationAlreadyExistsException ex
    ) {
        ErrorResponse response =
                new ErrorResponse(
                        "APPLICATION_ALREADY_EXISTS",
                        ex.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(
            ApplicationCannotBeSubmittedException.class
    )
    public ResponseEntity<ErrorResponse>
    handleApplicationCannotBeSubmitted(
            ApplicationCannotBeSubmittedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ErrorResponse(
                                "APPLICATION_SUBMISSION_NOT_ALLOWED",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            ApplicationCannotBeWithdrawnException.class
    )
    public ResponseEntity<ErrorResponse>
    handleApplicationCannotBeWithdrawn(
            ApplicationCannotBeWithdrawnException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ErrorResponse(
                                "APPLICATION_WITHDRAWAL_NOT_ALLOWED",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(
            InvalidApplicationStatusException.class
    )
    public ResponseEntity<ErrorResponse>
    handleInvalidApplicationStatus(
            InvalidApplicationStatusException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                "INVALID_APPLICATION_STATUS",
                                ex.getMessage()
                        )
                );
    }


    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotFound(
            ApplicationNotFoundException ex
    ) {
        ErrorResponse response =
                new ErrorResponse(
                        "APPLICATION_NOT_FOUND",
                        ex.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(ApplicationCannotBeUpdatedException.class)
    public ResponseEntity<ErrorResponse> handleApplicationCannotBeUpdated(
            ApplicationCannotBeUpdatedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                "APPLICATION_UPDATE_NOT_ALLOWED",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        new ErrorResponse(
                                "FORBIDDEN",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        new ErrorResponse(
                                "INVALID_TOKEN",
                                "Invalid or expired JWT token: " + ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                "BAD_REQUEST",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleGeneralThrowable(
            Throwable ex
    ) {
        log.error("[ApplicationService UNHANDLED ERROR] {}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ErrorResponse(
                                "INTERNAL_SERVER_ERROR",
                                ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()
                        )
                );
    }
}