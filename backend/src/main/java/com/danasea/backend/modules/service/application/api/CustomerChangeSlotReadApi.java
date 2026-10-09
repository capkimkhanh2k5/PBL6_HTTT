package com.danasea.backend.modules.service.application.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface CustomerChangeSlotReadApi {
    record SlotTarget(UUID slotId, UUID serviceId, LocalDate date, LocalTime startTime, String status) {}
    SlotTarget read(UUID slotId);
}
