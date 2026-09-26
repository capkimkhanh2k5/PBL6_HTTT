package com.danasea.backend.modules.order.domain.exceptions;

public class PaymentVerificationException extends RuntimeException {

    public PaymentVerificationException(String message) {
        super(message);
    }
}
