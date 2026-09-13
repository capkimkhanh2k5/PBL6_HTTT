package com.danasea.backend.modules.weather.infrastructure.adapters;

import com.danasea.backend.modules.weather.application.ports.output.WeatherCacheRepositoryPort;
import com.danasea.backend.modules.weather.domain.models.WeatherCache;
import com.danasea.backend.modules.weather.infrastructure.persistence.entities.WeatherCacheJpaEntity;
import com.danasea.backend.modules.weather.infrastructure.persistence.repositories.JpaWeatherCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WeatherCacheAdapter implements WeatherCacheRepositoryPort {

    private final JpaWeatherCacheRepository repository;

    @Override
    public void save(WeatherCache weatherCache) {
        WeatherCacheJpaEntity entity = new WeatherCacheJpaEntity();
        if (weatherCache.getId() != null) {
            entity.setId(weatherCache.getId());
        }
        entity.setLocationKey(weatherCache.getLocationKey());
        entity.setWindSpeedKmh(weatherCache.getWindSpeedKmh());
        entity.setWaveHeightM(weatherCache.getWaveHeightM());
        entity.setPrecipitationMm(weatherCache.getPrecipitationMm());
        entity.setRawPayload(weatherCache.getRawPayload());
        entity.setFetchedAt(weatherCache.getFetchedAt());
        entity.setExpiresAt(weatherCache.getExpiresAt());
        
        repository.save(entity);
    }

    @Override
    public Optional<WeatherCache> getLatestByLocation(String locationKey) {
        return repository.findFirstByLocationKeyOrderByFetchedAtDesc(locationKey)
                .map(entity -> {
                    WeatherCache cache = new WeatherCache();
                    cache.setId(entity.getId());
                    cache.setLocationKey(entity.getLocationKey());
                    cache.setWindSpeedKmh(entity.getWindSpeedKmh());
                    cache.setWaveHeightM(entity.getWaveHeightM());
                    cache.setPrecipitationMm(entity.getPrecipitationMm());
                    cache.setRawPayload(entity.getRawPayload());
                    cache.setFetchedAt(entity.getFetchedAt());
                    cache.setExpiresAt(entity.getExpiresAt());
                    return cache;
                });
    }
}
