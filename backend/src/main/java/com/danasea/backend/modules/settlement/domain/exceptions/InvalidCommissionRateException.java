package com.danasea.backend.modules.settlement.domain.exceptions;

public class InvalidCommissionRateException extends RuntimeException {
    public InvalidCommissionRateException(String message) {
        super(message);
    }
}
