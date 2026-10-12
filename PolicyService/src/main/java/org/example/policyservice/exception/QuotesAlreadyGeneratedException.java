package org.example.policyservice.exception;

public class QuotesAlreadyGeneratedException
        extends RuntimeException {

    public QuotesAlreadyGeneratedException(
            String message
    ) {
        super(message);
    }
}
