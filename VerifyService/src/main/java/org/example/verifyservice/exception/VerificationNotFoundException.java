package org.example.verifyservice.exception;

public class VerificationNotFoundException extends RuntimeException {
    public VerificationNotFoundException(String message) {
        super(message);
    }
}