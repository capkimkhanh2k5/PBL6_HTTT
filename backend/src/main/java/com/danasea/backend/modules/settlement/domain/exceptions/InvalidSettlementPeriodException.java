package com.danasea.backend.modules.settlement.domain.exceptions;

public class InvalidSettlementPeriodException extends RuntimeException {
    public InvalidSettlementPeriodException(String message) {
        super(message);
    }
}
