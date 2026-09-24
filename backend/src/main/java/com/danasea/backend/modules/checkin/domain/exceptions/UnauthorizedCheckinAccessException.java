package com.danasea.backend.modules.checkin.domain.exceptions;

public class UnauthorizedCheckinAccessException extends RuntimeException {
    public UnauthorizedCheckinAccessException(String message) {
        super(message);
    }

    public UnauthorizedCheckinAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
