package com.danasea.backend.modules.dispute.domain.exceptions;

public class InvalidSubOrderStateException extends RuntimeException {
    public InvalidSubOrderStateException(String message) {
        super(message);
    }
}
