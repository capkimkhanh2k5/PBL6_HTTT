package com.danasea.backend.modules.settlement.domain.exceptions;

public class InvalidOrderAmountException extends RuntimeException {
    public InvalidOrderAmountException(String message) {
        super(message);
    }
}
