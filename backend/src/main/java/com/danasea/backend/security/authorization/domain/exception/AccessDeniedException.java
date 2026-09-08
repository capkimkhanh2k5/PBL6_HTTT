package com.danasea.backend.security.authorization.domain.exception;

public class AccessDeniedException extends RuntimeException {
    
    public AccessDeniedException() {
        super("You do not have permission to perform this action.");
    }
    
}
