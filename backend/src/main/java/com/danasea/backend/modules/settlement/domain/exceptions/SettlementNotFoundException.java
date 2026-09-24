package com.danasea.backend.modules.settlement.domain.exceptions;

public class SettlementNotFoundException extends RuntimeException {
    public SettlementNotFoundException(String message) {
        super(message);
    }
}
