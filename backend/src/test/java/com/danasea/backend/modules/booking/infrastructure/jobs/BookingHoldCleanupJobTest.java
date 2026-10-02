package com.danasea.backend.modules.booking.infrastructure.jobs;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.booking.domain.events.BookingHoldExpiredEvent;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.context.ApplicationEventPublisher;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingHoldCleanupJobTest {

    @Mock
    private BookingRepositoryPort bookingRepository;

    @Mock
    private InventoryLockPort inventoryLockPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BookingHoldCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        cleanupJob = new BookingHoldCleanupJob(bookingRepository, inventoryLockPort, eventPublisher);
    }

    private Booking createExpiredBooking(UUID bookingId) {
        BookingItem item = BookingItem.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .slotId(UUID.randomUUID())
                .serviceId(UUID.randomUUID())
                .vendorId(UUID.randomUUID())
                .quantity(1)
                .bookingDate(LocalDate.now())
                .bookingTime(LocalTime.of(9, 0))
                .price(BigDecimal.valueOf(50_000))
                .build();

        Booking booking = Booking.createHold(
                bookingId,
                UUID.randomUUID(),
                List.of(item),
                Duration.ofMinutes(15),
                OffsetDateTime.now().minusMinutes(30)
        );
        return booking;
    }

    @Test
    @DisplayName("Scheduled job cleans up all expired holds and releases Redis locks")
    void cleanExpiredHolds_WhenExpiredHoldsExist_ShouldCancelAndReleaseLocks() {
        Booking b1 = createExpiredBooking(UUID.randomUUID());
        Booking b2 = createExpiredBooking(UUID.randomUUID());

        when(bookingRepository.findExpiredHolds(any(OffsetDateTime.class))).thenReturn(List.of(b1, b2));

        cleanupJob.cleanExpiredHolds();

        assertThat(b1.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(b2.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        verify(inventoryLockPort, times(1)).releaseHolds(eq(b1.getId()), any());
        verify(inventoryLockPort, times(1)).releaseHolds(eq(b2.getId()), any());
        verify(bookingRepository, times(2)).save(any(Booking.class));
        verify(eventPublisher, times(2)).publishEvent(any(BookingHoldExpiredEvent.class));
    }

    @Test
    @DisplayName("Scheduled job does nothing when there are no expired holds")
    void cleanExpiredHolds_WhenNoExpiredHolds_ShouldDoNothing() {
        when(bookingRepository.findExpiredHolds(any(OffsetDateTime.class))).thenReturn(List.of());

        cleanupJob.cleanExpiredHolds();

        verify(inventoryLockPort, never()).releaseHolds(any(), any());
        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(BookingHoldExpiredEvent.class));
    }

    @Test
    @DisplayName("Scheduled job continues processing subsequent holds if one fails")
    void cleanExpiredHolds_WhenOneFails_ShouldContinueWithRemaining() {
        Booking b1 = createExpiredBooking(UUID.randomUUID());
        Booking b2 = createExpiredBooking(UUID.randomUUID());

        when(bookingRepository.findExpiredHolds(any(OffsetDateTime.class))).thenReturn(List.of(b1, b2));
        when(bookingRepository.save(b1)).thenThrow(new RuntimeException("DB error on b1"));

        cleanupJob.cleanExpiredHolds();

        assertThat(b2.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(inventoryLockPort, times(1)).releaseHolds(eq(b2.getId()), any());
        verify(bookingRepository, times(1)).save(b2);
    }
}
