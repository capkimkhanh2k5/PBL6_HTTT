package com.danasea.backend.modules.booking.presentation.dtos;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingHoldItemRequest(
        @NotNull(message = "{validation.booking.slot.required}")
        UUID slotId,

        @NotNull(message = "{validation.booking.quantity.required}")
        @Min(value = 1, message = "{validation.booking.quantity.min}")
        Integer quantity
) {}
