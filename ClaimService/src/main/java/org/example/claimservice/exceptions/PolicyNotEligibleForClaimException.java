package org.example.claimservice.exceptions;

public class PolicyNotEligibleForClaimException extends RuntimeException {

    public PolicyNotEligibleForClaimException(String message) {
        super(message);
    }
}
