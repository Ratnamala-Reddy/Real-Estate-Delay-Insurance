package org.example.claimservice.exceptions;

public class ClaimAlreadyExistsException extends RuntimeException {

    public ClaimAlreadyExistsException(String message) {
        super(message);
    }
}
