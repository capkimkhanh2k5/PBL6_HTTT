package com.danasea.backend.authentication.domain.exception;

public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException() {
        super ("Invalid email or password");
    }
}
