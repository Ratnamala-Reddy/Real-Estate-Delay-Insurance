package org.example.applicationservice.exceptions;

public class ApplicationCannotBeUpdatedException
        extends RuntimeException {

    public ApplicationCannotBeUpdatedException(
            String message
    ) {
        super(message);
    }
}