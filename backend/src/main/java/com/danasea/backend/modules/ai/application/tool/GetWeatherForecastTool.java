package com.danasea.backend.modules.ai.application.tool;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GetWeatherForecastTool implements ToolExecutor {

    private final GetWeatherInfoUseCase weatherInfoUseCase;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "ai:weather:forecast:";
    private static final Duration TTL = Duration.ofMinutes(10); // 5-15 min TTL
    private static final String DEFAULT_LOCATION = "Đà Nẵng";
    private static final String DEFAULT_DATE = "hôm nay";
    private static final double FALLBACK_TEMPERATURE = 28.5;
    private static final double FALLBACK_WIND_SPEED = 12.0;
    private static final double FALLBACK_WAVE_HEIGHT = 0.5;
    private static final String FALLBACK_CONDITION = "Sunny";

    @Autowired
    public GetWeatherForecastTool(@Autowired(required = false) GetWeatherInfoUseCase weatherInfoUseCase,
                                  @Autowired(required = false) StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper) {
        this.weatherInfoUseCase = weatherInfoUseCase;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public GetWeatherForecastTool() {
        this(null, null, new ObjectMapper());
    }

    @Override
    public String getName() {
        return "get_weather_forecast";
    }

    @Override
    public String execute(String argumentsJson) {
        String location = DEFAULT_LOCATION;
        String date = DEFAULT_DATE;

        try {
            if (argumentsJson != null && !argumentsJson.isBlank()) {
                JsonNode node = objectMapper.readTree(argumentsJson);
                if (node.has("location") && !node.get("location").isNull()) {
                    location = node.get("location").asText();
                }
                if (node.has("date") && !node.get("date").isNull()) {
                    date = node.get("date").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse get_weather_forecast arguments: {}", argumentsJson, e);
        }

        String cacheKey = CACHE_PREFIX + location + ":" + date;
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null && !cached.isBlank()) {
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Failed to read weather forecast from Redis", e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("location", location);
        result.put("date", date);

        try {
            WeatherInfoDto weatherInfo = weatherInfoUseCase != null ? weatherInfoUseCase.execute() : null;
            if (weatherInfo != null && weatherInfo.getWeather() != null) {
                result.put("temperature", weatherInfo.getWeather().getTemperature());
                result.put("windSpeed", weatherInfo.getWeather().getWindSpeed());
                result.put("condition", FALLBACK_CONDITION);
                if (weatherInfo.getMarine() != null) {
                    result.put("waveHeight", weatherInfo.getMarine().getWaveHeight());
                }
            } else {
                result.put("temperature", FALLBACK_TEMPERATURE);
                result.put("condition", FALLBACK_CONDITION);
                result.put("waveHeight", FALLBACK_WAVE_HEIGHT);
                result.put("windSpeed", FALLBACK_WIND_SPEED);
            }
        } catch (Exception e) {
            log.warn("Weather provider query failed, using fallback summary", e);
            result.put("temperature", FALLBACK_TEMPERATURE);
            result.put("condition", "Fair");
            result.put("waveHeight", FALLBACK_WAVE_HEIGHT);
        }

        String jsonResult;
        try {
            jsonResult = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            jsonResult = String.format("{\"location\":\"%s\",\"date\":\"%s\",\"condition\":\"Sunny\",\"waveHeight\":0.5}", location, date);
        }

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, jsonResult, TTL);
            } catch (Exception e) {
                log.warn("Failed to cache weather forecast in Redis", e);
            }
        }

        return jsonResult;
    }
}
