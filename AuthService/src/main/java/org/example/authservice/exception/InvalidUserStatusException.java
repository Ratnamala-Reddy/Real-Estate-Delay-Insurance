package org.example.authservice.exception;

public class InvalidUserStatusException extends RuntimeException {

    public InvalidUserStatusException(String message) {
        super(message);
    }
}