package org.example.policyservice.exception;

public class QuoteExpiredException
        extends RuntimeException {

    public QuoteExpiredException(
            String message
    ) {
        super(message);
    }
}