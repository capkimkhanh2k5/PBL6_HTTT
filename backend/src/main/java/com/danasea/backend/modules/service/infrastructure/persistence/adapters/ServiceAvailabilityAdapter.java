package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.service.domain.models.SlotStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceAvailabilityPort;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceAvailabilityAdapter implements ServiceAvailabilityPort {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JpaServiceSlotRepository slotRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> findAvailableSlots(UUID serviceId) {
        return slotRepository
                .findByServiceIdAndStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                        serviceId, SlotStatus.OPEN, LocalDate.now())
                .stream()
                .filter(slot -> slot.getCapacity() != null
                        && slot.getBookedCount() != null
                        && slot.getBookedCount() < slot.getCapacity())
                .map(slot -> slot.getDate() + "T" + slot.getStartTime().format(TIME_FORMAT))
                .toList();
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
        return slotRepository
                .findByServiceIdAndStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                        serviceId, SlotStatus.OPEN, LocalDate.now())
                .stream()
                .filter(slot -> slot.getDate().equals(requested.toLocalDate()))
                .filter(slot -> slot.getStartTime().equals(requested.toLocalTime()))
                .filter(slot -> slot.getCapacity() != null
                        && slot.getBookedCount() != null
                        && slot.getBookedCount() < slot.getCapacity())
                .map(slot -> slot.getId())
                .findFirst();
    }
}
