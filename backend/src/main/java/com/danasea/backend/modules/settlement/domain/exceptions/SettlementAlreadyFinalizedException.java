package com.danasea.backend.modules.settlement.domain.exceptions;

public class SettlementAlreadyFinalizedException extends RuntimeException {
    public SettlementAlreadyFinalizedException(String message) {
        super(message);
    }
}
