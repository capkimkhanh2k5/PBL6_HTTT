package com.danasea.backend.modules.service.presentation.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateSlotUnitRequest(
        @NotNull(message = "Unit number must not be null")
        @Min(value = 1, message = "Unit number must be >= 1")
        Integer unitNumber,

        @NotNull(message = "Unit capacity must not be null")
        @Min(value = 1, message = "Unit capacity must be >= 1")
        Integer capacity
) {}
