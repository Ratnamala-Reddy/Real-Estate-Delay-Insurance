package org.example.paymentservice.exception;

public class QuoteServiceException
        extends RuntimeException {

    public QuoteServiceException(
            String message
    ) {
        super(message);
    }

    public QuoteServiceException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}