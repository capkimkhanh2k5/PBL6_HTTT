package com.danasea.backend.modules.checkin.domain.exceptions;

public class InvalidQrTokenException extends RuntimeException {
    public InvalidQrTokenException(String message) {
        super(message);
    }

    public InvalidQrTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
