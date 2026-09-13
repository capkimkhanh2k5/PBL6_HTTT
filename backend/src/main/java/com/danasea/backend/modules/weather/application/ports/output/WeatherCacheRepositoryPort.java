package com.danasea.backend.modules.weather.application.ports.output;

import com.danasea.backend.modules.weather.domain.models.WeatherCache;
import java.util.Optional;

public interface WeatherCacheRepositoryPort {
    void save(WeatherCache weatherCache);
    Optional<WeatherCache> getLatestByLocation(String locationKey);
}
