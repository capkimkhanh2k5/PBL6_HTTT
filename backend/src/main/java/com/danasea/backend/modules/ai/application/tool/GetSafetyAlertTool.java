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
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.danasea.backend.modules.weather.application.services.WeatherSafetyMessageRenderer;

@Slf4j
@Component
public class GetSafetyAlertTool implements ToolExecutor {

    private final GetWeatherInfoUseCase weatherInfoUseCase;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine weatherRuleEngine;
    private LocalizedMessageService messages = LocalizedMessageService.standalone();
    private WeatherSafetyMessageRenderer weatherMessages = new WeatherSafetyMessageRenderer();

    @Autowired
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.messages = messages;
        this.weatherMessages = new WeatherSafetyMessageRenderer(messages);
    }

    private static final String CACHE_PREFIX = "ai:weather:safety:";
    private static final Duration TTL = Duration.ofMinutes(10); // 5-15 min TTL
    private static final String DEFAULT_LOCATION = "Biển Đà Nẵng";
    private static final String ALERT_GREEN = "GREEN";
    private static final String ALERT_YELLOW = "YELLOW";
    private static final String ALERT_RED = "RED";

    @Autowired
    public GetSafetyAlertTool(@Autowired(required = false) GetWeatherInfoUseCase weatherInfoUseCase,
                              @Autowired(required = false) StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              @Autowired(required = false) com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine weatherRuleEngine) {
        this.weatherInfoUseCase = weatherInfoUseCase;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.weatherRuleEngine = weatherRuleEngine != null ? weatherRuleEngine : new com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine();
    }

    public GetSafetyAlertTool(GetWeatherInfoUseCase weatherInfoUseCase,
                              StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper) {
        this(weatherInfoUseCase, redisTemplate, objectMapper, new com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine());
    }

    public GetSafetyAlertTool() {
        this(null, null, new ObjectMapper(), new com.danasea.backend.modules.weather.domain.services.WeatherRuleEngine());
    }

    @Override
    public String getName() {
        return "get_safety_alert";
    }

    @Override
    public String execute(String argumentsJson) {
        return executeInternal(argumentsJson, null);
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        return executeInternal(argumentsJson, context == null ? SupportedLanguage.VI : context.language());
    }

    private String executeInternal(String argumentsJson, SupportedLanguage language) {
        String location = DEFAULT_LOCATION;
        String categorySlug = "default";

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
            }
        } catch (Exception e) {
            log.warn("Failed to parse get_safety_alert arguments: {}", argumentsJson, e);
        }

        String cacheKey = "default".equalsIgnoreCase(categorySlug)
                ? CACHE_PREFIX + location
                : CACHE_PREFIX + location + ":" + categorySlug;
        if (language != null) {
            cacheKey += ":" + language.code();
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

        boolean isSafe = true;
        String alertLevel = ALERT_GREEN;
        String message = language == null
                ? "Điều kiện an toàn hàng hải và thời tiết tốt, các hoạt động vui chơi trên biển diễn ra bình thường."
                : messages.get("weather.safety.safe", language);

        try {
            WeatherInfoDto weatherInfo = weatherInfoUseCase != null ? weatherInfoUseCase.execute() : null;
            if (weatherInfo != null) {
                Double waveHeight = weatherInfo.getMarine() != null ? weatherInfo.getMarine().getWaveHeight() : null;
                Double oceanCurrent = weatherInfo.getMarine() != null ? weatherInfo.getMarine().getOceanCurrentVelocity() : null;
                Double windSpeed = weatherInfo.getWeather() != null ? weatherInfo.getWeather().getWindSpeed() : null;
                Double windGust = weatherInfo.getWeather() != null ? weatherInfo.getWeather().getWindGust() : null;
                Double visibility = weatherInfo.getWeather() != null ? weatherInfo.getWeather().getVisibility() : null;
                Integer weatherCode = weatherInfo.getWeather() != null ? weatherInfo.getWeather().getWeatherCode() : null;

                var rule = com.danasea.backend.modules.weather.domain.models.CategorySafetyRule.getBySlug(categorySlug);
                var eval = weatherRuleEngine.evaluate(rule, waveHeight, windSpeed, windGust, oceanCurrent, visibility, weatherCode);

                isSafe = eval.isSafe();
                alertLevel = eval.getAlertLevel();
                message = language == null
                        ? eval.getWarningMessage()
                        : weatherMessages.render(eval, language);
            }
        } catch (Exception e) {
            log.warn("Safety rule evaluation failed, using default safe status", e);
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
}
