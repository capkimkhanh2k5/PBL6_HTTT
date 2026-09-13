package com.danasea.backend.modules.weather.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OpenMeteoWeatherResponse {
    private double latitude;
    private double longitude;
    private String timezone;
    private double elevation;

    private CurrentData current;
    private HourlyData hourly;

    @Data
    public static class CurrentData {
        private String time;
        private int interval;
        @JsonProperty("temperature_2m")
        private Double temperature2m;

        private Double precipitation;

        @JsonProperty("wind_speed_10m")
        private Double windSpeed10m;

        @JsonProperty("wind_gusts_10m")
        private Double windGusts10m;

        @JsonProperty("wind_direction_10m")
        private Double windDirection10m;

        private Double visibility;

        @JsonProperty("cloud_cover")
        private Double cloudCover;

        @JsonProperty("weather_code")
        private Integer weatherCode;
    }

    @Data
    public static class HourlyData {
        private List<String> time;

        @JsonProperty("precipitation_probability")
        private List<Integer> precipitationProbability;

        @JsonProperty("uv_index")
        private List<Double> uvIndex;
    }
}
