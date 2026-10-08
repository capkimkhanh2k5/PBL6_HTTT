package com.danasea.backend.modules.order.domain.exceptions;

import java.util.UUID;

public class RefundNotFoundException extends RuntimeException {
    public RefundNotFoundException(UUID refundId) {
        super("Refund record not found with id: " + refundId);
    }
}
