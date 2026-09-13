package com.danasea.backend.modules.weather.application.dtos;

import lombok.Builder;
import lombok.Data;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherInfoDto {
    private double latitude;
    private double longitude;
    
    private WeatherData weather;
    private MarineData marine;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherData {
        private String time;
        private Double temperature;
        private Double precipitation;
        private Double windSpeed;
        private Double windGust;
        private Double windDirection;
        private Double visibility;
        private Double cloudCover;
        private Integer weatherCode;
        private Integer precipitationProbability;
        private Double uvIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarineData {
        private String time;
        private Double waveHeight;
        private Double waveDirection;
        private Double wavePeriod;
        private Double swellHeight;
        private Double swellDirection;
        private Double swellPeriod;
        private Double oceanCurrentVelocity;
        private Double oceanCurrentDirection;
        private Double seaLevelHeight;
        private Double seaSurfaceTemperature;
    }
}
