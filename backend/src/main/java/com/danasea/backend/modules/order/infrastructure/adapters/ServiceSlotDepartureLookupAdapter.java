package com.danasea.backend.modules.order.infrastructure.adapters;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.domain.ports.ServiceSlotDepartureLookupPort;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;

@Component
public class ServiceSlotDepartureLookupAdapter implements ServiceSlotDepartureLookupPort {

    private final JpaServiceSlotRepository serviceSlotRepository;

    public ServiceSlotDepartureLookupAdapter(JpaServiceSlotRepository serviceSlotRepository) {
        this.serviceSlotRepository = serviceSlotRepository;
    }

    @Override
    public Optional<LocalDateTime> findDepartureTime(UUID slotId) {
        if (slotId == null) {
            return Optional.empty();
        }
        return serviceSlotRepository.findById(slotId)
                .filter(slot -> slot.getDate() != null && slot.getStartTime() != null)
                .map(slot -> LocalDateTime.of(slot.getDate(), slot.getStartTime()));
    }
}
