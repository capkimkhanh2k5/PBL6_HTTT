package com.danasea.backend.modules.booking.application.usecases;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemDto;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemResult;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldResult;
import com.danasea.backend.modules.booking.application.dtos.CreateBookingHoldCommand;
import com.danasea.backend.modules.booking.domain.exceptions.SlotNotAvailableException;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingItemAllocation;
import com.danasea.backend.modules.booking.domain.models.InventoryLockItem;
import com.danasea.backend.modules.booking.domain.models.SlotValidationDetails;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;
import com.danasea.backend.modules.booking.domain.ports.ServiceSlotPort;
import com.danasea.backend.modules.service.domain.models.InventoryType;
import com.danasea.backend.modules.service.domain.models.OptionType;
import com.danasea.backend.modules.service.domain.models.PricingUnit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateBookingHoldUseCase {

    public static final Duration HOLD_DURATION = Duration.ofMinutes(15);
    private static final int MAX_REQUEST_ITEMS = 20;
    private static final int MAX_TOTAL_QUANTITY = 100;

    private final BookingRepositoryPort bookingRepository;
    private final InventoryLockPort inventoryLockPort;
    private final ServiceSlotPort serviceSlotPort;

    private record AggregationKey(UUID slotId, UUID optionId, Integer participantsCount) {}

    public BookingHoldResult execute(CreateBookingHoldCommand command) {
        // 1. Validate basic input
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (command.customerId() == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new IllegalArgumentException("Booking must contain at least one item");
        }
        if (command.items().size() > MAX_REQUEST_ITEMS) {
            throw new IllegalArgumentException("Booking must contain at most " + MAX_REQUEST_ITEMS + " items");
        }

        // 2. Aggregate quantities by (slotId, optionId, participantsCount)
        Map<AggregationKey, Integer> aggregatedQuantities = new LinkedHashMap<>();
        int totalQuantity = 0;
        for (BookingHoldItemDto item : command.items()) {
            if (item.slotId() == null) {
                throw new IllegalArgumentException("slotId cannot be null");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            try {
                totalQuantity = Math.addExact(totalQuantity, item.quantity());
                AggregationKey key = new AggregationKey(item.slotId(), item.optionId(), item.participantsCount());
                aggregatedQuantities.merge(key, item.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Total booking quantity is too large", exception);
            }
            if (totalQuantity > MAX_TOTAL_QUANTITY) {
                throw new IllegalArgumentException(
                        "Total booking quantity must not exceed " + MAX_TOTAL_QUANTITY);
            }
        }

        // 3. Query slot and service details to validate business rules
        List<UUID> slotIds = aggregatedQuantities.keySet().stream()
                .map(AggregationKey::slotId)
                .distinct()
                .toList();

        List<SlotValidationDetails> slotDetailsList = serviceSlotPort.findSlotDetailsBatch(slotIds);
        Map<UUID, SlotValidationDetails> slotDetailsMap = slotDetailsList.stream()
                .collect(Collectors.toMap(SlotValidationDetails::getSlotId, Function.identity()));

        LocalDate today = LocalDate.now();
        for (Map.Entry<AggregationKey, Integer> entry : aggregatedQuantities.entrySet()) {
            UUID slotId = entry.getKey().slotId();
            SlotValidationDetails details = slotDetailsMap.get(slotId);
            if (details == null) {
                throw new SlotNotAvailableException(slotId, "Slot does not exist");
            }
            if (!details.isServicePublished()) {
                throw new SlotNotAvailableException(slotId, "Associated service is not published");
            }
            if (!details.isOpen()) {
                throw new SlotNotAvailableException(slotId, "Slot status is " + details.getStatus());
            }
            if (details.getBookingDate().isBefore(today)) {
                throw new SlotNotAvailableException(slotId, "Cannot book slots in the past");
            }
        }

        // 4. Build lock items for Redis batch Lua script
        List<InventoryLockItem> lockItems = new ArrayList<>();
        List<AggregationKey> orderedKeys = new ArrayList<>(aggregatedQuantities.keySet());

        for (AggregationKey key : orderedKeys) {
            int qty = aggregatedQuantities.get(key);
            SlotValidationDetails details = slotDetailsMap.get(key.slotId());
            SlotValidationDetails.ServiceOptionValidationDetails option = null;

            if (key.optionId() != null) {
                option = details.getOptions().get(key.optionId());
                if (option == null || !option.isActive()) {
                    throw new IllegalArgumentException("Option is invalid or no longer active: " + key.optionId());
                }
                if (option.isPrivate()) {
                    if (key.participantsCount() != null && option.getMaxPaxPerPackage() != null) {
                        if (key.participantsCount() > option.getMaxPaxPerPackage()) {
                            throw new IllegalArgumentException("Number of guests in package (" + key.participantsCount()
                                    + ") exceeds maximum allowed limit (" + option.getMaxPaxPerPackage() + ")");
                        }
                    }
                }
            } else if (!details.getOptions().isEmpty()) {
                // Backward compatible fallback
                option = details.getOptions().values().iterator().next();
            }

            if (InventoryType.SHARED_CAPACITY_UNITS.equals(details.getInventoryType())) {
                List<InventoryLockItem.UnitLockInfo> unitInfos = details.getUnits().stream()
                        .map(u -> new InventoryLockItem.UnitLockInfo(u.getUnitNumber(), u.getCapacity(), u.getBookedCount()))
                        .toList();

                OptionType optType = option != null ? option.getOptionType() : OptionType.SHARED;
                Integer pax = key.participantsCount();
                if (pax == null && option != null && option.getMaxPaxPerPackage() != null) {
                    pax = option.getMaxPaxPerPackage();
                }

                lockItems.add(InventoryLockItem.builder()
                        .slotId(key.slotId())
                        .quantity(qty)
                        .maxCapacity(details.getCapacity())
                        .inventoryType(InventoryType.SHARED_CAPACITY_UNITS)
                        .optionType(optType)
                        .paxPerPackage(pax != null ? pax : 1)
                        .units(unitInfos)
                        .allocations(new ArrayList<>())
                        .build());
            } else {
                // PERSON_LIMIT
                lockItems.add(InventoryLockItem.builder()
                        .slotId(key.slotId())
                        .quantity(qty)
                        .maxCapacity(details.getAvailableCapacity())
                        .inventoryType(InventoryType.PERSON_LIMIT)
                        .optionType(option != null ? option.getOptionType() : OptionType.SHARED)
                        .paxPerPackage(key.participantsCount())
                        .units(new ArrayList<>())
                        .allocations(new ArrayList<>())
                        .build());
            }
        }

        UUID bookingId = UUID.randomUUID();

        // 5. Atomic Redis inventory lock (All-or-Nothing)
        inventoryLockPort.acquireHolds(bookingId, lockItems, HOLD_DURATION);

        // 6. Build Booking & BookingItems and persist to DB with compensating rollback on failure
        try {
            List<BookingItem> bookingItems = new ArrayList<>();
            for (int i = 0; i < orderedKeys.size(); i++) {
                AggregationKey key = orderedKeys.get(i);
                int qty = aggregatedQuantities.get(key);
                InventoryLockItem lockItem = lockItems.get(i);
                SlotValidationDetails details = slotDetailsMap.get(key.slotId());
                SlotValidationDetails.ServiceOptionValidationDetails option = null;

                if (key.optionId() != null) {
                    option = details.getOptions().get(key.optionId());
                } else if (!details.getOptions().isEmpty()) {
                    option = details.getOptions().values().iterator().next();
                }

                BigDecimal itemPrice = (option != null && option.getPrice() != null) ? option.getPrice() : details.getPrice();
                PricingUnit pricingUnit = option != null ? option.getPricingUnit() : PricingUnit.PER_PERSON;
                UUID optId = option != null ? option.getId() : null;

                UUID bookingItemId = UUID.randomUUID();
                List<BookingItemAllocation> itemAllocations = new ArrayList<>();
                if (lockItem.getAllocations() != null) {
                    for (BookingItemAllocation alloc : lockItem.getAllocations()) {
                        alloc.setBookingItemId(bookingItemId);
                        itemAllocations.add(alloc);
                    }
                }

                bookingItems.add(BookingItem.builder()
                        .id(bookingItemId)
                        .bookingId(bookingId)
                        .slotId(key.slotId())
                        .serviceId(details.getServiceId())
                        .vendorId(details.getVendorId())
                        .optionId(optId)
                        .pricingUnit(pricingUnit)
                        .participantsCount(key.participantsCount())
                        .quantity(qty)
                        .bookingDate(details.getBookingDate())
                        .bookingTime(details.getBookingTime())
                        .price(itemPrice)
                        .allocations(itemAllocations)
                        .build());
            }

            OffsetDateTime now = OffsetDateTime.now();
            Booking booking = Booking.createHold(bookingId, command.customerId(), bookingItems, HOLD_DURATION, now);
            Booking savedBooking = bookingRepository.save(booking);

            return mapToResult(savedBooking);
        } catch (Exception ex) {
            // Compensating action: Release Redis holds immediately if database persistence fails
            inventoryLockPort.releaseHolds(bookingId, lockItems);
            throw ex;
        }
    }

    private BookingHoldResult mapToResult(Booking booking) {
        List<BookingHoldItemResult> itemResults = booking.getItems().stream()
                .map(item -> new BookingHoldItemResult(
                        item.getId(),
                        item.getServiceId(),
                        item.getVendorId(),
                        item.getSlotId(),
                        item.getQuantity(),
                        item.getBookingDate(),
                        item.getBookingTime(),
                        item.getPrice(),
                        item.calculateSubtotal()
                ))
                .toList();

        return new BookingHoldResult(
                booking.getId(),
                booking.getCustomerId(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getHoldExpiresAt(),
                itemResults,
                booking.getCreatedAt()
        );
    }
}
