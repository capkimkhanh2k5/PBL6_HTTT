package com.danasea.backend.modules.booking.application.usecases;

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
import com.danasea.backend.modules.booking.domain.models.InventoryLockItem;
import com.danasea.backend.modules.booking.domain.models.SlotValidationDetails;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;
import com.danasea.backend.modules.booking.domain.ports.ServiceSlotPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateBookingHoldUseCase {

    public static final Duration HOLD_DURATION = Duration.ofMinutes(15);

    private final BookingRepositoryPort bookingRepository;
    private final InventoryLockPort inventoryLockPort;
    private final ServiceSlotPort serviceSlotPort;

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

        // 2. Aggregate quantities by slotId to avoid duplicate entries in lock requests
        Map<UUID, Integer> aggregatedSlotQuantities = new LinkedHashMap<>();
        for (BookingHoldItemDto item : command.items()) {
            if (item.slotId() == null) {
                throw new IllegalArgumentException("slotId cannot be null");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            aggregatedSlotQuantities.merge(item.slotId(), item.quantity(), Integer::sum);
        }

        // 3. Query slot and service details to validate business rules
        List<UUID> slotIds = new ArrayList<>(aggregatedSlotQuantities.keySet());
        List<SlotValidationDetails> slotDetailsList = serviceSlotPort.findSlotDetailsBatch(slotIds);
        Map<UUID, SlotValidationDetails> slotDetailsMap = slotDetailsList.stream()
                .collect(Collectors.toMap(SlotValidationDetails::getSlotId, Function.identity()));

        LocalDate today = LocalDate.now();
        for (Map.Entry<UUID, Integer> entry : aggregatedSlotQuantities.entrySet()) {
            UUID slotId = entry.getKey();
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
        List<InventoryLockItem> lockItems = aggregatedSlotQuantities.entrySet().stream()
                .map(e -> {
                    SlotValidationDetails details = slotDetailsMap.get(e.getKey());
                    return InventoryLockItem.of(e.getKey(), e.getValue(), details.getAvailableCapacity());
                })
                .toList();

        UUID bookingId = UUID.randomUUID();

        // 5. Atomic Redis inventory lock (All-or-Nothing)
        inventoryLockPort.acquireHolds(bookingId, lockItems, HOLD_DURATION);

        // 6. Build Booking & BookingItems and persist to DB with compensating rollback on failure
        try {
            List<BookingItem> bookingItems = new ArrayList<>();
            for (Map.Entry<UUID, Integer> entry : aggregatedSlotQuantities.entrySet()) {
                UUID slotId = entry.getKey();
                int qty = entry.getValue();
                SlotValidationDetails details = slotDetailsMap.get(slotId);

                bookingItems.add(BookingItem.builder()
                        .id(UUID.randomUUID())
                        .bookingId(bookingId)
                        .slotId(slotId)
                        .serviceId(details.getServiceId())
                        .vendorId(details.getVendorId())
                        .quantity(qty)
                        .bookingDate(details.getBookingDate())
                        .bookingTime(details.getBookingTime())
                        .price(details.getPrice())
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
