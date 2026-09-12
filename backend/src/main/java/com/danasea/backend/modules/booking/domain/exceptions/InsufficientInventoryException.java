package com.danasea.backend.modules.booking.domain.exceptions;

import java.util.UUID;

public class InsufficientInventoryException extends BookingDomainException {

    private final UUID slotId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientInventoryException(UUID slotId, int requestedQuantity, int availableQuantity) {
        super(String.format("Slot %s has insufficient capacity: requested %d, available %d",
                slotId, requestedQuantity, availableQuantity));
        this.slotId = slotId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public InsufficientInventoryException(String message) {
        super(message);
        this.slotId = null;
        this.requestedQuantity = 0;
        this.availableQuantity = 0;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
