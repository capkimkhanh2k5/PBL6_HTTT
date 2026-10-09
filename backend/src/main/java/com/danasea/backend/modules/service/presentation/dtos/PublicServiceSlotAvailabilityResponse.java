package com.danasea.backend.modules.service.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.InventoryType;
import com.danasea.backend.modules.service.domain.models.PricingUnit;
import com.danasea.backend.modules.service.domain.models.SlotStatus;
import lombok.Builder;

@Builder
public record PublicServiceSlotAvailabilityResponse(
        UUID slotId,
        UUID serviceId,
        UUID optionId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        PricingUnit pricingUnit,
        SlotStatus status,
        Integer availablePaxOrPackages,
        Integer maxPaxPerPackage,
        InventoryType inventoryType,
        boolean bookable
) {}
