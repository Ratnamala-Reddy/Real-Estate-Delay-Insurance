package org.example.authservice.exception;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(String msg){
        super(msg);
    }
}

