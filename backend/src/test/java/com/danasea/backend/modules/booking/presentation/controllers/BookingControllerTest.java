package com.danasea.backend.modules.booking.presentation.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.danasea.backend.modules.booking.application.dtos.BookingDetailResult;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemResult;
import com.danasea.backend.modules.booking.application.dtos.BookingSummaryResult;
import com.danasea.backend.modules.booking.application.dtos.GetBookingDetailQuery;
import com.danasea.backend.modules.booking.application.dtos.GetCustomerBookingsQuery;
import com.danasea.backend.modules.booking.application.dtos.BookingCancelResult;
import com.danasea.backend.modules.booking.application.dtos.CancelBookingCommand;
import com.danasea.backend.modules.booking.application.usecases.CancelBookingUseCase;
import com.danasea.backend.modules.booking.application.usecases.GetBookingDetailUseCase;
import com.danasea.backend.modules.booking.application.usecases.GetCustomerBookingsUseCase;
import com.danasea.backend.modules.booking.domain.exceptions.BookingNotFoundException;
import com.danasea.backend.modules.booking.domain.exceptions.InvalidBookingStateException;
import com.danasea.backend.modules.booking.domain.exceptions.UnauthorizedBookingAccessException;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.PagedResult;
import com.danasea.backend.modules.booking.presentation.handlers.BookingExceptionHandler;
import com.danasea.backend.shared.presentation.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetBookingDetailUseCase getBookingDetailUseCase;

    @Mock
    private GetCustomerBookingsUseCase getCustomerBookingsUseCase;

    @Mock
    private CancelBookingUseCase cancelBookingUseCase;

    @InjectMocks
    private BookingController bookingController;

    private final UUID customerId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new BookingExceptionHandler(), new GlobalExceptionHandler())
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                customerId.toString(),
                "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/bookings/{id} returns 200 OK with booking details")
    void getBookingDetail_Success() throws Exception {
        UUID itemId = UUID.randomUUID();
        BookingHoldItemResult itemResult = new BookingHoldItemResult(
                itemId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                2,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(200000)
        );

        BookingDetailResult detailResult = new BookingDetailResult(
                bookingId,
                customerId,
                BookingStatus.HOLD,
                BigDecimal.valueOf(200000),
                OffsetDateTime.now().plusMinutes(15),
                List.of(itemResult),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(getBookingDetailUseCase.execute(any(GetBookingDetailQuery.class))).thenReturn(detailResult);

        mockMvc.perform(get("/api/bookings/" + bookingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("HOLD"))
                .andExpect(jsonPath("$.totalAmount").value(200000))
                .andExpect(jsonPath("$.items[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(getBookingDetailUseCase).execute(any(GetBookingDetailQuery.class));
    }

    @Test
    @DisplayName("GET /api/bookings/{id} returns 403 FORBIDDEN when user is not the booking owner (IDOR prevented)")
    void getBookingDetail_Forbidden_IDOR() throws Exception {
        when(getBookingDetailUseCase.execute(any(GetBookingDetailQuery.class)))
                .thenThrow(new UnauthorizedBookingAccessException(bookingId, customerId));

        mockMvc.perform(get("/api/bookings/" + bookingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_BOOKING_ACCESS"));

        verify(getBookingDetailUseCase).execute(any(GetBookingDetailQuery.class));
    }

    @Test
    @DisplayName("GET /api/bookings/{id} returns 404 NOT FOUND when booking does not exist")
    void getBookingDetail_NotFound() throws Exception {
        when(getBookingDetailUseCase.execute(any(GetBookingDetailQuery.class)))
                .thenThrow(new BookingNotFoundException(bookingId));

        mockMvc.perform(get("/api/bookings/" + bookingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));

        verify(getBookingDetailUseCase).execute(any(GetBookingDetailQuery.class));
    }

    @Test
    @DisplayName("GET /api/bookings returns 200 OK with paginated booking summaries")
    void getMyBookings_Success() throws Exception {
        BookingSummaryResult summary = new BookingSummaryResult(
                bookingId,
                customerId,
                BookingStatus.CONFIRMED,
                BigDecimal.valueOf(250000),
                2,
                OffsetDateTime.now().plusMinutes(15),
                OffsetDateTime.now()
        );

        PagedResult<BookingSummaryResult> pagedResult = new PagedResult<>(
                List.of(summary),
                0,
                10,
                1L,
                1
        );

        when(getCustomerBookingsUseCase.execute(any(GetCustomerBookingsQuery.class))).thenReturn(pagedResult);

        mockMvc.perform(get("/api/bookings")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.content[0].totalItems").value(2));

        verify(getCustomerBookingsUseCase).execute(any(GetCustomerBookingsQuery.class));
    }

    @Test
    @DisplayName("PATCH /api/bookings/{id}/cancel returns 200 OK with cancellation details and refund policy")
    void cancelBooking_Success() throws Exception {
        BookingCancelResult cancelResult = new BookingCancelResult(
                bookingId,
                customerId,
                BookingStatus.CANCELLED,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(2),
                true,
                100,
                BigDecimal.valueOf(500000),
                "Customer requested cancellation",
                "Hủy thành công. Bạn đủ điều kiện hoàn 100% tiền theo chính sách trước 24h."
        );

        when(cancelBookingUseCase.execute(any(CancelBookingCommand.class))).thenReturn(cancelResult);

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Kế hoạch thay đổi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundEligible").value(true))
                .andExpect(jsonPath("$.refundPercentage").value(100))
                .andExpect(jsonPath("$.refundAmount").value(500000))
                .andExpect(jsonPath("$.message").value("Hủy thành công. Bạn đủ điều kiện hoàn 100% tiền theo chính sách trước 24h."));

        verify(cancelBookingUseCase).execute(any(CancelBookingCommand.class));
    }

    @Test
    @DisplayName("PATCH /api/bookings/{id}/cancel returns 403 FORBIDDEN when user is not owner (IDOR)")
    void cancelBooking_Forbidden_IDOR() throws Exception {
        when(cancelBookingUseCase.execute(any(CancelBookingCommand.class)))
                .thenThrow(new UnauthorizedBookingAccessException(bookingId, customerId));

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_BOOKING_ACCESS"));

        verify(cancelBookingUseCase).execute(any(CancelBookingCommand.class));
    }

    @Test
    @DisplayName("PATCH /api/bookings/{id}/cancel returns 400 BAD REQUEST when booking is already cancelled")
    void cancelBooking_BadRequest_AlreadyCancelled() throws Exception {
        when(cancelBookingUseCase.execute(any(CancelBookingCommand.class)))
                .thenThrow(new InvalidBookingStateException("Booking is already cancelled."));

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BOOKING_STATE"));

        verify(cancelBookingUseCase).execute(any(CancelBookingCommand.class));
    }

    @Test
    @DisplayName("PATCH /api/bookings/{id}/cancel returns 404 NOT FOUND when booking does not exist")
    void cancelBooking_NotFound() throws Exception {
        when(cancelBookingUseCase.execute(any(CancelBookingCommand.class)))
                .thenThrow(new BookingNotFoundException(bookingId));

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));

        verify(cancelBookingUseCase).execute(any(CancelBookingCommand.class));
    }
}
