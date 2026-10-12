package org.example.policyservice.exception;

public class InvalidQuoteRequestException
        extends RuntimeException {

    public InvalidQuoteRequestException(
            String message
    ) {
        super(message);
    }
}
