package com.danasea.backend.modules.order.domain.exceptions;

public class InvalidWebhookException extends RuntimeException {
    public InvalidWebhookException(String message) {
        super(message);
    }
}
