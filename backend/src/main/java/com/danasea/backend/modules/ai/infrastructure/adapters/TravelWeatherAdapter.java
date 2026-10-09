package com.danasea.backend.modules.ai.infrastructure.adapters;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.ai.application.port.TravelWeatherPort;
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
        if (service.latitude() == null || service.longitude() == null) {
            return new Assessment("UNKNOWN", false, false, null, "Service coordinates are unavailable", null);
        }
        try {
            var result = safety.checkBySlotId(slot.id());
            boolean missing = result.getForecast() == null
                    || result.getForecast().getPeakWindSpeed() == null;
            return new Assessment(missing ? "UNKNOWN" : result.isSafe() ? "ACCEPTABLE" : "UNSAFE",
                    !missing && result.isSafe(), result.isProvisional() || result.isEstimatedMarine(), result.getAlertLevel(),
                    result.getWarningMessage(), result.getDataCoverage());
        } catch (Exception exception) {
            return new Assessment("UNKNOWN", false, false, null, "Weather assessment is unavailable", null);
        }
    }
}
