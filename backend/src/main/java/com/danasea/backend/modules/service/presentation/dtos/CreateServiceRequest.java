package com.danasea.backend.modules.service.presentation.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateServiceRequest(
        @NotNull UUID categoryId,
        @NotBlank String name,
        String nameEn,
        String description,
        String descriptionEn,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @NotNull @Positive Integer durationMinutes,
        @Positive Integer capacityPerSlot,
        String locationName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String waiverContent,
        Boolean weatherSensitive,
        BigDecimal minWindKmh,
        BigDecimal maxWaveM
) {}
