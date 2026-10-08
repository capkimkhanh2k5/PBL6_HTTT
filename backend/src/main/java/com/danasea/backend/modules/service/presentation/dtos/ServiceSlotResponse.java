package com.danasea.backend.modules.service.presentation.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.InventoryType;
import com.danasea.backend.modules.service.domain.models.SlotStatus;
import lombok.Builder;

@Builder
public record ServiceSlotResponse(
        UUID id,
        UUID serviceId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        SlotStatus status,
        InventoryType inventoryType,
        Integer capacity,
        Integer bookedCount,
        Integer availableCapacity,
        List<ServiceSlotUnitResponse> units
) {}
