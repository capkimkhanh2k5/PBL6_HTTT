package com.danasea.backend.modules.booking.application.usecases;

import java.time.OffsetDateTime;
import java.util.List;

import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemResult;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldResult;
import com.danasea.backend.modules.booking.application.dtos.ConfirmBookingCommand;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.InventoryLockItem;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;
import com.danasea.backend.modules.booking.domain.ports.ServiceSlotPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfirmBookingUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final InventoryLockPort inventoryLockPort;
    private final ServiceSlotPort serviceSlotPort;

    public BookingHoldResult execute(ConfirmBookingCommand command) {
        if (command == null || command.bookingId() == null) {
            throw new IllegalArgumentException("Booking ID cannot be null");
        }
        if (command.customerId() == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

        booking.validateOwner(command.customerId());

        OffsetDateTime now = OffsetDateTime.now();
        booking.confirm(now);

        // Deduct/commit capacity into PostgreSQL service_slots
        serviceSlotPort.commitCapacityBatch(booking.getItems());

        // Release temporary hold from Redis cache
        List<InventoryLockItem> lockItems = booking.getItems().stream()
                .map(item -> InventoryLockItem.of(item.getSlotId(), item.getQuantity(), 0))
                .toList();
        inventoryLockPort.releaseHolds(booking.getId(), lockItems);

        Booking savedBooking = bookingRepository.save(booking);
        return mapToResult(savedBooking);
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
