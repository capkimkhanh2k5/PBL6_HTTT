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
public class GetSafetyAlertTool implements ToolExecutor {

    private final GetWeatherInfoUseCase weatherInfoUseCase;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "ai:weather:safety:";
    private static final Duration TTL = Duration.ofMinutes(10); // 5-15 min TTL
    private static final String DEFAULT_LOCATION = "Biển Đà Nẵng";
    private static final String ALERT_GREEN = "GREEN";
    private static final String ALERT_YELLOW = "YELLOW";
    private static final String ALERT_RED = "RED";
    private static final double HIGH_WAVE_THRESHOLD = 2.0;
    private static final double MODERATE_WAVE_THRESHOLD = 1.0;

    @Autowired
    public GetSafetyAlertTool(@Autowired(required = false) GetWeatherInfoUseCase weatherInfoUseCase,
                              @Autowired(required = false) StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper) {
        this.weatherInfoUseCase = weatherInfoUseCase;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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

        try {
            if (argumentsJson != null && !argumentsJson.isBlank()) {
                JsonNode node = objectMapper.readTree(argumentsJson);
                if (node.has("location") && !node.get("location").isNull()) {
                    location = node.get("location").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse get_safety_alert arguments: {}", argumentsJson, e);
        }

        String cacheKey = CACHE_PREFIX + location;
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

        boolean isSafe = true;
        String alertLevel = ALERT_GREEN;
        String message = "Điều kiện an toàn hàng hải và thời tiết tốt, các hoạt động vui chơi trên biển diễn ra bình thường.";

        try {
            WeatherInfoDto weatherInfo = weatherInfoUseCase != null ? weatherInfoUseCase.execute() : null;
            if (weatherInfo != null && weatherInfo.getMarine() != null) {
                Double waveHeight = weatherInfo.getMarine().getWaveHeight();

                if (waveHeight != null && waveHeight > HIGH_WAVE_THRESHOLD) {
                    isSafe = false;
                    alertLevel = ALERT_RED;
                    message = "Cảnh báo sóng biển cao trên 2.0m. Tạm ngưng các hoạt động tàu bè và lặn biển.";
                } else if (waveHeight != null && waveHeight > MODERATE_WAVE_THRESHOLD) {
                    isSafe = true;
                    alertLevel = ALERT_YELLOW;
                    message = "Cảnh báo sóng ngầm nhẹ, khuyến cáo chỉ bơi trong vùng phao cứu sinh.";
                }
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
