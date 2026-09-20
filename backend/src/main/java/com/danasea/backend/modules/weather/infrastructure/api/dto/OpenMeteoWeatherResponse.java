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

        @JsonProperty("uv_index")
        private Double uvIndex;

        @JsonProperty("relative_humidity_2m")
        private Double relativeHumidity2m;

        @JsonProperty("dew_point_2m")
        private Double dewPoint2m;

        @JsonProperty("surface_pressure")
        private Double surfacePressure;
    }

    @Data
    public static class HourlyData {
        private List<String> time;

        @JsonProperty("temperature_2m")
        private List<Double> temperature2m;

        @JsonProperty("precipitation")
        private List<Double> precipitation;

        @JsonProperty("precipitation_probability")
        private List<Integer> precipitationProbability;

        @JsonProperty("wind_speed_10m")
        private List<Double> windSpeed10m;

        @JsonProperty("wind_gusts_10m")
        private List<Double> windGusts10m;

        @JsonProperty("wind_direction_10m")
        private List<Double> windDirection10m;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;

        @JsonProperty("visibility")
        private List<Double> visibility;

        @JsonProperty("uv_index")
        private List<Double> uvIndex;

        @JsonProperty("relative_humidity_2m")
        private List<Integer> relativeHumidity2m;

        @JsonProperty("dew_point_2m")
        private List<Double> dewPoint2m;

        @JsonProperty("surface_pressure")
        private List<Double> surfacePressure;

        @JsonProperty("cloud_cover")
        private List<Integer> cloudCover;
    }
}
