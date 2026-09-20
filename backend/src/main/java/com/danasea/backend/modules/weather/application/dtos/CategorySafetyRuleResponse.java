package com.danasea.backend.modules.weather.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySafetyRuleResponse {
    private UUID id;
    private UUID categoryId;
    private String categorySlug;
    private String categoryName;
    private Double cautionWaveHeightM;
    private Double maxWaveHeightM;
    private Double cautionWindSpeedKmh;
    private Double maxWindSpeedKmh;
    private Double maxWindGustKmh;
    private Double maxOceanCurrentMs;
    private Double minVisibilityM;
    private String fatalThunderstormCodes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
