package com.danasea.backend.modules.order.domain.exceptions;

public class UnpaidOrderReceiptException extends RuntimeException {

    public UnpaidOrderReceiptException(String message) {
        super(message);
    }
}
