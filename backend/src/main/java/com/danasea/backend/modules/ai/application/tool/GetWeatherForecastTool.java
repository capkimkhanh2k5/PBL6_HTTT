package com.danasea.backend.modules.ai.application.tool;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import com.danasea.backend.shared.i18n.LocalizedMessageService;
import com.danasea.backend.shared.i18n.SupportedLanguage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class GetWeatherForecastTool implements ToolExecutor {

    private static final String CACHE_PREFIX = "ai:weather:forecast:";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration UNAVAILABLE_TTL = Duration.ofMinutes(1);
    private static final String DEFAULT_LOCATION = "Da Nang";
    private static final String DEFAULT_DATE = "today";

    private final GetWeatherInfoUseCase weatherInfoUseCase;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WeatherProviderPort weatherProviderPort;
    private LocalizedMessageService messages = LocalizedMessageService.standalone();

    @Autowired
    void setLocalizedMessageService(LocalizedMessageService messages) {
        this.messages = messages;
    }

    @Autowired
    public GetWeatherForecastTool(@Autowired(required = false) GetWeatherInfoUseCase weatherInfoUseCase,
                                  @Autowired(required = false) StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  @Autowired(required = false) WeatherProviderPort weatherProviderPort) {
        this.weatherInfoUseCase = weatherInfoUseCase;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.weatherProviderPort = weatherProviderPort;
    }

    public GetWeatherForecastTool(GetWeatherInfoUseCase weatherInfoUseCase,
                                  StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper) {
        this(weatherInfoUseCase, redisTemplate, objectMapper, null);
    }

    public GetWeatherForecastTool() {
        this(null, null, new ObjectMapper(), null);
    }

    @Override
    public String getName() {
        return "get_weather_forecast";
    }

    @Override
    public String execute(String argumentsJson) {
        return executeInternal(argumentsJson, null);
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        return executeInternal(argumentsJson, context == null ? null : context.language());
    }

    private String executeInternal(String argumentsJson, SupportedLanguage language) {
        String location = DEFAULT_LOCATION;
        String date = DEFAULT_DATE;
        Double latitude = null;
        Double longitude = null;

        try {
            if (argumentsJson != null && !argumentsJson.isBlank()) {
                JsonNode node = objectMapper.readTree(argumentsJson);
                if (node.has("location") && !node.get("location").isNull()) {
                    location = node.get("location").asText();
                }
                if (node.has("date") && !node.get("date").isNull()) {
                    date = node.get("date").asText();
                }
                if (node.has("latitude") && node.get("latitude").isNumber()) {
                    latitude = node.get("latitude").asDouble();
                }
                if (node.has("longitude") && node.get("longitude").isNumber()) {
                    longitude = node.get("longitude").asDouble();
                }
            }
        } catch (Exception exception) {
            log.warn("Failed to parse get_weather_forecast arguments", exception);
        }

        location = location == null ? "" : location.trim();
        date = date == null ? "" : date.trim();
        if (location.isBlank() || location.length() > 120) {
            return error("INVALID_ARGUMENTS", "location is required and must not exceed 120 characters");
        }

        boolean explicitCoordinates = latitude != null || longitude != null;
        double[] coordinates;
        LocalDate targetDate;
        try {
            coordinates = resolveCoordinates(location, latitude, longitude);
            targetDate = parseDate(date);
        } catch (IllegalArgumentException exception) {
            return error("INVALID_ARGUMENTS", exception.getMessage());
        }
        long daysAhead = ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
        if (daysAhead < 0 || daysAhead > 16) {
            return error("DATE_OUT_OF_RANGE", "date must be from today through 16 days ahead");
        }

        String cacheKey = CACHE_PREFIX + location + ":" + date
                + (explicitCoordinates
                        ? String.format(Locale.ROOT, ":%.4f:%.4f", coordinates[0], coordinates[1])
                        : "")
                + (language == null ? "" : ":" + language.code());
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null && !cached.isBlank()) {
                    return cached;
                }
            } catch (Exception exception) {
                log.warn("Failed to read weather forecast from Redis", exception);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("location", location);
        result.put("date", date);
        Duration cacheTtl = TTL;

        try {
            if (weatherProviderPort != null && daysAhead > 0) {
                WeatherInfoDto.TimeWindowForecast forecast = weatherProviderPort.getTimeWindowForecast(
                        coordinates[0], coordinates[1], targetDate, LocalTime.MIN, LocalTime.MAX);
                if (hasForecastData(forecast)) {
                    result.put("available", true);
                    result.put("latitude", coordinates[0]);
                    result.put("longitude", coordinates[1]);
                    result.put("peakWaveHeight", forecast.getPeakWaveHeight());
                    result.put("peakWindSpeed", forecast.getPeakWindSpeed());
                    result.put("peakWindGust", forecast.getPeakWindGust());
                    result.put("minimumVisibility", forecast.getMinVisibility());
                    result.put("weatherCode", forecast.getSevereWeatherCode());
                    result.put("totalPrecipitation", forecast.getTotalPrecipitation());
                } else {
                    markUnavailable(result, language);
                    cacheTtl = UNAVAILABLE_TTL;
                }
            } else {
                WeatherInfoDto weatherInfo = weatherInfoUseCase != null
                        ? (weatherProviderPort != null
                                ? weatherInfoUseCase.execute(coordinates[0], coordinates[1])
                                : weatherInfoUseCase.execute())
                        : null;
                if (hasForecastData(weatherInfo)) {
                    result.put("available", true);
                    result.put("latitude", coordinates[0]);
                    result.put("longitude", coordinates[1]);
                    if (weatherInfo.getWeather() != null) {
                        result.put("temperature", weatherInfo.getWeather().getTemperature());
                        result.put("windSpeed", weatherInfo.getWeather().getWindSpeed());
                        result.put("windGust", weatherInfo.getWeather().getWindGust());
                        result.put("weatherCode", weatherInfo.getWeather().getWeatherCode());
                    }
                    if (weatherInfo.getMarine() != null) {
                        result.put("waveHeight", weatherInfo.getMarine().getWaveHeight());
                    }
                } else {
                    markUnavailable(result, language);
                    cacheTtl = UNAVAILABLE_TTL;
                }
            }
        } catch (Exception exception) {
            log.warn("Weather provider query failed; returning an explicit unavailable result", exception);
            markUnavailable(result, language);
            cacheTtl = UNAVAILABLE_TTL;
        }

        String jsonResult;
        try {
            jsonResult = objectMapper.writeValueAsString(result);
        } catch (Exception exception) {
            jsonResult = "{\"available\":false}";
        }

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, jsonResult, cacheTtl);
            } catch (Exception exception) {
                log.warn("Failed to cache weather forecast in Redis", exception);
            }
        }
        return jsonResult;
    }

    private boolean hasForecastData(WeatherInfoDto weatherInfo) {
        if (weatherInfo == null) {
            return false;
        }
        WeatherInfoDto.WeatherData weather = weatherInfo.getWeather();
        WeatherInfoDto.MarineData marine = weatherInfo.getMarine();
        return (weather != null && (weather.getTemperature() != null
                || weather.getWindSpeed() != null
                || weather.getWindGust() != null
                || weather.getWeatherCode() != null))
                || (marine != null && marine.getWaveHeight() != null);
    }

    private boolean hasForecastData(WeatherInfoDto.TimeWindowForecast forecast) {
        return forecast != null && (forecast.getPeakWaveHeight() != null
                || forecast.getPeakWindSpeed() != null
                || forecast.getPeakWindGust() != null
                || forecast.getMinVisibility() != null
                || forecast.getSevereWeatherCode() != null);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank() || DEFAULT_DATE.equalsIgnoreCase(value)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("date must use YYYY-MM-DD format");
        }
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

        String normalized = location == null ? "" : location.toLowerCase(Locale.ROOT);
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

    private void markUnavailable(Map<String, Object> result, SupportedLanguage language) {
        result.put("available", false);
        result.put("message", language == null
                ? "Weather forecast data is temporarily unavailable. Check an official source before making safety decisions."
                : messages.get("ai.weather.unavailable", language));
    }
}
