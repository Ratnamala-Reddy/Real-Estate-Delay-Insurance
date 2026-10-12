package org.example.riskservice.exceptions;

public class RiskAssessmentAlreadyExistsException
        extends RuntimeException {

    public RiskAssessmentAlreadyExistsException(
            String message
    ) {
        super(message);
    }
}
