package org.example.claimservice.exceptions;

public class InvalidClaimStateException extends RuntimeException {

    public InvalidClaimStateException(String message) {
        super(message);
    }
}
