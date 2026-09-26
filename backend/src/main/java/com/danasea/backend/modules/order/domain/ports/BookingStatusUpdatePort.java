package com.danasea.backend.modules.order.domain.ports;

import java.util.UUID;

/**
 * Domain Port cập nhật trạng thái Booking từ Order module (Mục 2.5 & 9.2.1).
 * Toàn bộ phương thức phải tham gia vào transaction hiện tại (Propagation.REQUIRED).
 */
public interface BookingStatusUpdatePort {

    void updateStatusToPendingPayment(UUID bookingId);

    void confirmBooking(UUID bookingId, UUID customerId);

    void cancelBooking(UUID bookingId);
}
