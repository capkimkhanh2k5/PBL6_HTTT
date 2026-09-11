package com.danasea.backend.modules.service.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.ServiceStatus;

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
        Boolean weatherSensitive,
        BigDecimal minWindKmh,
        BigDecimal maxWaveM,
        BigDecimal avgRating,
        Integer ratingCount,
        Integer viewCount,
        List<ServiceImageResult> images,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
