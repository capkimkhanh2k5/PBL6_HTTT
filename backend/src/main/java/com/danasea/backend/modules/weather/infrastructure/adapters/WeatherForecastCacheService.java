package com.danasea.backend.modules.weather.infrastructure.adapters;

import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoMarineResponse;
import com.danasea.backend.modules.weather.infrastructure.api.dto.OpenMeteoWeatherResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * High-performance multi-day forecast caching service for Open-Meteo Weather (16 days) and Marine (8 days).
 * Provides two-tier caching:
 * - Tier 1: Fast local thread-safe ConcurrentHashMap in-memory cache with TTL (1-3 hours, default 2 hours).
 * - Tier 2: Redis distributed cache (when available) for cluster-wide consistency across instances.
 * Prevents third-party rate limits and enables rapid 7-14 day advance booking lookups.
 */
@Slf4j
@Service
public class WeatherForecastCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private Duration ttl = Duration.ofHours(2);

    private final Map<String, CacheEntry<OpenMeteoWeatherResponse>> localWeatherCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<OpenMeteoMarineResponse>> localMarineCache = new ConcurrentHashMap<>();

    private static class CacheEntry<T> {
        private final T data;
        private final Instant expiresAt;

        public CacheEntry(T data, Duration ttl) {
            this.data = data;
            this.expiresAt = Instant.now().plus(ttl);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public T getData() {
            return data;
        }
    }

    @Autowired
    public WeatherForecastCacheService(@Autowired(required = false) StringRedisTemplate redisTemplate,
                                       ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public WeatherForecastCacheService() {
        this(null, new ObjectMapper());
    }

    public OpenMeteoWeatherResponse getOrFetchWeather(double lat, double lon, Supplier<OpenMeteoWeatherResponse> fetcher) {
        String key = buildWeatherCacheKey(lat, lon);

        // 1. Check local memory cache
        CacheEntry<OpenMeteoWeatherResponse> localEntry = localWeatherCache.get(key);
        if (localEntry != null && !localEntry.isExpired()) {
            return localEntry.getData();
        }

        // 2. Check Redis if available
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(key);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    OpenMeteoWeatherResponse parsed = objectMapper.readValue(cachedJson, OpenMeteoWeatherResponse.class);
                    localWeatherCache.put(key, new CacheEntry<>(parsed, ttl));
                    return parsed;
                }
            } catch (Exception e) {
                log.warn("Redis read failed for {}: {}", key, e.getMessage());
            }
        }

        // 3. Fetch from API
        OpenMeteoWeatherResponse fetched = fetcher.get();
        if (fetched != null) {
            localWeatherCache.put(key, new CacheEntry<>(fetched, ttl));
            if (redisTemplate != null) {
                try {
                    String json = objectMapper.writeValueAsString(fetched);
                    redisTemplate.opsForValue().set(key, json, ttl);
                } catch (Exception e) {
                    log.warn("Redis write failed for {}: {}", key, e.getMessage());
                }
            }
        }

        return fetched;
    }

    public OpenMeteoMarineResponse getOrFetchMarine(double lat, double lon, Supplier<OpenMeteoMarineResponse> fetcher) {
        String key = buildMarineCacheKey(lat, lon);

        // 1. Check local memory cache
        CacheEntry<OpenMeteoMarineResponse> localEntry = localMarineCache.get(key);
        if (localEntry != null && !localEntry.isExpired()) {
            return localEntry.getData();
        }

        // 2. Check Redis if available
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(key);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    OpenMeteoMarineResponse parsed = objectMapper.readValue(cachedJson, OpenMeteoMarineResponse.class);
                    localMarineCache.put(key, new CacheEntry<>(parsed, ttl));
                    return parsed;
                }
            } catch (Exception e) {
                log.warn("Redis read failed for {}: {}", key, e.getMessage());
            }
        }

        // 3. Fetch from API
        OpenMeteoMarineResponse fetched = fetcher.get();
        if (fetched != null) {
            localMarineCache.put(key, new CacheEntry<>(fetched, ttl));
            if (redisTemplate != null) {
                try {
                    String json = objectMapper.writeValueAsString(fetched);
                    redisTemplate.opsForValue().set(key, json, ttl);
                } catch (Exception e) {
                    log.warn("Redis write failed for {}: {}", key, e.getMessage());
                }
            }
        }

        return fetched;
    }

    public void invalidate(double lat, double lon) {
        String weatherKey = buildWeatherCacheKey(lat, lon);
        String marineKey = buildMarineCacheKey(lat, lon);
        localWeatherCache.remove(weatherKey);
        localMarineCache.remove(marineKey);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(weatherKey);
                redisTemplate.delete(marineKey);
            } catch (Exception ignored) {
            }
        }
    }

    public void clear() {
        localWeatherCache.clear();
        localMarineCache.clear();
    }

    public boolean isWeatherCached(double lat, double lon) {
        String key = buildWeatherCacheKey(lat, lon);
        CacheEntry<OpenMeteoWeatherResponse> localEntry = localWeatherCache.get(key);
        return localEntry != null && !localEntry.isExpired();
    }

    public boolean isMarineCached(double lat, double lon) {
        String key = buildMarineCacheKey(lat, lon);
        CacheEntry<OpenMeteoMarineResponse> localEntry = localMarineCache.get(key);
        return localEntry != null && !localEntry.isExpired();
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getTtl() {
        return ttl;
    }

    private String buildWeatherCacheKey(double lat, double lon) {
        return String.format(Locale.US, "weather:forecast:%.4f:%.4f", lat, lon);
    }

    private String buildMarineCacheKey(double lat, double lon) {
        return String.format(Locale.US, "marine:forecast:%.4f:%.4f", lat, lon);
    }
}
