package org.example.applicationservice.exceptions;

public class InvalidApplicationStatusException
        extends RuntimeException {

    public InvalidApplicationStatusException(
            String message
    ) {
        super(message);
    }
}
