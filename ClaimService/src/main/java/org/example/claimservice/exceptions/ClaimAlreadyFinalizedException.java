package org.example.claimservice.exceptions;

public class ClaimAlreadyFinalizedException extends RuntimeException {

    public ClaimAlreadyFinalizedException(String message) {
        super(message);
    }
}
