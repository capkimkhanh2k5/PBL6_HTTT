package com.danasea.backend.modules.checkin.domain.exceptions;

public class CheckinTokenNotFoundException extends RuntimeException {
    public CheckinTokenNotFoundException(String message) {
        super(message);
    }

    public CheckinTokenNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
