package org.example.applicationservice.exceptions;

public class ApplicationCannotBeSubmittedException
        extends RuntimeException {

    public ApplicationCannotBeSubmittedException(
            String message
    ) {
        super(message);
    }
}
