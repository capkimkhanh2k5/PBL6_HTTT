package com.danasea.backend.modules.service.domain.exceptions;

public class UnauthorizedServiceAccessException extends RuntimeException {

    public UnauthorizedServiceAccessException() {
        super("You do not have permission to access or modify this service.");
    }

    public UnauthorizedServiceAccessException(String message) {
        super(message);
    }
}
