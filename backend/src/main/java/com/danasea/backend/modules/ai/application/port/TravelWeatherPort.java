package com.danasea.backend.modules.ai.application.port;

import java.time.Instant;
import java.util.Map;

import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Slot;

public interface TravelWeatherPort {
    record Assessment(String status, boolean acceptable, boolean provisional, String alertLevel,
                      String message, String dataCoverage, String provider, Instant forecastAt, Instant checkedAt,
                      Instant validUntil, String timezone, Map<String, Double> metrics, Map<String, Double> thresholds,
                      boolean requiresRevalidation) {
        public Assessment(String status, boolean acceptable, boolean provisional, String alertLevel,
                          String message, String dataCoverage) {
            this(status, acceptable, provisional, alertLevel, message, dataCoverage, null, null, Instant.now(),
                    null, "Asia/Ho_Chi_Minh", Map.of(), Map.of(), !acceptable || provisional);
        }
    }
    Assessment assess(PublishedService service, Slot slot);
}
