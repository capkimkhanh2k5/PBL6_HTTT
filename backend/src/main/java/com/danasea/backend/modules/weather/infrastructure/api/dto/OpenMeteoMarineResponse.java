package com.danasea.backend.modules.weather.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OpenMeteoMarineResponse {
    private double latitude;
    private double longitude;
    private String timezone;

    private CurrentData current;

    @Data
    public static class CurrentData {
        private String time;
        private int interval;

        @JsonProperty("wave_height")
        private Double waveHeight;

        @JsonProperty("wave_direction")
        private Double waveDirection;

        @JsonProperty("wave_period")
        private Double wavePeriod;

        @JsonProperty("swell_wave_height")
        private Double swellWaveHeight;

        @JsonProperty("swell_wave_direction")
        private Double swellWaveDirection;

        @JsonProperty("swell_wave_period")
        private Double swellWavePeriod;

        @JsonProperty("ocean_current_velocity")
        private Double oceanCurrentVelocity;

        @JsonProperty("ocean_current_direction")
        private Double oceanCurrentDirection;
    }

    private HourlyData hourly;

    @Data
    public static class HourlyData {
        private List<String> time;

        @JsonProperty("wave_height")
        private List<Double> waveHeight;

        @JsonProperty("wave_direction")
        private List<Double> waveDirection;

        @JsonProperty("wave_period")
        private List<Double> wavePeriod;

        @JsonProperty("swell_wave_height")
        private List<Double> swellWaveHeight;

        @JsonProperty("swell_wave_direction")
        private List<Double> swellWaveDirection;

        @JsonProperty("swell_wave_period")
        private List<Double> swellWavePeriod;

        @JsonProperty("ocean_current_velocity")
        private List<Double> oceanCurrentVelocity;

        @JsonProperty("ocean_current_direction")
        private List<Double> oceanCurrentDirection;

        @JsonProperty("sea_level_height_msl")
        private List<Double> seaLevelHeightMsl;

        @JsonProperty("sea_surface_temperature")
        private List<Double> seaSurfaceTemperature;
    }
}
