package com.danasea.backend.modules.order.domain.ports;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain Port tra cứu thông tin Booking để khởi tạo Order (Mục 2 & 9.2.1).
 */
public interface BookingLookupPort {

    Optional<BookingOrderView> findBookingForOrder(UUID bookingId);
}
