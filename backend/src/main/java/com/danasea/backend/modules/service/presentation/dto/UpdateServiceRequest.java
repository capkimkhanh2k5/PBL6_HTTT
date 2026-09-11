package com.danasea.backend.modules.service.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateServiceRequest(
        UUID categoryId,
        String name,
        String nameEn,
        String description,
        String descriptionEn,
        BigDecimal price,
        Integer durationMinutes,
        Integer capacityPerSlot,
        String locationName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String waiverContent,
        Boolean weatherSensitive,
        BigDecimal minWindKmh,
        BigDecimal maxWaveM
) {}
