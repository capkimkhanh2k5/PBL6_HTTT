package com.danasea.backend.modules.settlement.domain.exceptions;

public class UnauthorizedSettlementAccessException extends RuntimeException {
    public UnauthorizedSettlementAccessException(String message) {
        super(message);
    }
}
