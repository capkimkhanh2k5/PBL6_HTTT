package com.danasea.backend.modules.service.presentation.dtos;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateServiceRequest(
        UUID categoryId,
        @Pattern(regexp = ".*\\S.*") @Size(max = 200)
        String name,
        @Size(max = 200)
        String nameEn,
        @Size(max = 5000)
        String description,
        @Size(max = 5000)
        String descriptionEn,
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal price,
        @Positive
        Integer durationMinutes,
        @Positive
        Integer capacityPerSlot,
        @Size(max = 255)
        String locationName,
        @Size(max = 500)
        String address,
        @DecimalMin("-90.0") @DecimalMax("90.0")
        BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0")
        BigDecimal longitude,
        @Size(max = 10000)
        String waiverContent,
        String waiverContentEn,
        Boolean weatherSensitive,
        @DecimalMin("0.0")
        BigDecimal minWindKmh,
        @DecimalMin("0.0")
        BigDecimal maxWaveM
) {}
