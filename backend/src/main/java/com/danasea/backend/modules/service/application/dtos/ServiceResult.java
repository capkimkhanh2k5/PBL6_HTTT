package com.danasea.backend.modules.service.application.dtos;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ServiceResult(
        UUID id,
        UUID vendorId,
        UUID categoryId,
        String name,
        String nameEn,
        String slug,
        String description,
        String descriptionEn,
        BigDecimal price,
        Integer durationMinutes,
        Integer capacityPerSlot,
        String locationName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        ServiceStatus status,
        String rejectionReason,
        String waiverContent,
        String waiverContentEn,
        Boolean waiverRequired,
        Integer waiverVersion,
        Boolean weatherSensitive,
        BigDecimal minWindKmh,
        BigDecimal maxWaveM,
        BigDecimal avgRating,
        Integer ratingCount,
        Integer viewCount,
        List<ServiceImageResult> images,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public ServiceResult(
            UUID id, UUID vendorId, UUID categoryId, String name, String nameEn,
            String slug, String description, String descriptionEn, BigDecimal price,
            Integer durationMinutes, Integer capacityPerSlot, String locationName,
            String address, BigDecimal latitude, BigDecimal longitude,
            ServiceStatus status, String rejectionReason, String waiverContent,
            String waiverContentEn, Boolean weatherSensitive, BigDecimal minWindKmh,
            BigDecimal maxWaveM, BigDecimal avgRating, Integer ratingCount,
            Integer viewCount, List<ServiceImageResult> images,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, vendorId, categoryId, name, nameEn, slug, description, descriptionEn,
                price, durationMinutes, capacityPerSlot, locationName, address,
                latitude, longitude, status, rejectionReason, waiverContent,
                waiverContentEn, false, 1, weatherSensitive, minWindKmh,
                maxWaveM, avgRating, ratingCount, viewCount, images, createdAt, updatedAt);
    }
}
