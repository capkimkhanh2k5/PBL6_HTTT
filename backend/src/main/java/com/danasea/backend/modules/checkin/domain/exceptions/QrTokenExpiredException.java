package com.danasea.backend.modules.checkin.domain.exceptions;

public class QrTokenExpiredException extends RuntimeException {
    public QrTokenExpiredException(String message) {
        super(message);
    }

    public QrTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
