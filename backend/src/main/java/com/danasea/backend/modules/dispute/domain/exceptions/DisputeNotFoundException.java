package com.danasea.backend.modules.dispute.domain.exceptions;

public class DisputeNotFoundException extends RuntimeException {
    public DisputeNotFoundException(String message) {
        super(message);
    }
}
