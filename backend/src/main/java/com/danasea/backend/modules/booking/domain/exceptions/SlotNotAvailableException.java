package com.danasea.backend.modules.booking.domain.exceptions;

import java.util.UUID;

public class SlotNotAvailableException extends BookingDomainException {

    private final UUID slotId;

    public SlotNotAvailableException(UUID slotId, String reason) {
        super(String.format("Slot %s is not available for booking: %s", slotId, reason));
        this.slotId = slotId;
    }

    public SlotNotAvailableException(String message) {
        super(message);
        this.slotId = null;
    }

    public UUID getSlotId() {
        return slotId;
    }
}
