package com.danasea.backend.modules.weather.infrastructure.persistence.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@Table(name = "safety_rule_evaluations")
public class SafetyRuleEvaluationJpaEntity extends BaseJpaEntity {

    private UUID serviceId;

    private UUID slotId;

    private Boolean isSafe;

    private String warningMessage;

    private OffsetDateTime evaluatedAt;

    @Column(length = 50)
    private String status;

    @Column(name = "alert_level", length = 20)
    private String alertLevel;

    @Column(name = "peak_wave_height_m")
    private Double peakWaveHeightM;

    @Column(name = "peak_wind_speed_kmh")
    private Double peakWindSpeedKmh;

    @Column(name = "peak_wind_gust_kmh")
    private Double peakWindGustKmh;

    @Column(name = "peak_ocean_current_ms")
    private Double peakOceanCurrentMs;

    @Column(name = "min_visibility_m")
    private Double minVisibilityM;

    @Column(name = "severe_weather_code")
    private Integer severeWeatherCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "findings_json", nullable = false)
    private String findingsJson = "[]";

}
