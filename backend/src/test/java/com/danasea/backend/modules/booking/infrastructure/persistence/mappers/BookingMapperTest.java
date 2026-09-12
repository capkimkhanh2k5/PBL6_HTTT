package com.danasea.backend.modules.booking.infrastructure.persistence.mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.infrastructure.persistence.entities.BookingJpaEntity;

import static org.assertj.core.api.Assertions.assertThat;

class BookingMapperTest {

    private final BookingMapper mapper = new BookingMapper();

    @Test
    @DisplayName("Should convert domain Booking to JPA entity and back with all fields preserved")
    void testTwoWayMapping() {
        UUID bookingId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        BookingItem item = BookingItem.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .serviceId(serviceId)
                .vendorId(vendorId)
                .slotId(slotId)
                .quantity(2)
                .bookingDate(LocalDate.now().plusDays(2))
                .bookingTime(LocalTime.of(10, 0))
                .price(new BigDecimal("120.00"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        Booking domain = Booking.builder()
                .id(bookingId)
                .customerId(customerId)
                .status(BookingStatus.HOLD)
                .totalAmount(new BigDecimal("240.00"))
                .holdExpiresAt(now.plusMinutes(15))
                .items(List.of(item))
                .createdAt(now)
                .updatedAt(now)
                .build();

        BookingJpaEntity entity = mapper.toEntity(domain);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(bookingId);
        assertThat(entity.getCustomerId()).isEqualTo(customerId);
        assertThat(entity.getStatus()).isEqualTo(BookingStatus.HOLD);
        assertThat(entity.getTotalAmount()).isEqualByComparingTo(new BigDecimal("240.00"));
        assertThat(entity.getItems()).hasSize(1);
        assertThat(entity.getItems().get(0).getSlotId()).isEqualTo(slotId);
        assertThat(entity.getItems().get(0).getBooking()).isSameAs(entity);

        Booking mappedBack = mapper.toDomain(entity);

        assertThat(mappedBack).isNotNull();
        assertThat(mappedBack.getId()).isEqualTo(bookingId);
        assertThat(mappedBack.getCustomerId()).isEqualTo(customerId);
        assertThat(mappedBack.getStatus()).isEqualTo(BookingStatus.HOLD);
        assertThat(mappedBack.getTotalAmount()).isEqualByComparingTo(new BigDecimal("240.00"));
        assertThat(mappedBack.getItems()).hasSize(1);
        assertThat(mappedBack.getItems().get(0).getSlotId()).isEqualTo(slotId);
    }
}
