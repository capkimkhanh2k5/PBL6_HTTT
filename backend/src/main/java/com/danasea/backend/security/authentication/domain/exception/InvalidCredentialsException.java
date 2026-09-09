package com.danasea.backend.security.authentication.domain.exception;

public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException() {
        super ("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
