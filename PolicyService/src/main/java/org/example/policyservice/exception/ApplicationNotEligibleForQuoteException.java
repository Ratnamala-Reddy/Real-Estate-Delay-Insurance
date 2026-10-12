package org.example.policyservice.exception;

public class ApplicationNotEligibleForQuoteException
        extends RuntimeException {

    public ApplicationNotEligibleForQuoteException(
            String message
    ) {
        super(message);
    }
}
