package com.danasea.backend.modules.service.application.usecases;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.InventoryType;
import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceOption;
import com.danasea.backend.modules.service.domain.models.ServiceSlot;
import com.danasea.backend.modules.service.domain.models.ServiceSlotUnit;
import com.danasea.backend.modules.service.domain.models.SlotStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceOptionRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceSlotRepositoryPort;
import com.danasea.backend.modules.service.presentation.dtos.PublicServiceSlotAvailabilityResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetServiceSlotsAvailabilityUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ServiceOptionRepositoryPort serviceOptionRepository;
    private final ServiceSlotRepositoryPort serviceSlotRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public List<PublicServiceSlotAvailabilityResponse> execute(
            UUID serviceId,
            UUID optionId,
            LocalDate from,
            LocalDate to,
            Integer quantity
    ) {
        return execute(serviceId, optionId, from, to, quantity, false);
    }

    @Transactional(readOnly = true)
    public List<PublicServiceSlotAvailabilityResponse> execute(UUID serviceId, UUID optionId,
            LocalDate from, LocalDate to, Integer quantity, boolean allowSplit) {
        if (quantity != null && quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("End date must not precede start date");
        }
        Service service = serviceRepository.findPublishedById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found or not published: " + serviceId));

        ServiceOption option;
        if (optionId != null) {
            option = serviceOptionRepository.findById(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("Option not found: " + optionId));
            if (!serviceId.equals(option.getServiceId()) || !OptionStatus.ACTIVE.equals(option.getStatus())) {
                throw new IllegalArgumentException("Option does not belong to service or is no longer active");
            }
        } else {
            List<ServiceOption> activeOptions = serviceOptionRepository.findByServiceIdAndStatus(serviceId, OptionStatus.ACTIVE);
            if (activeOptions.isEmpty()) {
                return Collections.emptyList();
            }
            if (activeOptions.size() > 1) {
                throw new IllegalArgumentException("An active optionId must be selected");
            }
            option = activeOptions.get(0);
        }

        LocalDate today = LocalDate.now(Booking.VIETNAM_ZONE);
        LocalTime nowTime = LocalTime.now(Booking.VIETNAM_ZONE);

        LocalDate queryFrom = (from != null && !from.isBefore(today)) ? from : today;
        LocalDate queryTo = (to != null && !to.isBefore(queryFrom)) ? to : queryFrom.plusDays(30);

        List<ServiceSlot> slots = serviceSlotRepository.findByServiceIdAndDateBetween(serviceId, queryFrom, queryTo);
        if (slots.isEmpty()) {
            return Collections.emptyList();
        }

        List<PublicServiceSlotAvailabilityResponse> responses = new ArrayList<>();

        for (ServiceSlot slot : slots) {
            if (!SlotStatus.OPEN.equals(slot.getStatus())) {
                continue;
            }
            if (slot.getDate().isBefore(today)) {
                continue;
            }
            if (slot.getDate().isEqual(today) && slot.getStartTime() != null && !slot.getStartTime().isAfter(nowTime)) {
                continue;
            }

            int availablePaxOrPackages = calculateAvailability(slot, option);
            int reqQty = (quantity != null && quantity > 0) ? quantity : 1;
            boolean bookable = availablePaxOrPackages >= reqQty;
            if (bookable && option.isShared() && !allowSplit
                    && InventoryType.SHARED_CAPACITY_UNITS.equals(slot.getInventoryType())) {
                bookable = largestAvailableSharedUnit(slot) >= reqQty;
            }

            responses.add(PublicServiceSlotAvailabilityResponse.builder()
                    .slotId(slot.getId())
                    .serviceId(serviceId)
                    .optionId(option.getId())
                    .date(slot.getDate())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .price(option.getPrice())
                    .pricingUnit(option.getPricingUnit())
                    .status(slot.getStatus())
                    .availablePaxOrPackages(availablePaxOrPackages)
                    .maxPaxPerPackage(option.getMaxPaxPerPackage())
                    .inventoryType(slot.getInventoryType())
                    .bookable(bookable)
                    .build());
        }

        return responses;
    }

    private int calculateAvailability(ServiceSlot slot, ServiceOption option) {
        String key = "inventory:slot:" + slot.getId() + ":holds";
        Map<Object, Object> rawHolds = redisTemplate.opsForHash().entries(key);
        long now = System.currentTimeMillis();

        if (InventoryType.SHARED_CAPACITY_UNITS.equals(slot.getInventoryType())) {
            List<ServiceSlotUnit> units = slot.getUnits();
            if (units == null || units.isEmpty()) {
                units = serviceSlotRepository.findUnitsBySlotId(slot.getId());
            }

            Map<Integer, Integer> unitHeldSeats = new HashMap<>();
            Map<Integer, Boolean> unitPrivateHeld = new HashMap<>();

            if (rawHolds != null) {
                for (Object valObj : rawHolds.values()) {
                    String val = String.valueOf(valObj);
                    parseUnitHoldEntry(val, now, unitHeldSeats, unitPrivateHeld);
                }
            }

            if (option.isPrivate()) {
                int requiredCap = (option.getMaxPaxPerPackage() != null && option.getMaxPaxPerPackage() > 0)
                        ? option.getMaxPaxPerPackage()
                        : 1;

                int emptyCount = 0;
                for (ServiceSlotUnit u : units) {
                    int booked = (u.getBookedCount() != null) ? u.getBookedCount() : 0;
                    int held = unitHeldSeats.getOrDefault(u.getUnitNumber(), 0);
                    boolean priv = unitPrivateHeld.getOrDefault(u.getUnitNumber(), false);

                    if (booked == 0 && held == 0 && !priv && u.getCapacity() >= requiredCap) {
                        emptyCount++;
                    }
                }
                return emptyCount;
            } else {
                // SHARED
                int totalAvailableSeats = 0;
                for (ServiceSlotUnit u : units) {
                    boolean priv = unitPrivateHeld.getOrDefault(u.getUnitNumber(), false);
                    if (priv) {
                        continue; // Locked for private tour
                    }
                    int booked = (u.getBookedCount() != null) ? u.getBookedCount() : 0;
                    int held = unitHeldSeats.getOrDefault(u.getUnitNumber(), 0);
                    int avail = Math.max(0, u.getCapacity() - booked - held);
                    totalAvailableSeats += avail;
                }
                return totalAvailableSeats;
            }
        } else {
            // PERSON_LIMIT
            int totalActiveHoldSeats = 0;
            if (rawHolds != null) {
                for (Object valObj : rawHolds.values()) {
                    String val = String.valueOf(valObj);
                    totalActiveHoldSeats += parsePersonLimitHoldSeats(val, now);
                }
            }

            int cap = slot.getCapacity() != null ? slot.getCapacity() : 0;
            int booked = slot.getBookedCount() != null ? slot.getBookedCount() : 0;
            int avail = Math.max(0, cap - booked - totalActiveHoldSeats);

            if (option.isPrivate()) {
                return 0;
            }
            return avail;
        }
    }

    private int largestAvailableSharedUnit(ServiceSlot slot) {
        Map<Integer, Integer> held = new HashMap<>();
        Map<Integer, Boolean> privateHeld = new HashMap<>();
        redisTemplate.opsForHash().entries("inventory:slot:" + slot.getId() + ":holds").values()
                .forEach(value -> parseUnitHoldEntry(String.valueOf(value), System.currentTimeMillis(), held, privateHeld));
        List<ServiceSlotUnit> units = slot.getUnits();
        if (units == null || units.isEmpty()) {
            units = serviceSlotRepository.findUnitsBySlotId(slot.getId());
        }
        return units.stream().filter(unit -> !privateHeld.getOrDefault(unit.getUnitNumber(), false))
                .mapToInt(unit -> Math.max(0, unit.getCapacity() - (unit.getBookedCount() != null ? unit.getBookedCount() : 0)
                        - held.getOrDefault(unit.getUnitNumber(), 0)))
                .max().orElse(0);
    }

    private void parseUnitHoldEntry(
            String val,
            long now,
            Map<Integer, Integer> unitHeldSeats,
            Map<Integer, Boolean> unitPrivateHeld
    ) {
        try {
            int barPos = val.indexOf('|');
            if (barPos < 0) return;
            long exp = Long.parseLong(val.substring(0, barPos));
            if (exp <= now) return;

            String details = val.substring(barPos + 1);
            if (!details.startsWith("UNITS:")) return;

            String unitsStr = details.substring("UNITS:".length());
            String[] allocations = unitsStr.split(";");
            for (String alloc : allocations) {
                if (alloc.isBlank()) continue;
                String[] parts = alloc.split(":");
                if (parts.length >= 3) {
                    int unitNum = Integer.parseInt(parts[0]);
                    int seats = Integer.parseInt(parts[1]);
                    int priv = Integer.parseInt(parts[2]);

                    unitHeldSeats.merge(unitNum, seats, Integer::sum);
                    if (priv == 1) {
                        unitPrivateHeld.put(unitNum, true);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private int parsePersonLimitHoldSeats(String val, long now) {
        try {
            int barPos = val.indexOf('|');
            if (barPos > 0) {
                long exp = Long.parseLong(val.substring(0, barPos));
                if (exp <= now) return 0;
                String details = val.substring(barPos + 1);
                if (details.startsWith("PERSON_LIMIT:")) {
                    return Integer.parseInt(details.substring("PERSON_LIMIT:".length()));
                }
            } else {
                int colonPos = val.indexOf(':');
                if (colonPos > 0) {
                    int qty = Integer.parseInt(val.substring(0, colonPos));
                    long exp = Long.parseLong(val.substring(colonPos + 1));
                    if (exp > now) return qty;
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
