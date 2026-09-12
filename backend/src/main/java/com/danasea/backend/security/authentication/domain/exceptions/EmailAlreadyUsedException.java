package com.danasea.backend.security.authentication.domain.exceptions;

public class EmailAlreadyUsedException extends RuntimeException {
    
    public EmailAlreadyUsedException() {
        super ("Email is already used");
    }
    
}
