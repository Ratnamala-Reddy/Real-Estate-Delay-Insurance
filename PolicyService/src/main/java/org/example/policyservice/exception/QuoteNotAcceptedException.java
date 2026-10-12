package org.example.policyservice.exception;

public class QuoteNotAcceptedException
        extends RuntimeException {

    public QuoteNotAcceptedException(
            String message
    ) {
        super(message);
    }
}