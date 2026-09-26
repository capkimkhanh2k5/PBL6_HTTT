package com.danasea.backend.modules.order.domain.exceptions;

public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String message) {
        super(message);
    }
}
