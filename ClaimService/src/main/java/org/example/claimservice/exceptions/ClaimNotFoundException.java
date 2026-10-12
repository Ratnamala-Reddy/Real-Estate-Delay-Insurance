package org.example.claimservice.exceptions;

public class ClaimNotFoundException extends RuntimeException{
    public ClaimNotFoundException(String msg){
        super(msg);
    }
}