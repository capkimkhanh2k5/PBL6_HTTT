package com.danasea.backend.modules.order.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.booking.domain.ports.BookingPaymentStatusPort;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaPaymentRepository;

@Component
public class BookingPaymentStatusAdapter implements BookingPaymentStatusPort {

    private final JpaMasterOrderRepository masterOrderRepository;
    private final JpaPaymentRepository paymentRepository;

    public BookingPaymentStatusAdapter(
            JpaMasterOrderRepository masterOrderRepository,
            JpaPaymentRepository paymentRepository) {
        this.masterOrderRepository = masterOrderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public boolean hasSuccessfulPayment(UUID bookingId) {
        return masterOrderRepository.findByBookingId(bookingId)
                .map(order -> paymentRepository.existsByMasterOrderIdAndStatus(order.getId(), PaymentStatus.SUCCESS))
                .orElse(false);
    }
}
