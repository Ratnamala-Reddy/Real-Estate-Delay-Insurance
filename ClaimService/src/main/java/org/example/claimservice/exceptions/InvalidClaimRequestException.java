package org.example.claimservice.exceptions;

public class InvalidClaimRequestException extends RuntimeException {

    public InvalidClaimRequestException(String message) {
        super(message);
    }
}
