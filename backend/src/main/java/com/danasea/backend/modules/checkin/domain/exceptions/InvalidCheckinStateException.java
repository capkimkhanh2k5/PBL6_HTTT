package com.danasea.backend.modules.checkin.domain.exceptions;

public class InvalidCheckinStateException extends RuntimeException {
    public InvalidCheckinStateException(String message) {
        super(message);
    }

    public InvalidCheckinStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
