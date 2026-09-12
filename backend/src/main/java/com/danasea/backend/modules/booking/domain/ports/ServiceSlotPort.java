package com.danasea.backend.modules.booking.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.SlotValidationDetails;

public interface ServiceSlotPort {

    Optional<SlotValidationDetails> findSlotDetails(UUID slotId);

    List<SlotValidationDetails> findSlotDetailsBatch(List<UUID> slotIds);
}
