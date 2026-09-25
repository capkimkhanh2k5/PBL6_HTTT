package com.danasea.backend.modules.booking.presentation.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record BookingHoldRequest(
        @NotEmpty(message = "{validation.booking.items.not_empty}")
        List<@Valid BookingHoldItemRequest> items
) {}
