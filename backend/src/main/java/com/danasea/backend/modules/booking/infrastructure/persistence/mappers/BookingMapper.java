package com.danasea.backend.modules.booking.infrastructure.persistence.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingItemAllocation;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemAllocationJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingItemJpaEntity;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;
import com.danasea.backend.modules.service.domain.models.PricingUnit;

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
                        .optionId(item.getOptionId())
                        .pricingUnit(item.getPricingUnit() != null ? item.getPricingUnit().name() : null)
                        .participantsCount(item.getParticipantsCount())
                        .quantity(item.getQuantity())
                        .bookingDate(item.getBookingDate())
                        .bookingTime(item.getBookingTime())
                        .price(item.getPrice())
                        .allocations(new ArrayList<>())
                        .build();

                itemEntity.setId(item.getId() != null ? item.getId() : UUID.randomUUID());
                itemEntity.setCreatedAt(item.getCreatedAt() != null ? item.getCreatedAt() : domain.getCreatedAt());
                itemEntity.setUpdatedAt(item.getUpdatedAt() != null ? item.getUpdatedAt() : domain.getUpdatedAt());

                if (item.getAllocations() != null) {
                    for (BookingItemAllocation alloc : item.getAllocations()) {
                        BookingItemAllocationJpaEntity allocEntity = BookingItemAllocationJpaEntity.builder()
                                .slotId(alloc.getSlotId() != null ? alloc.getSlotId() : item.getSlotId())
                                .unitNumber(alloc.getUnitNumber())
                                .allocatedSeats(alloc.getAllocatedSeats())
                                .isPrivateLock(alloc.getIsPrivateLock() != null ? alloc.getIsPrivateLock() : false)
                                .build();
                        allocEntity.setId(alloc.getId() != null ? alloc.getId() : UUID.randomUUID());
                        allocEntity.setCreatedAt(alloc.getCreatedAt() != null ? alloc.getCreatedAt() : itemEntity.getCreatedAt());
                        allocEntity.setUpdatedAt(alloc.getUpdatedAt() != null ? alloc.getUpdatedAt() : itemEntity.getUpdatedAt());
                        itemEntity.addAllocation(allocEntity);
                    }
                }

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

        PricingUnit pUnit = null;
        if (itemEntity.getPricingUnit() != null) {
            try {
                pUnit = PricingUnit.valueOf(itemEntity.getPricingUnit());
            } catch (Exception ignored) {
            }
        }

        List<BookingItemAllocation> allocations = new ArrayList<>();
        if (itemEntity.getAllocations() != null) {
            for (BookingItemAllocationJpaEntity aEntity : itemEntity.getAllocations()) {
                BookingItemAllocation alloc = BookingItemAllocation.builder()
                        .bookingItemId(itemEntity.getId())
                        .slotId(aEntity.getSlotId())
                        .unitNumber(aEntity.getUnitNumber())
                        .allocatedSeats(aEntity.getAllocatedSeats())
                        .isPrivateLock(aEntity.getIsPrivateLock())
                        .build();
                alloc.setId(aEntity.getId());
                alloc.setCreatedAt(aEntity.getCreatedAt());
                alloc.setUpdatedAt(aEntity.getUpdatedAt());
                allocations.add(alloc);
            }
        }

        return BookingItem.builder()
                .id(itemEntity.getId())
                .bookingId(itemEntity.getBooking() != null ? itemEntity.getBooking().getId() : null)
                .serviceId(itemEntity.getServiceId())
                .vendorId(itemEntity.getVendorId())
                .slotId(itemEntity.getSlotId())
                .optionId(itemEntity.getOptionId())
                .pricingUnit(pUnit)
                .participantsCount(itemEntity.getParticipantsCount())
                .quantity(itemEntity.getQuantity())
                .bookingDate(itemEntity.getBookingDate())
                .bookingTime(itemEntity.getBookingTime())
                .price(itemEntity.getPrice())
                .bookingStatus(status)
                .allocations(allocations)
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
