package org.example.policyservice.exception;

public class RiskAssessmentNotEligibleException
        extends RuntimeException {

    public RiskAssessmentNotEligibleException(
            String message
    ) {
        super(message);
    }
}
