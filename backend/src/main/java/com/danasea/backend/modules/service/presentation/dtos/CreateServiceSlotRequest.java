package com.danasea.backend.modules.service.presentation.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.danasea.backend.modules.service.domain.models.InventoryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

public record CreateServiceSlotRequest(
        @NotNull(message = "Slot date must not be null")
        @FutureOrPresent(message = "Slot date must not be in the past")
        LocalDate date,

        @NotNull(message = "Start time must not be null")
        LocalTime startTime,

        @NotNull(message = "End time must not be null")
        LocalTime endTime,

        @NotNull(message = "Inventory type (PERSON_LIMIT/SHARED_CAPACITY_UNITS) must not be null")
        InventoryType inventoryType,

        Integer capacity,

        @Valid
        List<CreateSlotUnitRequest> units
) {}
