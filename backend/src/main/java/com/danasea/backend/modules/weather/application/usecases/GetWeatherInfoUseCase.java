package com.danasea.backend.modules.weather.application.usecases;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherCacheRepositoryPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetWeatherInfoUseCase {

    private final WeatherCacheRepositoryPort weatherCacheRepositoryPort;
    private final ObjectMapper objectMapper;
    private static final String LOCATION_KEY = "MAN_THAI_BEACH";

    public WeatherInfoDto execute() {
        return weatherCacheRepositoryPort.getLatestByLocation(LOCATION_KEY)
                .map(cache -> {
                    try {
                        return objectMapper.readValue(cache.getRawPayload(), WeatherInfoDto.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error parsing weather cache payload", e);
                    }
                })
                .orElse(null);
    }
}
