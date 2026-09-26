package com.danasea.backend.modules.order.infrastructure.adapters;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingRepository;
import com.danasea.backend.modules.order.domain.ports.BookingLookupPort;
import com.danasea.backend.modules.order.domain.ports.BookingOrderView;

@Component
public class BookingLookupAdapter implements BookingLookupPort {

    private final JpaBookingRepository jpaBookingRepository;

    public BookingLookupAdapter(JpaBookingRepository jpaBookingRepository) {
        this.jpaBookingRepository = jpaBookingRepository;
    }

    @Override
    public Optional<BookingOrderView> findBookingForOrder(UUID bookingId) {
        return jpaBookingRepository.findByIdWithItems(bookingId).map(this::toView);
    }

    private BookingOrderView toView(BookingJpaEntity entity) {
        List<BookingOrderView.BookingItemOrderView> items = entity.getItems() != null
                ? entity.getItems().stream()
                        .map(item -> new BookingOrderView.BookingItemOrderView(
                                item.getId(),
                                item.getVendorId(),
                                item.getServiceId(),
                                item.getSlotId(),
                                item.getQuantity(),
                                item.getPrice()
                        ))
                        .toList()
                : Collections.emptyList();

        return new BookingOrderView(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getTotalAmount(),
                entity.getHoldExpiresAt(),
                items
        );
    }
}
