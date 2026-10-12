package org.example.applicationservice.exceptions;

public class ApplicationAlreadyExistsException
        extends RuntimeException {

    public ApplicationAlreadyExistsException(
            String message
    ) {
        super(message);
    }
}
