package com.danasea.backend.modules.ai.application.tool;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import com.danasea.backend.modules.weather.domain.models.CategorySafetyRule;
import com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class GetSafetyAlertTool implements ToolExecutor {

    private final GetWeatherInfoUseCase weatherInfoUseCase;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WeatherRuleEngine weatherRuleEngine;
    private final boolean coordinateAware;

    private static final String CACHE_PREFIX = "ai:weather:safety:";
    private static final Duration TTL = Duration.ofMinutes(10); // 5-15 min TTL
    private static final String DEFAULT_LOCATION = "Da Nang coast";
    private static final String ALERT_GREEN = "GREEN";
    private static final String ALERT_YELLOW = "YELLOW";
    private static final String ALERT_RED = "RED";

    @Autowired
    public GetSafetyAlertTool(@Autowired(required = false) GetWeatherInfoUseCase weatherInfoUseCase,
                              @Autowired(required = false) StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              @Autowired(required = false) WeatherRuleEngine weatherRuleEngine) {
        this.weatherInfoUseCase = weatherInfoUseCase;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.weatherRuleEngine = weatherRuleEngine != null ? weatherRuleEngine : new WeatherRuleEngine();
        this.coordinateAware = true;
    }

    public GetSafetyAlertTool(GetWeatherInfoUseCase weatherInfoUseCase,
                              StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper) {
        this.weatherInfoUseCase = weatherInfoUseCase;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.weatherRuleEngine = new WeatherRuleEngine();
        this.coordinateAware = false;
    }

    public GetSafetyAlertTool() {
        this(null, null, new ObjectMapper());
    }

    @Override
    public String getName() {
        return "get_safety_alert";
    }

    @Override
    public String execute(String argumentsJson) {
        String location = DEFAULT_LOCATION;
        String categorySlug = "default";
        Double latitude = null;
        Double longitude = null;

        try {
            if (argumentsJson != null && !argumentsJson.isBlank()) {
                JsonNode node = objectMapper.readTree(argumentsJson);
                if (node.has("location") && !node.get("location").isNull()) {
                    location = node.get("location").asText();
                }
                if (node.has("category") && !node.get("category").isNull()) {
                    categorySlug = node.get("category").asText();
                } else if (node.has("category_slug") && !node.get("category_slug").isNull()) {
                    categorySlug = node.get("category_slug").asText();
                } else if (node.has("activity") && !node.get("activity").isNull()) {
                    categorySlug = node.get("activity").asText();
                }
                if (node.has("latitude") && node.get("latitude").isNumber()) {
                    latitude = node.get("latitude").asDouble();
                }
                if (node.has("longitude") && node.get("longitude").isNumber()) {
                    longitude = node.get("longitude").asDouble();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse get_safety_alert arguments: {}", argumentsJson, e);
        }

        location = location == null ? "" : location.trim();
        categorySlug = categorySlug == null ? "default" : categorySlug.trim();
        if (location.isBlank() || location.length() > 120 || categorySlug.isBlank() || categorySlug.length() > 80) {
            return error("INVALID_ARGUMENTS", "location and category must be non-blank and within their length limits");
        }
        boolean explicitCoordinates = latitude != null || longitude != null;
        double[] coordinates;
        try {
            coordinates = resolveCoordinates(location, latitude, longitude);
        } catch (IllegalArgumentException exception) {
            return error("INVALID_ARGUMENTS", exception.getMessage());
        }

        String cacheKey = "default".equalsIgnoreCase(categorySlug)
                ? CACHE_PREFIX + location
                : CACHE_PREFIX + location + ":" + categorySlug;
        if (explicitCoordinates) {
            cacheKey += String.format(Locale.ROOT, ":%.4f:%.4f", coordinates[0], coordinates[1]);
        }
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null && !cached.isBlank()) {
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Failed to read safety alert from Redis", e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("location", location);
        result.put("category", categorySlug);
        result.put("latitude", coordinates[0]);
        result.put("longitude", coordinates[1]);

        boolean isSafe = false;
        String alertLevel = ALERT_YELLOW;
        String message = "Weather data is unavailable. Do not treat marine conditions as safe; check an official forecast before departure.";

        try {
            WeatherInfoDto weatherInfo = weatherInfoUseCase != null
                    ? (coordinateAware
                            ? weatherInfoUseCase.execute(coordinates[0], coordinates[1])
                            : weatherInfoUseCase.execute())
                    : null;
            if (hasSafetyData(weatherInfo)) {
                Double waveHeight = weatherInfo.getMarine() != null ? weatherInfo.getMarine().getWaveHeight() : null;
                Double oceanCurrent = weatherInfo.getMarine() != null ? weatherInfo.getMarine().getOceanCurrentVelocity() : null;
                Double windSpeed = weatherInfo.getWeather() != null ? weatherInfo.getWeather().getWindSpeed() : null;
                Double windGust = weatherInfo.getWeather() != null ? weatherInfo.getWeather().getWindGust() : null;
                Double visibility = weatherInfo.getWeather() != null ? weatherInfo.getWeather().getVisibility() : null;
                Integer weatherCode = weatherInfo.getWeather() != null ? weatherInfo.getWeather().getWeatherCode() : null;

                var rule = CategorySafetyRule.getBySlug(categorySlug);
                var eval = weatherRuleEngine.evaluate(rule, waveHeight, windSpeed, windGust, oceanCurrent, visibility, weatherCode);

                isSafe = eval.isSafe();
                alertLevel = eval.getAlertLevel();
                message = eval.getWarningMessage();
            }
        } catch (Exception e) {
            log.warn("Safety rule evaluation failed; returning an unavailable, fail-closed status", e);
        }

        result.put("isSafe", isSafe);
        result.put("alertLevel", alertLevel);
        result.put("message", message);

        String jsonResult;
        try {
            jsonResult = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            jsonResult = String.format("{\"location\":\"%s\",\"isSafe\":%s,\"alertLevel\":\"%s\",\"message\":\"%s\"}",
                    location, isSafe, alertLevel, message);
        }

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, jsonResult, TTL);
            } catch (Exception e) {
                log.warn("Failed to cache safety alert in Redis", e);
            }
        }

        return jsonResult;
    }

    private double[] resolveCoordinates(String location, Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("latitude and longitude must be provided together");
        }
        if (latitude != null) {
            if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                    || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("latitude or longitude is outside the valid range");
            }
            return new double[]{latitude, longitude};
        }

        String normalized = location.toLowerCase(Locale.ROOT);
        if (normalized.contains("mỹ khê") || normalized.contains("my khe")) {
            return new double[]{16.0544, 108.2469};
        }
        if (normalized.contains("sơn trà") || normalized.contains("son tra")) {
            return new double[]{16.1067, 108.2774};
        }
        if (normalized.contains("non nước") || normalized.contains("non nuoc")) {
            return new double[]{16.0050, 108.2690};
        }
        if (normalized.contains("mân thái") || normalized.contains("man thai")) {
            return new double[]{16.0890, 108.2495};
        }
        return new double[]{16.0544, 108.2022};
    }

    private String error(String code, String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", code, "message", message));
        } catch (Exception exception) {
            return "{\"error\":\"" + code + "\"}";
        }
    }

    private boolean hasSafetyData(WeatherInfoDto weatherInfo) {
        if (weatherInfo == null) {
            return false;
        }
        WeatherInfoDto.WeatherData weather = weatherInfo.getWeather();
        WeatherInfoDto.MarineData marine = weatherInfo.getMarine();
        return (weather != null && (weather.getWindSpeed() != null
                || weather.getWindGust() != null
                || weather.getVisibility() != null
                || weather.getWeatherCode() != null))
                || (marine != null && (marine.getWaveHeight() != null
                || marine.getOceanCurrentVelocity() != null));
    }
}
