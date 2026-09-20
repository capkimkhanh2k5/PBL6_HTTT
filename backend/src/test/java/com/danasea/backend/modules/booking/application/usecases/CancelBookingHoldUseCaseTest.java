package com.danasea.backend.modules.booking.application.usecases;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.booking.application.dtos.BookingHoldResult;
import com.danasea.backend.modules.booking.application.dtos.CancelBookingHoldCommand;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.exceptions.InvalidBookingStateException;
import com.danasea.backend.modules.booking.domain.exceptions.UnauthorizedBookingAccessException;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.InventoryLockItem;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelBookingHoldUseCaseTest {

    @Mock
    private BookingRepositoryPort bookingRepository;

    @Mock
    private InventoryLockPort inventoryLockPort;

    private CancelBookingHoldUseCase cancelBookingHoldUseCase;

    private UUID customerId;
    private UUID bookingId;
    private UUID slotId;
    private Booking sampleHoldBooking;

    @BeforeEach
    void setUp() {
        cancelBookingHoldUseCase = new CancelBookingHoldUseCase(bookingRepository, inventoryLockPort);

        customerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        slotId = UUID.randomUUID();

        BookingItem item = BookingItem.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .slotId(slotId)
                .serviceId(UUID.randomUUID())
                .vendorId(UUID.randomUUID())
                .quantity(1)
                .bookingDate(LocalDate.now().plusDays(2))
                .bookingTime(LocalTime.of(14, 0))
                .price(BigDecimal.valueOf(150_000))
                .build();

        sampleHoldBooking = Booking.createHold(
                bookingId,
                customerId,
                List.of(item),
                Duration.ofMinutes(15),
                OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("Cancel hold actively changes status to CANCELLED and releases Redis holds")
    void execute_WhenValidHold_ShouldCancelSuccessfully() {
        CancelBookingHoldCommand command = new CancelBookingHoldCommand(bookingId, customerId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleHoldBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingHoldResult result = cancelBookingHoldUseCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);

        ArgumentCaptor<List<InventoryLockItem>> lockItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryLockPort).releaseHolds(eq(bookingId), lockItemsCaptor.capture());
        assertThat(lockItemsCaptor.getValue()).hasSize(1);
        assertThat(lockItemsCaptor.getValue().get(0).getSlotId()).isEqualTo(slotId);

        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Cancel hold when booking is already CANCELLED is idempotent")
    void execute_WhenAlreadyCancelled_ShouldReturnWithoutError() {
        sampleHoldBooking.setStatus(BookingStatus.CANCELLED);
        CancelBookingHoldCommand command = new CancelBookingHoldCommand(bookingId, customerId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleHoldBooking));

        BookingHoldResult result = cancelBookingHoldUseCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(inventoryLockPort, never()).releaseHolds(any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel hold by different customer throws UnauthorizedBookingAccessException")
    void execute_WhenDifferentCustomer_ShouldThrowException() {
        UUID otherCustomerId = UUID.randomUUID();
        CancelBookingHoldCommand command = new CancelBookingHoldCommand(bookingId, otherCustomerId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleHoldBooking));

        assertThatThrownBy(() -> cancelBookingHoldUseCase.execute(command))
                .isInstanceOf(UnauthorizedBookingAccessException.class);

        verify(inventoryLockPort, never()).releaseHolds(any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel non-existent booking throws BookingNotFoundException")
    void execute_WhenBookingNotFound_ShouldThrowException() {
        CancelBookingHoldCommand command = new CancelBookingHoldCommand(bookingId, customerId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cancelBookingHoldUseCase.execute(command))
                .isInstanceOf(BookingNotFoundException.class);

        verify(inventoryLockPort, never()).releaseHolds(any(), any());
    }

    @Test
    @DisplayName("Cancel hold when booking is in CONFIRMED state throws InvalidBookingStateException")
    void execute_WhenStatusIsConfirmed_ShouldThrowException() {
        sampleHoldBooking.setStatus(BookingStatus.CONFIRMED);
        CancelBookingHoldCommand command = new CancelBookingHoldCommand(bookingId, customerId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleHoldBooking));

        assertThatThrownBy(() -> cancelBookingHoldUseCase.execute(command))
                .isInstanceOf(InvalidBookingStateException.class);

        verify(inventoryLockPort, never()).releaseHolds(any(), any());
        verify(bookingRepository, never()).save(any());
    }
}
