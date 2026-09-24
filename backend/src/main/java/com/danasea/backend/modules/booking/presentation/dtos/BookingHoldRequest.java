package com.danasea.backend.modules.booking.presentation.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BookingHoldRequest(
        @NotEmpty(message = "items cannot be empty")
        @Size(max = 20, message = "items must contain at most 20 entries")
        List<@Valid BookingHoldItemRequest> items
) {}
