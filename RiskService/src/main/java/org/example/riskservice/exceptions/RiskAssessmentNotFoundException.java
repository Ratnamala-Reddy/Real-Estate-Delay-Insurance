package org.example.riskservice.exceptions;

public class RiskAssessmentNotFoundException extends RuntimeException {
    public RiskAssessmentNotFoundException(String message) {
        super(message);
    }
}
