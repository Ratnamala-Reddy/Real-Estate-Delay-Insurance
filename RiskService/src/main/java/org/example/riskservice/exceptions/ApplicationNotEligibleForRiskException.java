package org.example.riskservice.exceptions;

public class ApplicationNotEligibleForRiskException
        extends RuntimeException {

    public ApplicationNotEligibleForRiskException(
            String message
    ) {
        super(message);
    }
}
