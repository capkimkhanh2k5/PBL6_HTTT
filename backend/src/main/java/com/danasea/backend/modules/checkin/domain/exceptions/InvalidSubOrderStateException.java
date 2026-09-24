package com.danasea.backend.modules.checkin.domain.exceptions;

public class InvalidSubOrderStateException extends RuntimeException {
    public InvalidSubOrderStateException(String message) {
        super(message);
    }

    public InvalidSubOrderStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
