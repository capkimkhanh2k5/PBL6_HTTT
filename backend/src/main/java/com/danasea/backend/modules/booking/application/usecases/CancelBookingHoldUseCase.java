package com.danasea.backend.modules.booking.application.usecases;

import java.time.OffsetDateTime;
import java.util.List;

import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemResult;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldResult;
import com.danasea.backend.modules.booking.application.dtos.CancelBookingHoldCommand;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.exceptions.InvalidBookingStateException;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.InventoryLockItem;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CancelBookingHoldUseCase {

    private final BookingRepositoryPort bookingRepository;
    private final InventoryLockPort inventoryLockPort;

    public BookingHoldResult execute(CancelBookingHoldCommand command) {
        if (command == null || command.bookingId() == null) {
            throw new IllegalArgumentException("Booking ID cannot be null");
        }

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

        if (command.customerId() != null) {
            booking.validateOwner(command.customerId());
        }

        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            return mapToResult(booking);
        }

        if (!BookingStatus.HOLD.equals(booking.getStatus())) {
            throw new InvalidBookingStateException("Cannot cancel hold in status " + booking.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        booking.cancel(now);

        List<InventoryLockItem> lockItems = booking.getItems().stream()
                .map(item -> InventoryLockItem.of(item.getSlotId(), item.getQuantity(), 0))
                .toList();
        inventoryLockPort.releaseHolds(booking.getId(), lockItems);

        Booking saved = bookingRepository.save(booking);
        return mapToResult(saved);
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
