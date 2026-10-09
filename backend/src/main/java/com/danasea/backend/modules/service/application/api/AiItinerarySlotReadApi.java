package com.danasea.backend.modules.service.application.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AiItinerarySlotReadApi {
    record Signal(UUID slotId, String eventId, String trigger, OffsetDateTime sourceAt) {}
    List<Signal> unavailableSignals(Set<UUID> slotIds);
    Set<UUID> futureSlotIds(Set<UUID> slotIds);
}
