package com.danasea.backend.modules.service.application.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateServiceCommand(
        UUID userId,
        UUID serviceId,
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
        String waiverContentEn,
        Boolean weatherSensitive,
        BigDecimal minWindKmh,
        BigDecimal maxWaveM
) {
    public UpdateServiceCommand(
            UUID userId, UUID serviceId, UUID categoryId, String name,
            String nameEn, String description, String descriptionEn, BigDecimal price,
            Integer durationMinutes, Integer capacityPerSlot, String locationName,
            String address, BigDecimal latitude, BigDecimal longitude,
            String waiverContent, Boolean weatherSensitive,
            BigDecimal minWindKmh, BigDecimal maxWaveM) {
        this(userId, serviceId, categoryId, name, nameEn, description, descriptionEn,
                price, durationMinutes, capacityPerSlot, locationName, address,
                latitude, longitude, waiverContent, null, weatherSensitive,
                minWindKmh, maxWaveM);
    }
}
