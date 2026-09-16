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
import com.danasea.backend.modules.booking.application.dtos.ConfirmBookingCommand;
import com.danasea.backend.modules.booking.domain.exceptions.BookingHoldExpiredException;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.exceptions.InsufficientInventoryException;
import com.danasea.backend.modules.booking.domain.exceptions.InvalidBookingStateException;
import com.danasea.backend.modules.booking.domain.exceptions.UnauthorizedBookingAccessException;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.InventoryLockItem;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.InventoryLockPort;
import com.danasea.backend.modules.booking.domain.ports.ServiceSlotPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmBookingUseCaseTest {

    @Mock
    private BookingRepositoryPort bookingRepository;

    @Mock
    private InventoryLockPort inventoryLockPort;

    @Mock
    private ServiceSlotPort serviceSlotPort;

    private ConfirmBookingUseCase confirmBookingUseCase;

    private UUID customerId;
    private UUID bookingId;
    private UUID slotId;
    private Booking sampleHoldBooking;

    @BeforeEach
    void setUp() {
        confirmBookingUseCase = new ConfirmBookingUseCase(bookingRepository, inventoryLockPort, serviceSlotPort);

        customerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        slotId = UUID.randomUUID();

        BookingItem item = BookingItem.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .slotId(slotId)
                .serviceId(UUID.randomUUID())
                .vendorId(UUID.randomUUID())
                .quantity(2)
                .bookingDate(LocalDate.now().plusDays(1))
                .bookingTime(LocalTime.of(10, 0))
                .price(BigDecimal.valueOf(100_000))
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
    @DisplayName("Confirm valid hold booking successfully commits DB capacity and releases Redis hold")
    void execute_WhenValidHold_ShouldConfirmSuccessfully() {
        ConfirmBookingCommand command = new ConfirmBookingCommand(bookingId, customerId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleHoldBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingHoldResult result = confirmBookingUseCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.status()).isEqualTo(BookingStatus.CONFIRMED);

        verify(serviceSlotPort).commitCapacityBatch(sampleHoldBooking.getItems());

        ArgumentCaptor<List<InventoryLockItem>> lockItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryLockPort).releaseHolds(eq(bookingId), lockItemsCaptor.capture());
        assertThat(lockItemsCaptor.getValue()).hasSize(1);
        assertThat(lockItemsCaptor.getValue().get(0).getSlotId()).isEqualTo(slotId);
        assertThat(lockItemsCaptor.getValue().get(0).getQuantity()).isEqualTo(2);

        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Confirm non-existent booking throws BookingNotFoundException")
    void execute_WhenBookingNotFound_ShouldThrowException() {
        ConfirmBookingCommand command = new ConfirmBookingCommand(bookingId, customerId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> confirmBookingUseCase.execute(command))
                .isInstanceOf(BookingNotFoundException.class);

        verify(serviceSlotPort, never()).commitCapacityBatch(any());
        verify(inventoryLockPort, never()).releaseHolds(any(), any());
    }

    @Test
    @DisplayName("Confirm by different customer throws UnauthorizedBookingAccessException (IDOR)")
    void execute_WhenDifferentCustomer_ShouldThrowException() {
        UUID otherCustomer = UUID.randomUUID();
        ConfirmBookingCommand command = new ConfirmBookingCommand(bookingId, otherCustomer);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleHoldBooking));

        assertThatThrownBy(() -> confirmBookingUseCase.execute(command))
                .isInstanceOf(UnauthorizedBookingAccessException.class);

        verify(serviceSlotPort, never()).commitCapacityBatch(any());
        verify(inventoryLockPort, never()).releaseHolds(any(), any());
    }

    @Test
    @DisplayName("Confirm expired hold throws BookingHoldExpiredException")
    void execute_WhenHoldExpired_ShouldThrowException() {
        sampleHoldBooking.setHoldExpiresAt(OffsetDateTime.now().minusMinutes(1));
        ConfirmBookingCommand command = new ConfirmBookingCommand(bookingId, customerId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleHoldBooking));

        assertThatThrownBy(() -> confirmBookingUseCase.execute(command))
                .isInstanceOf(BookingHoldExpiredException.class);

        verify(serviceSlotPort, never()).commitCapacityBatch(any());
        verify(inventoryLockPort, never()).releaseHolds(any(), any());
    }

    @Test
    @DisplayName("Confirm already confirmed booking throws InvalidBookingStateException")
    void execute_WhenAlreadyConfirmed_ShouldThrowException() {
        sampleHoldBooking.setStatus(BookingStatus.CONFIRMED);
        ConfirmBookingCommand command = new ConfirmBookingCommand(bookingId, customerId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleHoldBooking));

        assertThatThrownBy(() -> confirmBookingUseCase.execute(command))
                .isInstanceOf(InvalidBookingStateException.class);

        verify(serviceSlotPort, never()).commitCapacityBatch(any());
        verify(inventoryLockPort, never()).releaseHolds(any(), any());
    }

    @Test
    @DisplayName("When DB commitCapacityBatch throws InsufficientInventoryException, Redis hold is not released")
    void execute_WhenCommitCapacityFails_ShouldThrowAndNotReleaseRedis() {
        ConfirmBookingCommand command = new ConfirmBookingCommand(bookingId, customerId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleHoldBooking));
        doThrow(new InsufficientInventoryException(slotId, 2, 0))
                .when(serviceSlotPort).commitCapacityBatch(any());

        assertThatThrownBy(() -> confirmBookingUseCase.execute(command))
                .isInstanceOf(InsufficientInventoryException.class);

        verify(inventoryLockPort, never()).releaseHolds(any(), any());
        verify(bookingRepository, never()).save(any());
    }
}
