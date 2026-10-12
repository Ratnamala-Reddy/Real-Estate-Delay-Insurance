package org.example.policyservice.exception;

public class PolicyAlreadyFinalizedException
        extends RuntimeException {

    public PolicyAlreadyFinalizedException(
            String message
    ) {
        super(message);
    }
}
