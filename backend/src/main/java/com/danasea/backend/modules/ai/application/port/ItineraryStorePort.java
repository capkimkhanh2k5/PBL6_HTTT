package com.danasea.backend.modules.ai.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.ai.domain.models.ItineraryPlan.Saved;
import com.danasea.backend.modules.ai.domain.models.ItineraryPlan;

public interface ItineraryStorePort {
    Saved create(UUID ownerId, ItineraryPlan plan);
    Optional<Saved> find(UUID id, UUID ownerId);
    List<Saved> list(UUID ownerId);
    Saved replace(UUID id, UUID ownerId, long expectedVersion, ItineraryPlan plan);
}
