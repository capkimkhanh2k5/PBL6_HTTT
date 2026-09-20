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

import com.danasea.backend.modules.booking.application.dtos.GetVendorBookingsQuery;
import com.danasea.backend.modules.booking.application.dtos.VendorBookingItemResult;
import com.danasea.backend.modules.booking.domain.exceptions.UnauthorizedBookingAccessException;
import com.danasea.backend.modules.booking.domain.models.BookingItem;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.PagedResult;
import com.danasea.backend.modules.booking.domain.ports.BookingRepositoryPort;
import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVendorBookingsUseCaseTest {

    @Mock
    private BookingRepositoryPort bookingRepository;

    @Mock
    private VendorLookupPort vendorLookupPort;

    private GetVendorBookingsUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetVendorBookingsUseCase(bookingRepository, vendorLookupPort);
    }

    private BookingItem createBookingItem(UUID bookingId, BookingStatus status) {
        return BookingItem.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .serviceId(UUID.randomUUID())
                .vendorId(vendorId)
                .slotId(UUID.randomUUID())
                .quantity(3)
                .bookingDate(LocalDate.now().plusDays(1))
                .bookingTime(LocalTime.of(14, 30))
                .price(BigDecimal.valueOf(100000))
                .bookingStatus(status)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("execute successfully resolves vendor and returns paged booking items")
    void execute_WhenVendorExists_ShouldReturnPagedVendorItems() {
        UUID bookingId = UUID.randomUUID();
        BookingItem item = createBookingItem(bookingId, BookingStatus.CONFIRMED);
        PagedResult<BookingItem> repoResult = new PagedResult<>(List.of(item), 0, 10, 1L, 1);

        when(vendorLookupPort.findVendorIdByUserId(userId)).thenReturn(Optional.of(vendorId));
        when(bookingRepository.findVendorBookingItems(vendorId, BookingStatus.CONFIRMED, 0, 10, "createdAt", "desc"))
                .thenReturn(repoResult);

        GetVendorBookingsQuery query = new GetVendorBookingsQuery(
                userId,
                BookingStatus.CONFIRMED,
                0,
                10,
                "createdAt",
                "desc"
        );

        PagedResult<VendorBookingItemResult> result = useCase.execute(query);

        assertThat(result).isNotNull();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.content()).hasSize(1);

        VendorBookingItemResult itemResult = result.content().get(0);
        assertThat(itemResult.itemId()).isEqualTo(item.getId());
        assertThat(itemResult.bookingId()).isEqualTo(bookingId);
        assertThat(itemResult.vendorId()).isEqualTo(vendorId);
        assertThat(itemResult.quantity()).isEqualTo(3);
        assertThat(itemResult.subtotal()).isEqualByComparingTo(BigDecimal.valueOf(300000));
        assertThat(itemResult.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);

        verify(vendorLookupPort).findVendorIdByUserId(userId);
        verify(bookingRepository).findVendorBookingItems(vendorId, BookingStatus.CONFIRMED, 0, 10, "createdAt", "desc");
    }

    @Test
    @DisplayName("execute throws UnauthorizedBookingAccessException when user is not associated with a vendor profile")
    void execute_WhenVendorNotFound_ShouldThrowUnauthorizedBookingAccessException() {
        when(vendorLookupPort.findVendorIdByUserId(userId)).thenReturn(Optional.empty());

        GetVendorBookingsQuery query = new GetVendorBookingsQuery(userId, null, 0, 10, "createdAt", "desc");

        assertThatThrownBy(() -> useCase.execute(query))
                .isInstanceOf(UnauthorizedBookingAccessException.class)
                .hasMessageContaining("Vendor profile not found");

        verify(vendorLookupPort).findVendorIdByUserId(userId);
    }

    @Test
    @DisplayName("execute throws IllegalArgumentException when query or userId is null")
    void execute_WhenUserIdIsNull_ShouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class);

        GetVendorBookingsQuery invalidQuery = new GetVendorBookingsQuery(null, null, 0, 10, "createdAt", "desc");
        assertThatThrownBy(() -> useCase.execute(invalidQuery))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
