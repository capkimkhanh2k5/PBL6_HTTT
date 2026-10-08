package com.danasea.backend.modules.service.presentation.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.domain.models.OptionType;
import com.danasea.backend.modules.service.domain.models.PricingUnit;
import lombok.Builder;

@Builder
public record ServiceOptionResponse(
        UUID id,
        UUID serviceId,
        String name,
        OptionType optionType,
        PricingUnit pricingUnit,
        BigDecimal price,
        Integer maxPaxPerPackage,
        String benefits,
        OptionStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
