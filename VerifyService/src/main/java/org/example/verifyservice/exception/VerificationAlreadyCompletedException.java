package org.example.verifyservice.exception;

public class VerificationAlreadyCompletedException
        extends RuntimeException {

    public VerificationAlreadyCompletedException(
            String message
    ) {
        super(message);
    }
}
