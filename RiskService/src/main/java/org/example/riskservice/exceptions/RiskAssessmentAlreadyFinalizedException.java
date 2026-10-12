package org.example.riskservice.exceptions;

public class RiskAssessmentAlreadyFinalizedException
        extends RuntimeException {

    public RiskAssessmentAlreadyFinalizedException(
            String message
    ) {
        super(message);
    }
}