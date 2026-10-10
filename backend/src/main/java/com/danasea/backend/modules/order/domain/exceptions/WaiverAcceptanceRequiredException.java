package com.danasea.backend.modules.order.domain.exceptions;

import com.danasea.backend.modules.order.domain.models.MissingWaiverItem;

import java.util.List;
import java.util.UUID;

public class WaiverAcceptanceRequiredException extends RuntimeException {

    private final UUID orderId;
    private final List<MissingWaiverItem> missingSubOrders;

    public WaiverAcceptanceRequiredException(
            UUID orderId, List<MissingWaiverItem> missingSubOrders) {
        super("Safety waiver acceptance is required before payment.");
        this.orderId = orderId;
        this.missingSubOrders =
                missingSubOrders != null ? List.copyOf(missingSubOrders) : List.of();
    }

    public UUID getOrderId() {
        return orderId;
    }

    public List<MissingWaiverItem> getMissingSubOrders() {
        return missingSubOrders;
    }
}
