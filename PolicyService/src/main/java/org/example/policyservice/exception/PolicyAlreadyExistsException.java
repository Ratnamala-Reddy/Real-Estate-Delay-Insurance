package org.example.policyservice.exception;

public class PolicyAlreadyExistsException
        extends RuntimeException {

    public PolicyAlreadyExistsException(
            String message
    ) {
        super(message);
    }
}
