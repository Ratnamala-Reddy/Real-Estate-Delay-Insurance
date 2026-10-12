package org.example.verifyservice.exception;

public class InvalidVerificationRequestException
        extends RuntimeException {

    public InvalidVerificationRequestException(
            String message
    ) {
        super(message);
    }
}
