package org.example.authservice.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleUserAlreadyExists(
            UserAlreadyExistsException exception
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleInvalidCredentials(
            InvalidCredentialsException exception
    ) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }

    @ExceptionHandler(AccountNotActiveException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleAccountNotActive(
            AccountNotActiveException exception
    ) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage()
        );
    }

    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleServerWebInput(
            ServerWebInputException exception
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid or missing request value"
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleUserNotFound(
            UserNotFoundException exception
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidUserStatusException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidUserStatus(
            InvalidUserStatusException exception
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Email or mobile number already exists"
        );
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationErrors(
            WebExchangeBindException exception
    ) {
        String message = exception.getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField()
                                + ": "
                                + error.getDefaultMessage()
                )
                .orElse("Request validation failed");

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource"
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneralException(
            Exception exception
    ) {
        log.error("[AuthService UNHANDLED ERROR] {}: {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + exception.getMessage()
        );
    }

    private Map<String, Object> buildErrorResponse(
            HttpStatus status,
            String message
    ) {
        if (status.is5xxServerError()) {
            log.error("[AuthService ERROR] status={} message={}", status.value(), message);
        } else if (status.is4xxClientError()) {
            log.warn("[AuthService WARN] status={} message={}", status.value(), message);
        } else {
            log.info("[AuthService INFO] status={} message={}", status.value(), message);
        }

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);

        return response;
    }
}