package org.example.policyservice.exception;

public class RiskServiceException
        extends RuntimeException {

    public RiskServiceException(
            String message
    ) {
        super(message);
    }

    public RiskServiceException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}