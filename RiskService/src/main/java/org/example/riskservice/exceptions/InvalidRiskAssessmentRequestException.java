package org.example.riskservice.exceptions;

public class InvalidRiskAssessmentRequestException
        extends RuntimeException {

    public InvalidRiskAssessmentRequestException(
            String message
    ) {
        super(message);
    }
}