package com.danasea.backend.modules.order.domain.exceptions;

public class InvalidDiscountException extends RuntimeException {

    private final String errorCode;

    public InvalidDiscountException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidDiscountException(String message) {
        this("INVALID_DISCOUNT", message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
