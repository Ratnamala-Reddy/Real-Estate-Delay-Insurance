package org.example.claimservice.exceptions;

public class ClaimNotEligibleException extends RuntimeException {

    public ClaimNotEligibleException(String message) {
        super(message);
    }
}
