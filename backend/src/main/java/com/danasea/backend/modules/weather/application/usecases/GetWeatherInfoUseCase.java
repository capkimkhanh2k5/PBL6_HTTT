package com.danasea.backend.modules.weather.application.usecases;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherCacheRepositoryPort;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.domain.models.WeatherCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GetWeatherInfoUseCase {

    private final WeatherCacheRepositoryPort weatherCacheRepositoryPort;
    private final ObjectMapper objectMapper;
    private final WeatherProviderPort weatherProviderPort;

    public static final String LOCATION_KEY = "MAN_THAI_BEACH";
    public static final double DEFAULT_LATITUDE = 16.089035780716284;
    public static final double DEFAULT_LONGITUDE = 108.24959555394304;

    private static final Duration FRESH_THRESHOLD = Duration.ofMinutes(30);
    private static final Duration MAX_STALE_THRESHOLD = Duration.ofHours(2);

    public GetWeatherInfoUseCase(
            WeatherCacheRepositoryPort weatherCacheRepositoryPort,
            ObjectMapper objectMapper,
            WeatherProviderPort weatherProviderPort) {
        this.weatherCacheRepositoryPort = weatherCacheRepositoryPort;
        this.objectMapper = objectMapper;
        this.weatherProviderPort = weatherProviderPort;
    }

    public WeatherInfoDto execute() {
        return execute(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
    }

    public WeatherInfoDto execute(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        String locationKey = String.format(Locale.ROOT, "COORD:%.4f:%.4f", latitude, longitude);
        OffsetDateTime now = OffsetDateTime.now();

        Optional<WeatherCache> cachedOpt = weatherCacheRepositoryPort.getLatestByLocation(locationKey);
        if (cachedOpt.isEmpty() && isDefaultLocation(latitude, longitude)) {
            cachedOpt = weatherCacheRepositoryPort.getLatestByLocation(LOCATION_KEY);
        }

        if (cachedOpt.isPresent()) {
            WeatherCache cached = cachedOpt.get();
            OffsetDateTime fetchedAt = cached.getFetchedAt() != null ? cached.getFetchedAt() : cached.getCreatedAt();
            if (fetchedAt != null) {
                Duration age = Duration.between(fetchedAt, now);
                // 1. Fresh cache: <= 30 minutes
                if (!age.isNegative() && age.compareTo(FRESH_THRESHOLD) <= 0) {
                    WeatherInfoDto dto = deserialize(cached);
                    enrichMetadata(dto, fetchedAt.toInstant(), false, "OPEN_METEO");
                    return dto;
                }

                // 2. Stale cache: > 30 mins to <= 2 hours -> Try to refresh first
                if (age.compareTo(MAX_STALE_THRESHOLD) <= 0) {
                    try {
                        return fetchAndSave(latitude, longitude, locationKey, now);
                    } catch (Exception ex) {
                        log.warn("Weather provider failed for ({}, {}), falling back to stale cache (age: {}m): {}",
                                latitude, longitude, age.toMinutes(), ex.getMessage());
                        WeatherInfoDto dto = deserialize(cached);
                        enrichMetadata(dto, fetchedAt.toInstant(), true, "OPEN_METEO");
                        return dto;
                    }
                }
            }
        }

        // 3. No cache or cache older than 2 hours: must fetch from provider
        try {
            return fetchAndSave(latitude, longitude, locationKey, now);
        } catch (Exception ex) {
            log.error("Weather provider failed and no valid cache <= 2h available for ({}, {}): {}",
                    latitude, longitude, ex.getMessage());
            return null;
        }
    }

    private WeatherInfoDto fetchAndSave(double latitude, double longitude, String locationKey, OffsetDateTime now) {
        WeatherInfoDto.WeatherData weather = weatherProviderPort.getWeatherByCoordinates(latitude, longitude);
        WeatherInfoDto.MarineData marine = weatherProviderPort.getMarineByCoordinates(latitude, longitude);

        WeatherInfoDto response = WeatherInfoDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .weather(weather)
                .marine(marine)
                .build();
        if (weather == null || (weather.getWindSpeed() == null && weather.getTemperature() == null && weather.getWeatherCode() == null)) {
            throw new IllegalStateException("Weather provider returned insufficient meteorological data.");
        }
        Instant sourceTime = weather.getSourceFetchedAt() == null ? now.toInstant() : weather.getSourceFetchedAt();
        if (marine != null && marine.getSourceFetchedAt() != null && marine.getSourceFetchedAt().isBefore(sourceTime)) {
            sourceTime = marine.getSourceFetchedAt();
        }
        Duration sourceAge = Duration.between(sourceTime, now.toInstant());
        if (sourceAge.isNegative() || sourceAge.compareTo(MAX_STALE_THRESHOLD) > 0) {
            throw new IllegalStateException("Weather source data has expired.");
        }
        enrichMetadata(response, sourceTime, sourceAge.compareTo(FRESH_THRESHOLD) > 0, "OPEN_METEO");

        try {
            WeatherCache cache = new WeatherCache();
            cache.setLocationKey(locationKey);
            cache.setFetchedAt(OffsetDateTime.ofInstant(sourceTime, ZoneOffset.UTC));
            cache.setExpiresAt(OffsetDateTime.ofInstant(sourceTime.plus(FRESH_THRESHOLD), ZoneOffset.UTC));
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

    private void enrichMetadata(WeatherInfoDto dto, Instant fetchedAt, boolean stale, String source) {
        dto.setFetchedAt(fetchedAt);
        dto.setStale(stale);
        dto.setSource(source);

        boolean hasWeather = dto.getWeather() != null && (dto.getWeather().getWindSpeed() != null
                || dto.getWeather().getTemperature() != null || dto.getWeather().getWeatherCode() != null);
        boolean hasMarine = dto.getMarine() != null && dto.getMarine().getWaveHeight() != null;

        if (hasWeather && hasMarine) {
            dto.setDataCoverage("FULL_MARINE_AND_METEOROLOGY");
            dto.setEstimatedMarine(false);
        } else if (hasWeather) {
            dto.setDataCoverage("METEOROLOGY_ONLY");
            dto.setEstimatedMarine(false);
        } else {
            dto.setDataCoverage("INSUFFICIENT_DATA");
            dto.setEstimatedMarine(false);
        }
    }

    private boolean isDefaultLocation(double latitude, double longitude) {
        return Math.abs(latitude - DEFAULT_LATITUDE) < 0.0001 && Math.abs(longitude - DEFAULT_LONGITUDE) < 0.0001;
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
