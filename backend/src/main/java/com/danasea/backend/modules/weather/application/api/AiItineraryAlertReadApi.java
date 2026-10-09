package com.danasea.backend.modules.weather.application.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AiItineraryAlertReadApi {
    record Signal(UUID slotId, String eventId, String trigger, OffsetDateTime sourceAt) {}
    List<Signal> activeSignals(Set<UUID> slotIds);
}
