package org.example.authservice.exception;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(String message) {
        super(message);
    }
}