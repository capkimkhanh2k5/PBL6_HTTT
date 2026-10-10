package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.danasea.backend.modules.service.application.services.ServiceDiscoveryAvailability;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.ports.ServiceAvailabilityPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ServiceAvailabilityAdapter implements ServiceAvailabilityPort {
    private final ServiceDiscoveryAvailability availability;

    @Override
    @Transactional(readOnly = true)
    public List<String> findAvailableSlots(UUID serviceId) {
        return availability.describe(Service.builder().id(serviceId).build()).stream().filter(slot -> slot.bookable())
                .map(slot -> slot.date() + "T" + slot.startTime().format(DateTimeFormatter.ISO_LOCAL_TIME))
                .distinct().toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findAvailableSlotId(UUID serviceId, String slotValue) {
        if (serviceId == null || slotValue == null || slotValue.isBlank()) {
            return Optional.empty();
        }
        final LocalDateTime requested;
        try {
            requested = LocalDateTime.parse(slotValue);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
        return availability.describe(Service.builder().id(serviceId).build()).stream().filter(slot -> slot.bookable())
                .filter(slot -> slot.date().equals(requested.toLocalDate()) && slot.startTime().equals(requested.toLocalTime()))
                .map(slot -> slot.slotId()).findFirst();
    }
}
