package com.danasea.backend.modules.booking.domain.events;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain Event phát ra khi một booking hold bị hết hạn và hệ thống nhả lock thành công.
 */
public record BookingHoldExpiredEvent(
        UUID bookingId,
        OffsetDateTime expiredAt
) {
    public BookingHoldExpiredEvent(UUID bookingId) {
        this(bookingId, OffsetDateTime.now());
    }
}
