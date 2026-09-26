package com.danasea.backend.modules.order.domain.exceptions;

import java.util.UUID;

public class UnauthorizedOrderAccessException extends RuntimeException {

    public UnauthorizedOrderAccessException(UUID orderId, UUID userId) {
        super("User " + userId + " is not authorized to access order " + orderId);
    }

    public UnauthorizedOrderAccessException(String message) {
        super(message);
    }
}
