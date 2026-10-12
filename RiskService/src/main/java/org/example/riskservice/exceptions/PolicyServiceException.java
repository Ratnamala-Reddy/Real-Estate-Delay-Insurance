package org.example.riskservice.exceptions;

public class PolicyServiceException
        extends RuntimeException {

    public PolicyServiceException(
            String message
    ) {
        super(message);
    }

    public PolicyServiceException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}