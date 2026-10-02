package com.danasea.backend.modules.order.domain.exceptions;

public class PaymentVerificationException extends InvalidWebhookException {

    public PaymentVerificationException(String message) {
        super(message);
    }
}
