package com.danasea.backend.modules.booking.application.dtos;

import java.util.UUID;

public record BookingHoldItemDto(
        UUID slotId,
        Integer quantity
) {}
