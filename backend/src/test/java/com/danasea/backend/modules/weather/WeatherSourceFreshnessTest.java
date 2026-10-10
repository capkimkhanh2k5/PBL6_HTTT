package com.danasea.backend.modules.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.danasea.backend.modules.weather.application.dtos.WeatherInfoDto;
import com.danasea.backend.modules.weather.application.ports.output.WeatherCacheRepositoryPort;
import com.danasea.backend.modules.weather.application.ports.output.WeatherProviderPort;
import com.danasea.backend.modules.weather.application.usecases.GetWeatherInfoUseCase;
import com.danasea.backend.modules.weather.domain.models.WeatherCache;
import com.danasea.backend.modules.weather.infrastructure.adapters.WeatherForecastCacheService;
import com.danasea.backend.modules.weather.infrastructure.api.dtos.OpenMeteoWeatherResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

class WeatherSourceFreshnessTest {
    @Test
    void redisStaleFallbackSurvivesProcessRestartWithoutResettingSourceAge() throws Exception {
        var redis = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
        var mapper = new ObjectMapper();
        var old = new OpenMeteoWeatherResponse();
        long timestamp = Instant.now().minusSeconds(5400).toEpochMilli();
        old.setSourceFetchedAtEpochMillis(timestamp);
        when(redis.opsForValue().get(anyString())).thenReturn(mapper.writeValueAsString(old));
        var cache = new WeatherForecastCacheService(redis, mapper);
        var fetches = new AtomicInteger();
        var result =
                cache.getOrFetchWeather(
                        16,
                        108,
                        () -> {
                            fetches.incrementAndGet();
                            throw new IllegalStateException("Timeout");
                        });
        assertEquals(1, fetches.get());
        assertEquals(timestamp, result.getSourceFetchedAtEpochMillis());
        assertFalse(cache.isWeatherCached(16, 108));
    }

    @Test
    void nullUpstreamFallsBackButExpiredRedisDataIsRejected() throws Exception {
        var redis = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
        var mapper = new ObjectMapper();
        var old = new OpenMeteoWeatherResponse();
        old.setSourceFetchedAtEpochMillis(Instant.now().minusSeconds(5400).toEpochMilli());
        when(redis.opsForValue().get(anyString())).thenReturn(mapper.writeValueAsString(old));
        assertNotNull(
                new WeatherForecastCacheService(redis, mapper)
                        .getOrFetchWeather(16, 108, () -> null));
        old.setSourceFetchedAtEpochMillis(Instant.now().minusSeconds(7260).toEpochMilli());
        when(redis.opsForValue().get(anyString())).thenReturn(mapper.writeValueAsString(old));
        assertThrows(
                IllegalStateException.class,
                () ->
                        new WeatherForecastCacheService(redis, mapper)
                                .getOrFetchWeather(
                                        16,
                                        108,
                                        () -> {
                                            throw new IllegalStateException("Timeout");
                                        }));
    }

    @Test
    void publicResponsePreservesOriginalAgeFromInnerCache() {
        var provider = mock(WeatherProviderPort.class);
        var repository = mock(WeatherCacheRepositoryPort.class);
        var mapper = new ObjectMapper().findAndRegisterModules();
        when(repository.getLatestByLocation(anyString())).thenReturn(Optional.empty());
        Instant timestamp = Instant.now().minusSeconds(5400);
        when(provider.getWeatherByCoordinates(anyDouble(), anyDouble()))
                .thenReturn(
                        WeatherInfoDto.WeatherData.builder()
                                .temperature(28.0)
                                .sourceFetchedAt(timestamp)
                                .build());
        var response = new GetWeatherInfoUseCase(repository, mapper, provider).execute(16, 108);
        assertEquals(timestamp, response.getFetchedAt());
        assertTrue(response.isStale());
        assertEquals("METEOROLOGY_ONLY", response.getDataCoverage());
        assertFalse(response.isEstimatedMarine());
        var saved = ArgumentCaptor.forClass(WeatherCache.class);
        verify(repository).save(saved.capture());
        assertEquals(timestamp, saved.getValue().getFetchedAt().toInstant());
    }
}
