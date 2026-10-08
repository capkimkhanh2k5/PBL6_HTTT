package com.danasea.backend.modules.service.application.usecases;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.VendorNotApprovedException;
import com.danasea.backend.modules.service.domain.models.InventoryType;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceSlot;
import com.danasea.backend.modules.service.domain.models.ServiceSlotUnit;
import com.danasea.backend.modules.service.domain.models.SlotStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceSlotRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.VendorPort;
import com.danasea.backend.modules.service.presentation.dtos.CreateServiceSlotRequest;
import com.danasea.backend.modules.service.presentation.dtos.CreateSlotUnitRequest;
import com.danasea.backend.modules.service.presentation.dtos.ServiceSlotResponse;
import com.danasea.backend.modules.service.presentation.dtos.ServiceSlotUnitResponse;
import com.danasea.backend.modules.vendor.domain.models.Vendor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateServiceSlotUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ServiceSlotRepositoryPort serviceSlotRepository;
    private final VendorPort vendorPort;

    @Transactional
    public ServiceSlotResponse execute(UUID userId, UUID serviceId, CreateServiceSlotRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        Vendor vendor = vendorPort.findByUserId(userId)
                .orElseThrow(() -> new VendorNotApprovedException("Vendor not found for user: " + userId));

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        service.validateOwnership(vendor.getId());

        if (request.date().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot create a slot in the past");
        }

        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        int totalCapacity;
        List<ServiceSlotUnit> units = new ArrayList<>();
        UUID slotId = UUID.randomUUID();

        if (InventoryType.SHARED_CAPACITY_UNITS.equals(request.inventoryType())) {
            if (request.units() == null || request.units().isEmpty()) {
                throw new IllegalArgumentException("SHARED_CAPACITY_UNITS inventory model requires a list of units");
            }

            Set<Integer> unitNumbers = new HashSet<>();
            int sum = 0;
            for (CreateSlotUnitRequest uReq : request.units()) {
                if (uReq.unitNumber() == null || uReq.unitNumber() <= 0) {
                    throw new IllegalArgumentException("Unit number must be greater than 0");
                }
                if (!unitNumbers.add(uReq.unitNumber())) {
                    throw new IllegalArgumentException("Duplicate unit number: " + uReq.unitNumber());
                }
                if (uReq.capacity() == null || uReq.capacity() <= 0) {
                    throw new IllegalArgumentException("Each unit capacity must be greater than 0");
                }
                sum += uReq.capacity();
                units.add(ServiceSlotUnit.builder()
                        .id(UUID.randomUUID())
                        .slotId(slotId)
                        .unitNumber(uReq.unitNumber())
                        .capacity(uReq.capacity())
                        .bookedCount(0)
                        .build());
            }
            totalCapacity = sum;
        } else {
            // PERSON_LIMIT
            if (request.capacity() == null || request.capacity() <= 0) {
                throw new IllegalArgumentException("Capacity must be greater than 0");
            }
            totalCapacity = request.capacity();
        }

        ServiceSlot slot = ServiceSlot.builder()
                .serviceId(serviceId)
                .date(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .capacity(totalCapacity)
                .bookedCount(0)
                .status(SlotStatus.OPEN)
                .inventoryType(request.inventoryType())
                .units(units)
                .build();
        slot.setId(slotId);

        ServiceSlot saved = serviceSlotRepository.save(slot);
        if (!units.isEmpty()) {
            serviceSlotRepository.saveUnits(slotId, units);
        }

        return toResponse(saved, units);
    }

    private ServiceSlotResponse toResponse(ServiceSlot s, List<ServiceSlotUnit> units) {
        List<ServiceSlotUnitResponse> unitResponses = units != null
                ? units.stream().map(this::toUnitResponse).toList()
                : List.of();

        return ServiceSlotResponse.builder()
                .id(s.getId())
                .serviceId(s.getServiceId())
                .date(s.getDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .inventoryType(s.getInventoryType())
                .capacity(s.getCapacity())
                .bookedCount(s.getBookedCount())
                .availableCapacity(s.getAvailableCapacity())
                .units(unitResponses)
                .build();
    }

    private ServiceSlotUnitResponse toUnitResponse(ServiceSlotUnit u) {
        return ServiceSlotUnitResponse.builder()
                .id(u.getId())
                .unitNumber(u.getUnitNumber())
                .capacity(u.getCapacity())
                .bookedCount(u.getBookedCount())
                .availableCapacity(u.getAvailableCapacity())
                .build();
    }
}
