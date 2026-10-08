package com.danasea.backend.modules.service.domain.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.ServiceSlot;
import com.danasea.backend.modules.service.domain.models.ServiceSlotUnit;

public interface ServiceSlotRepositoryPort {

    ServiceSlot save(ServiceSlot slot);

    Optional<ServiceSlot> findById(UUID id);

    List<ServiceSlot> findByServiceId(UUID serviceId);

    List<ServiceSlot> findByServiceIdAndDateBetween(UUID serviceId, LocalDate from, LocalDate to);

    List<ServiceSlotUnit> findUnitsBySlotId(UUID slotId);

    List<ServiceSlotUnit> findUnitsBySlotIds(List<UUID> slotIds);

    void saveUnits(UUID slotId, List<ServiceSlotUnit> units);

    void deleteUnitsBySlotId(UUID slotId);

    boolean hasActiveBookingOrHold(UUID slotId);

    int getCommittedCountForUnit(UUID slotId, int unitNumber);
}
