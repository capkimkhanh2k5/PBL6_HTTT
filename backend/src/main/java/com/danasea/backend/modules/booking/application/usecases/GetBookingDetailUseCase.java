package com.danasea.backend.modules.booking.application.usecases;

import java.util.List;

import com.danasea.backend.modules.booking.application.dtos.BookingDetailResult;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemResult;
import com.danasea.backend.modules.booking.application.dtos.GetBookingDetailQuery;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetBookingDetailUseCase {

    private final BookingRepositoryPort bookingRepository;

    public BookingDetailResult execute(GetBookingDetailQuery query) {
        if (query == null || query.bookingId() == null) {
            throw new IllegalArgumentException("Booking ID cannot be null");
        }

        Booking booking = bookingRepository.findByIdWithItems(query.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(query.bookingId()));

        // Chống IDOR triệt để: kiểm tra quyền sở hữu (ngoại trừ ADMIN)
        if (!query.isAdmin()) {
            booking.validateOwner(query.currentUserId());
        }

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

        return new BookingDetailResult(
                booking.getId(),
                booking.getCustomerId(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getHoldExpiresAt(),
                itemResults,
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
