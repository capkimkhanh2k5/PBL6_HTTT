package com.danasea.backend.modules.booking.infrastructure.persistence.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;

@Component
public class BookingMapper {

    public BookingJpaEntity toEntity(Booking domain) {
        if (domain == null) {
            return null;
        }

        BookingJpaEntity entity = BookingJpaEntity.builder()
                .customerId(domain.getCustomerId())
                .status(domain.getStatus())
                .totalAmount(domain.getTotalAmount())
                .holdExpiresAt(domain.getHoldExpiresAt())
                .items(new ArrayList<>())
                .build();

        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        if (domain.getItems() != null) {
            for (BookingItem item : domain.getItems()) {
                BookingItemJpaEntity itemEntity = BookingItemJpaEntity.builder()
                        .serviceId(item.getServiceId())
                        .vendorId(item.getVendorId())
                        .slotId(item.getSlotId())
                        .quantity(item.getQuantity())
                        .bookingDate(item.getBookingDate())
                        .bookingTime(item.getBookingTime())
                        .price(item.getPrice())
                        .build();

                itemEntity.setId(item.getId() != null ? item.getId() : UUID.randomUUID());
                itemEntity.setCreatedAt(item.getCreatedAt() != null ? item.getCreatedAt() : domain.getCreatedAt());
                itemEntity.setUpdatedAt(item.getUpdatedAt() != null ? item.getUpdatedAt() : domain.getUpdatedAt());

                entity.addItem(itemEntity);
            }
        }

        return entity;
    }

    public BookingItem toDomainItem(BookingItemJpaEntity itemEntity) {
        if (itemEntity == null) {
            return null;
        }
        BookingStatus status = null;
        if (itemEntity.getBooking() != null) {
            status = itemEntity.getBooking().getStatus();
        }

        return BookingItem.builder()
                .id(itemEntity.getId())
                .bookingId(itemEntity.getBooking() != null ? itemEntity.getBooking().getId() : null)
                .serviceId(itemEntity.getServiceId())
                .vendorId(itemEntity.getVendorId())
                .slotId(itemEntity.getSlotId())
                .quantity(itemEntity.getQuantity())
                .bookingDate(itemEntity.getBookingDate())
                .bookingTime(itemEntity.getBookingTime())
                .price(itemEntity.getPrice())
                .bookingStatus(status)
                .createdAt(itemEntity.getCreatedAt())
                .updatedAt(itemEntity.getUpdatedAt())
                .build();
    }

    public Booking toDomain(BookingJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<BookingItem> items = new ArrayList<>();
        if (entity.getItems() != null) {
            for (BookingItemJpaEntity itemEntity : entity.getItems()) {
                BookingItem item = toDomainItem(itemEntity);
                if (item != null) {
                    if (item.getBookingId() == null) {
                        item.setBookingId(entity.getId());
                    }
                    items.add(item);
                }
            }
        }

        return Booking.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .holdExpiresAt(entity.getHoldExpiresAt())
                .items(items)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
