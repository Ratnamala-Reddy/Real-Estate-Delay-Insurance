package org.example.policyservice.exception;

public class QuoteAlreadyFinalizedException
        extends RuntimeException {

    public QuoteAlreadyFinalizedException(
            String message
    ) {
        super(message);
    }
}
