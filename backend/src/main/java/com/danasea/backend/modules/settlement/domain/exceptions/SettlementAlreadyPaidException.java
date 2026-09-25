package com.danasea.backend.modules.settlement.domain.exceptions;

public class SettlementAlreadyPaidException extends RuntimeException {
    public SettlementAlreadyPaidException(String message) {
        super(message);
    }
}
