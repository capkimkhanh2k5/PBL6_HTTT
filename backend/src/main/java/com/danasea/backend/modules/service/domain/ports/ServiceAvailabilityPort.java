package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceAvailabilityPort {
    List<String> findAvailableSlots(UUID serviceId);

    Optional<UUID> findAvailableSlotId(UUID serviceId, String slotValue);
}
