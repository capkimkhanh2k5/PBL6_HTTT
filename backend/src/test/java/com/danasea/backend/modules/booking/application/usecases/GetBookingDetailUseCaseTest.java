package com.danasea.backend.modules.booking.application.usecases;

import java.math.BigDecimal;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.booking.application.dtos.BookingDetailResult;
import com.danasea.backend.modules.booking.application.dtos.GetBookingDetailQuery;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.exceptions.UnauthorizedBookingAccessException;
import com.danasea.backend.modules.booking.domain.models.Booking;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBookingDetailUseCaseTest {

    @Mock
    private BookingRepositoryPort bookingRepository;

    private GetBookingDetailUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetBookingDetailUseCase(bookingRepository);
    }

    private Booking createSampleBooking(UUID ownerId) {
        BookingItem item = BookingItem.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .serviceId(UUID.randomUUID())
                .vendorId(UUID.randomUUID())
                .slotId(UUID.randomUUID())
                .quantity(2)
                .bookingDate(LocalDate.now().plusDays(2))
                .bookingTime(LocalTime.of(10, 0))
                .price(BigDecimal.valueOf(150000))
                .build();

        return Booking.builder()
                .id(bookingId)
                .customerId(ownerId)
                .status(BookingStatus.HOLD)
                .totalAmount(BigDecimal.valueOf(300000))
                .holdExpiresAt(OffsetDateTime.now().plusMinutes(15))
                .items(List.of(item))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("execute successfully returns booking details when caller is the booking owner")
    void execute_WhenCallerIsOwner_ShouldReturnBookingDetailResult() {
        Booking booking = createSampleBooking(customerId);
        when(bookingRepository.findByIdWithItems(bookingId)).thenReturn(Optional.of(booking));

        GetBookingDetailQuery query = new GetBookingDetailQuery(bookingId, customerId, false);
        BookingDetailResult result = useCase.execute(query);

        assertThat(result).isNotNull();
        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.status()).isEqualTo(BookingStatus.HOLD);
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(300000));
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        assertThat(result.items().get(0).subtotal()).isEqualByComparingTo(BigDecimal.valueOf(300000));

        verify(bookingRepository).findByIdWithItems(bookingId);
    }

    @Test
    @DisplayName("execute successfully returns booking details when caller is ADMIN, bypassing ownership check")
    void execute_WhenCallerIsAdmin_ShouldBypassOwnershipCheckAndReturnResult() {
        Booking booking = createSampleBooking(customerId);
        when(bookingRepository.findByIdWithItems(bookingId)).thenReturn(Optional.of(booking));

        // Admin caller with a completely different userId
        GetBookingDetailQuery query = new GetBookingDetailQuery(bookingId, otherUserId, true);
        BookingDetailResult result = useCase.execute(query);

        assertThat(result).isNotNull();
        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.customerId()).isEqualTo(customerId);
        assertThat(result.items()).hasSize(1);

        verify(bookingRepository).findByIdWithItems(bookingId);
    }

    @Test
    @DisplayName("execute throws UnauthorizedBookingAccessException (IDOR protection) when caller is neither owner nor ADMIN")
    void execute_WhenCallerNotOwnerAndNotAdmin_ShouldThrowUnauthorizedBookingAccessException() {
        Booking booking = createSampleBooking(customerId);
        when(bookingRepository.findByIdWithItems(bookingId)).thenReturn(Optional.of(booking));

        // Different user trying to access customerId's booking
        GetBookingDetailQuery query = new GetBookingDetailQuery(bookingId, otherUserId, false);

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(UnauthorizedBookingAccessException.class)
                .hasMessageContaining(bookingId.toString())
                .hasMessageContaining(otherUserId.toString());

        verify(bookingRepository).findByIdWithItems(bookingId);
    }

    @Test
    @DisplayName("execute throws BookingNotFoundException when booking does not exist")
    void execute_WhenBookingNotFound_ShouldThrowBookingNotFoundException() {
        when(bookingRepository.findByIdWithItems(bookingId)).thenReturn(Optional.empty());

        GetBookingDetailQuery query = new GetBookingDetailQuery(bookingId, customerId, false);

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining(bookingId.toString());

        verify(bookingRepository).findByIdWithItems(bookingId);
    }

    @Test
    @DisplayName("execute throws IllegalArgumentException when query or bookingId is null")
    void execute_WhenQueryOrBookingIdIsNull_ShouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class);

        GetBookingDetailQuery invalidQuery = new GetBookingDetailQuery(null, customerId, false);
        assertThatThrownBy(() -> useCase.execute(invalidQuery))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
