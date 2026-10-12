package org.example.policyservice.exception;

public class QuoteNotFoundException extends RuntimeException {
    public QuoteNotFoundException(String message) {
        super(message);
    }
}