package com.danasea.backend.modules.service.presentation.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Builder;
import com.danasea.backend.modules.service.domain.models.InventoryType;
import com.danasea.backend.modules.service.domain.models.PricingUnit;

@Builder
public record StructuredSlotResponse(
        UUID slotId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        Integer availableCapacity,
        BigDecimal price,
        UUID optionId,
        PricingUnit pricingUnit,
        Integer maxPaxPerPackage,
        InventoryType inventoryType,
        boolean bookable
) {}
