package com.danasea.backend.modules.weather.application.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherInfoDto {
    private double latitude;
    private double longitude;
    
    private WeatherData weather;
    private MarineData marine;

    // Cache quality & fallback metadata
    private Instant fetchedAt;
    private String source;
    private Boolean stale;
    private String dataCoverage;
    private Boolean estimatedMarine;

    @JsonIgnore
    public boolean isStale() {
        return Boolean.TRUE.equals(stale);
    }

    @JsonIgnore
    public boolean isEstimatedMarine() {
        return Boolean.TRUE.equals(estimatedMarine);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherData {
        private Instant sourceFetchedAt;
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
        private Double relativeHumidity;
        private Double dewPoint;
        private Double surfacePressure;

        @JsonIgnore
        public Double getRelativeHumidity2m() {
            return relativeHumidity;
        }

        @JsonIgnore
        public Double getDewPoint2m() {
            return dewPoint;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarineData {
        private Instant sourceFetchedAt;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeWindowForecast {
        @JsonIgnore
        public boolean isFreshAt(Instant now) {
            return sourceFetchedAt != null && validUntil != null && !sourceFetchedAt.isAfter(now)
                    && validUntil.isAfter(now) && sourceFetchedAt.plusSeconds(1800).isAfter(now);
        }

        private Instant sourceFetchedAt;
        private Instant validUntil;
        private String provider;
        private String timezone;
        private Double peakWaveHeight;
        private Double peakWindSpeed;
        private Double peakWindGust;
        private Double peakOceanCurrent;
        private Double minVisibility;
        private Integer severeWeatherCode;
        private Double totalPrecipitation;
        private Double maxUvIndex;
        private Double avgHumidity;
        private Double maxCloudCover;
    }
}
