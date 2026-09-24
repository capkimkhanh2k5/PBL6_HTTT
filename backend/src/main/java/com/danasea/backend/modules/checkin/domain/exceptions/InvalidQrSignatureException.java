package com.danasea.backend.modules.checkin.domain.exceptions;

public class InvalidQrSignatureException extends InvalidQrTokenException {
    public InvalidQrSignatureException(String message) {
        super(message);
    }

    public InvalidQrSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
