package com.danasea.backend.modules.booking.presentation.dtos;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;

public record BookingHoldItemRequest(
        @NotNull(message = "slotId is required")
        UUID slotId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 100, message = "quantity must not exceed 100")
        Integer quantity
) {}
