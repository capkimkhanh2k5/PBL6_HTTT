package com.danasea.backend.modules.dispute.domain.exceptions;

public class DisputeAlreadyResolvedException extends RuntimeException {
    public DisputeAlreadyResolvedException(String message) {
        super(message);
    }
}
