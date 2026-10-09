package com.danasea.backend.modules.ai.application.port;

import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.PublishedService;
import com.danasea.backend.modules.service.application.api.AiCatalogReadApi.Slot;

public interface TravelWeatherPort {
    record Assessment(String status, boolean acceptable, boolean provisional, String alertLevel,
                      String message, String dataCoverage) {}
    Assessment assess(PublishedService service, Slot slot);
}
