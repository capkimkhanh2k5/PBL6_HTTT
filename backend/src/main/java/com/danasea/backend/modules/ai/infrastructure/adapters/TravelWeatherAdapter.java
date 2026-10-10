package com.danasea.backend.modules.ai.infrastructure.adapters;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.ports.TravelWeatherPort;
import com.danasea.backend.modules.ai.domain.models.TravelContext;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Slot;
import com.danasea.backend.modules.weather.application.usecases.CheckAdvanceBookingSafetyUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TravelWeatherAdapter implements TravelWeatherPort {
    private final CheckAdvanceBookingSafetyUseCase safety;

    @Override
    public Assessment assess(PublishedService service, Slot slot) {
        if (!service.weatherSensitive()) return new Assessment("NOT_REQUIRED", true, false, null, null, null);
        if (service.latitude() == null || service.longitude() == null) return unknown("UNKNOWN", "Service coordinates are unavailable");
        if (slot.date().isAfter(LocalDate.now(TravelContext.ZONE).plusDays(16))) return unknown("WAITING_FOR_FORECAST", "The slot is outside the direct forecast horizon");
        try {
            var result = safety.checkBySlotId(slot.id());
            var forecast = result.getForecast();
            Instant now = Instant.now();
            boolean missing = forecast == null || forecast.getPeakWindSpeed() == null;
            boolean stale = !missing && (forecast.getSourceFetchedAt() == null || forecast.getValidUntil() == null || forecast.getSourceFetchedAt().isAfter(now.plusSeconds(60)) || !forecast.getValidUntil().isAfter(now));
            boolean provisional = result.isProvisional() || result.isEstimatedMarine();
            Map<String, Double> metrics = new LinkedHashMap<>();
            Map<String, Double> thresholds = new LinkedHashMap<>();
            if (forecast != null) {
                put(metrics, "peakWaveHeightM", forecast.getPeakWaveHeight());
                put(metrics, "peakWindSpeedKmh", forecast.getPeakWindSpeed());
                put(metrics, "peakWindGustKmh", forecast.getPeakWindGust());
                put(metrics, "minimumVisibilityM", forecast.getMinVisibility());
                put(metrics, "peakCurrentMs", forecast.getPeakOceanCurrent());
            }
            put(thresholds, "maxWaveHeightM", result.getMaxWaveHeightM());
            put(thresholds, "maxWindSpeedKmh", result.getMaxWindSpeedKmh());
            put(thresholds, "maxWindGustKmh", result.getMaxWindGustKmh());
            put(thresholds, "minVisibilityM", result.getRuleMinVisibilityM());
            return new Assessment(missing ? "UNKNOWN" : stale ? "STALE" : result.isSafe() ? provisional ? "PROVISIONAL" : "ACCEPTABLE" : "UNSAFE",
                    !missing && !stale && result.isSafe(), provisional, result.getAlertLevel(),
                    stale ? "Forecast freshness cannot be verified; revalidation is required" : result.getWarningMessage(), result.getDataCoverage(),
                    forecast == null ? null : forecast.getProvider(), forecast == null ? null : forecast.getSourceFetchedAt(), now,
                    forecast == null ? null : forecast.getValidUntil(), forecast == null ? "Asia/Ho_Chi_Minh" : forecast.getTimezone(),
                    Map.copyOf(metrics), Map.copyOf(thresholds), missing || stale || provisional);
        } catch (Exception exception) { return unknown("UNKNOWN", "Weather assessment is unavailable"); }
    }

    private Assessment unknown(String status, String message) { return new Assessment(status, false, false, null, message, null); }
    private void put(Map<String, Double> values, String key, Double value) { if (value != null && Double.isFinite(value)) values.put(key, value); }
}
