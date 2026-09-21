package com.danasea.backend.modules.weather.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "category_safety_rules", uniqueConstraints = {
    @UniqueConstraint(name = "uk_category_safety_rules_slug", columnNames = "category_slug"),
    @UniqueConstraint(name = "uk_category_safety_rules_category_id", columnNames = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySafetyRuleJpaEntity extends BaseJpaEntity {

    @Column(name = "category_id", unique = true)
    private UUID categoryId;

    @Column(name = "category_slug", nullable = false, unique = true, length = 120)
    private String categorySlug;

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;

    @Column(name = "caution_wave_height_m")
    private Double cautionWaveHeightM;

    @Column(name = "max_wave_height_m", nullable = false)
    private Double maxWaveHeightM;

    @Column(name = "caution_wind_speed_kmh")
    private Double cautionWindSpeedKmh;

    @Column(name = "max_wind_speed_kmh", nullable = false)
    private Double maxWindSpeedKmh;

    @Column(name = "max_wind_gust_kmh")
    private Double maxWindGustKmh;

    @Column(name = "max_ocean_current_ms")
    private Double maxOceanCurrentMs;

    @Column(name = "min_visibility_m")
    private Double minVisibilityM;

    @Column(name = "fatal_thunderstorm_codes", length = 255)
    @Builder.Default
    private String fatalThunderstormCodes = "95,96,99";
}
