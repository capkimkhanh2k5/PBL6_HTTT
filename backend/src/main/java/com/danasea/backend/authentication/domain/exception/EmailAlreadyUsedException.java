package com.danasea.backend.authentication.domain.exception;

public class EmailAlreadyUsedException extends RuntimeException {
    
    public EmailAlreadyUsedException() {
        super ("Email is already used");
    }
    
}
