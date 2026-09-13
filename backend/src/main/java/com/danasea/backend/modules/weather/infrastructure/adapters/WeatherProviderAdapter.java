package com.danasea.backend.modules.weather.infrastructure.adapters;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.infrastructure.api.OpenMeteoApiClient;
import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoMarineResponse;
import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoWeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WeatherProviderAdapter implements WeatherProviderPort {

    private final OpenMeteoApiClient apiClient;

    @Override
    public WeatherInfoDto.WeatherData getWeatherByCoordinates(double latitude, double longitude) {
        OpenMeteoWeatherResponse response = apiClient.fetchWeather(latitude, longitude);

        if (response == null || response.getCurrent() == null) {
            return null;
        }

        OpenMeteoWeatherResponse.CurrentData current = response.getCurrent();
        OpenMeteoWeatherResponse.HourlyData hourly = response.getHourly();

        Integer precipProb = null;
        Double uvIndex = null;

        if (hourly != null) {
            if (hourly.getPrecipitationProbability() != null && !hourly.getPrecipitationProbability().isEmpty()) {
                precipProb = hourly.getPrecipitationProbability().get(0);
            }
            if (hourly.getUvIndex() != null && !hourly.getUvIndex().isEmpty()) {
                uvIndex = hourly.getUvIndex().get(0);
            }
        }

        return WeatherInfoDto.WeatherData.builder()
                .time(current.getTime())
                .temperature(current.getTemperature2m())
                .precipitation(current.getPrecipitation())
                .windSpeed(current.getWindSpeed10m())
                .windGust(current.getWindGusts10m())
                .windDirection(current.getWindDirection10m())
                .visibility(current.getVisibility())
                .cloudCover(current.getCloudCover())
                .weatherCode(current.getWeatherCode())
                .precipitationProbability(precipProb)
                .uvIndex(uvIndex)
                .build();
    }

    @Override
    public WeatherInfoDto.MarineData getMarineByCoordinates(double latitude, double longitude) {
        OpenMeteoMarineResponse response = apiClient.fetchMarine(latitude, longitude);

        if (response == null || response.getHourly() == null) {
            return null;
        }

        OpenMeteoMarineResponse.HourlyData hourly = response.getHourly();

        if (hourly.getTime() == null || hourly.getTime().isEmpty()) {
            return null;
        }

        // Fetch hourly and just get the first hour for the current representation
        return WeatherInfoDto.MarineData.builder()
                .time(hourly.getTime().get(0))
                .waveHeight(getFirstOrNull(hourly.getWaveHeight()))
                .waveDirection(getFirstOrNull(hourly.getWaveDirection()))
                .wavePeriod(getFirstOrNull(hourly.getWavePeriod()))
                .swellHeight(getFirstOrNull(hourly.getSwellWaveHeight()))
                .swellDirection(getFirstOrNull(hourly.getSwellWaveDirection()))
                .swellPeriod(getFirstOrNull(hourly.getSwellWavePeriod()))
                .oceanCurrentVelocity(getFirstOrNull(hourly.getOceanCurrentVelocity()))
                .oceanCurrentDirection(getFirstOrNull(hourly.getOceanCurrentDirection()))
                .seaLevelHeight(getFirstOrNull(hourly.getSeaLevelHeightMsl()))
                .seaSurfaceTemperature(getFirstOrNull(hourly.getSeaSurfaceTemperature()))
                .build();
    }

    private <T> T getFirstOrNull(java.util.List<T> list) {
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
}
