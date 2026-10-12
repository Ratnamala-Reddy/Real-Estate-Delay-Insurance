package org.example.applicationservice.exceptions;

public class ApplicationCannotBeWithdrawnException
        extends RuntimeException {

    public ApplicationCannotBeWithdrawnException(
            String message
    ) {
        super(message);
    }
}
