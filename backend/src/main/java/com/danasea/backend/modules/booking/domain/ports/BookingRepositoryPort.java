package com.danasea.backend.modules.booking.domain.ports;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.models.Booking;

public interface BookingRepositoryPort {

    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByIdAndCustomerId(UUID id, UUID customerId);

    List<Booking> findExpiredHolds(OffsetDateTime threshold);
}
