package com.danasea.backend.modules.booking.infrastructure.adapters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.booking.domain.models.SlotValidationDetails;
import com.danasea.backend.modules.booking.domain.ports.ServiceSlotPort;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceSlotRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ServiceSlotAdapter implements ServiceSlotPort {

    private final JpaServiceSlotRepository jpaServiceSlotRepository;
    private final JpaServiceRepository jpaServiceRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<SlotValidationDetails> findSlotDetails(UUID slotId) {
        if (slotId == null) {
            return Optional.empty();
        }
        List<SlotValidationDetails> list = findSlotDetailsBatch(List.of(slotId));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotValidationDetails> findSlotDetailsBatch(List<UUID> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ServiceSlotJpaEntity> slotEntities = jpaServiceSlotRepository.findAllById(slotIds);
        if (slotEntities.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> serviceIds = slotEntities.stream()
                .map(ServiceSlotJpaEntity::getServiceId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<UUID, ServiceJpaEntity> serviceMap = jpaServiceRepository.findAllById(serviceIds).stream()
                .collect(Collectors.toMap(ServiceJpaEntity::getId, Function.identity()));

        return slotEntities.stream()
                .map(slot -> {
                    ServiceJpaEntity service = serviceMap.get(slot.getServiceId());
                    boolean published = service != null && ServiceStatus.PUBLISHED.equals(service.getStatus());

                    return SlotValidationDetails.builder()
                            .slotId(slot.getId())
                            .serviceId(slot.getServiceId())
                            .vendorId(service != null ? service.getVendorId() : null)
                            .serviceName(service != null ? service.getName() : null)
                            .bookingDate(slot.getDate())
                            .bookingTime(slot.getStartTime())
                            .capacity(slot.getCapacity())
                            .bookedCount(slot.getBookedCount())
                            .status(slot.getStatus())
                            .price(service != null ? service.getPrice() : null)
                            .servicePublished(published)
                            .build();
                })
                .toList();
    }
}
