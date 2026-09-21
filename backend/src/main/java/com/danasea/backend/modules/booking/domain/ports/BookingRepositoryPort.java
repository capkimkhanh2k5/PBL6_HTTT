package com.danasea.backend.modules.booking.domain.ports;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.PagedResult;

public interface BookingRepositoryPort {

    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<Booking> findByIdWithItems(UUID id);

    PagedResult<Booking> findCustomerBookings(
            UUID customerId,
            BookingStatus status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    PagedResult<BookingItem> findVendorBookingItems(
            UUID vendorId,
            BookingStatus status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    List<Booking> findExpiredHolds(OffsetDateTime threshold);
}
