package com.danasea.backend.modules.weather.application.dtos;

import java.util.Collection;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategorySafetyRuleRequest {

    @PositiveOrZero(message = "Caution wave threshold must not be negative")
    private Double cautionWaveHeightM;

    @PositiveOrZero(message = "Danger wave threshold must not be negative")
    private Double maxWaveHeightM;

    @PositiveOrZero(message = "Caution sustained-wind threshold must not be negative")
    private Double cautionWindSpeedKmh;

    @PositiveOrZero(message = "Danger sustained-wind threshold must not be negative")
    private Double maxWindSpeedKmh;

    @PositiveOrZero(message = "Danger wind-gust threshold must not be negative")
    private Double maxWindGustKmh;

    @PositiveOrZero(message = "Danger ocean-current threshold must not be negative")
    private Double maxOceanCurrentMs;

    @PositiveOrZero(message = "Minimum visibility must not be negative")
    private Double minVisibilityM;

    private String fatalThunderstormCodes;

    @JsonSetter("fatalThunderstormCodes")
    public void setFatalThunderstormCodes(Object value) {
        if (value == null) {
            this.fatalThunderstormCodes = null;
        } else if (value instanceof Collection<?> col) {
            this.fatalThunderstormCodes = col.stream().map(Object::toString).collect(Collectors.joining(","));
        } else {
            this.fatalThunderstormCodes = value.toString();
        }
    }
}
