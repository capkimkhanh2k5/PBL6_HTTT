package com.danasea.backend.modules.booking.presentation.dtos;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingHoldItemRequest(
        @NotNull(message = "slotId is required")
        UUID slotId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {}
