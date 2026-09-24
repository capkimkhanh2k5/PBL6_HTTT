package com.danasea.backend.modules.dispute.domain.exceptions;

public class DisputePeriodExpiredException extends RuntimeException {
    public DisputePeriodExpiredException(String message) {
        super(message);
    }
}
