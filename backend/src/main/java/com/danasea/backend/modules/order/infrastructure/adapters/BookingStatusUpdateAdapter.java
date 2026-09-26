package com.danasea.backend.modules.order.infrastructure.adapters;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.booking.application.dtos.ConfirmBookingCommand;
import com.danasea.backend.modules.booking.application.usecases.ConfirmBookingUseCase;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingRepository;
import com.danasea.backend.modules.order.domain.ports.BookingStatusUpdatePort;

@Component
public class BookingStatusUpdateAdapter implements BookingStatusUpdatePort {

    private final JpaBookingRepository jpaBookingRepository;
    private final ConfirmBookingUseCase confirmBookingUseCase;

    public BookingStatusUpdateAdapter(
            JpaBookingRepository jpaBookingRepository,
            ConfirmBookingUseCase confirmBookingUseCase) {
        this.jpaBookingRepository = jpaBookingRepository;
        this.confirmBookingUseCase = confirmBookingUseCase;
    }

    /**
     * Tham gia cùng transaction hiện tại (Mục 2.5 — cấm REQUIRES_NEW).
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateStatusToPendingPayment(UUID bookingId) {
        jpaBookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
            jpaBookingRepository.save(booking);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void confirmBooking(UUID bookingId, UUID customerId) {
        confirmBookingUseCase.execute(new ConfirmBookingCommand(bookingId, customerId));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelBooking(UUID bookingId) {
        jpaBookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.setStatus(BookingStatus.CANCELLED);
            jpaBookingRepository.save(booking);
        });
    }
}
