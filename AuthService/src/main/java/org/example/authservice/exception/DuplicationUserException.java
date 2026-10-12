package org.example.authservice.exception;

public class DuplicationUserException extends RuntimeException{
    public DuplicationUserException(String msg){
        super(msg);
    }
}
