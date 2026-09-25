package com.danasea.backend.modules.dispute.domain.exceptions;

public class DisputeTimeLimitExceededException extends DisputePeriodExpiredException {
    public DisputeTimeLimitExceededException(String message) {
        super(message);
    }
}
