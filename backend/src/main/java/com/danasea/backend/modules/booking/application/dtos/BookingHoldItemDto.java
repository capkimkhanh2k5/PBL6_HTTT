package com.danasea.backend.modules.booking.application.dtos;

import java.util.UUID;

public record BookingHoldItemDto(
        UUID slotId,
        Integer quantity,
        UUID optionId,
        Integer participantsCount
) {
    public BookingHoldItemDto(UUID slotId, Integer quantity) {
        this(slotId, quantity, null, null);
    }
}
