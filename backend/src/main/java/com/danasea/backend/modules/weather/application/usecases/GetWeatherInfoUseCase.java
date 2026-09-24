package com.danasea.backend.modules.weather.application.usecases;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherCacheRepositoryPort;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.domain.models.WeatherCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class GetWeatherInfoUseCase {

    private final WeatherCacheRepositoryPort weatherCacheRepositoryPort;
    private final ObjectMapper objectMapper;
    private final WeatherProviderPort weatherProviderPort;
    private static final String LOCATION_KEY = "MAN_THAI_BEACH";

    public GetWeatherInfoUseCase(
            WeatherCacheRepositoryPort weatherCacheRepositoryPort,
            ObjectMapper objectMapper,
            WeatherProviderPort weatherProviderPort) {
        this.weatherCacheRepositoryPort = weatherCacheRepositoryPort;
        this.objectMapper = objectMapper;
        this.weatherProviderPort = weatherProviderPort;
    }

    public WeatherInfoDto execute() {
        return weatherCacheRepositoryPort.getLatestByLocation(LOCATION_KEY)
                .map(this::deserialize)
                .orElse(null);
    }

    public WeatherInfoDto execute(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        String locationKey = String.format(Locale.ROOT, "COORD:%.4f:%.4f", latitude, longitude);
        OffsetDateTime now = OffsetDateTime.now();
        var cached = weatherCacheRepositoryPort.getLatestByLocation(locationKey)
                .filter(cache -> cache.getExpiresAt() != null && cache.getExpiresAt().isAfter(now));
        if (cached.isPresent()) {
            return deserialize(cached.get());
        }

        WeatherInfoDto.WeatherData weather = weatherProviderPort.getWeatherByCoordinates(latitude, longitude);
        WeatherInfoDto.MarineData marine = weatherProviderPort.getMarineByCoordinates(latitude, longitude);
        WeatherInfoDto response = WeatherInfoDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .weather(weather)
                .marine(marine)
                .build();
        try {
            WeatherCache cache = new WeatherCache();
            cache.setLocationKey(locationKey);
            cache.setFetchedAt(now);
            cache.setExpiresAt(now.plusMinutes(30));
            cache.setRawPayload(objectMapper.writeValueAsString(response));
            if (weather != null && weather.getWindSpeed() != null) {
                cache.setWindSpeedKmh(BigDecimal.valueOf(weather.getWindSpeed()));
            }
            if (weather != null && weather.getPrecipitation() != null) {
                cache.setPrecipitationMm(BigDecimal.valueOf(weather.getPrecipitation()));
            }
            if (marine != null && marine.getWaveHeight() != null) {
                cache.setWaveHeightM(BigDecimal.valueOf(marine.getWaveHeight()));
            }
            weatherCacheRepositoryPort.save(cache);
            return response;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize weather response.", ex);
        }
    }

    private WeatherInfoDto deserialize(WeatherCache cache) {
        try {
            return objectMapper.readValue(cache.getRawPayload(), WeatherInfoDto.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse cached weather data.", ex);
        }
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 and longitude between -180 and 180.");
        }
    }
}
