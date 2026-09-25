package com.danasea.backend.modules.order.domain.exceptions;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Order not found with id: " + orderId);
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}
