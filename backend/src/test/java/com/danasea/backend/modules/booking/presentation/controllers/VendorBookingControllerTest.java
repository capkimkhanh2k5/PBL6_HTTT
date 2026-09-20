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

import com.danasea.backend.modules.booking.application.dtos.GetVendorBookingsQuery;
import com.danasea.backend.modules.booking.application.dtos.VendorBookingItemResult;
import com.danasea.backend.modules.booking.application.usecases.GetVendorBookingsUseCase;
import com.danasea.backend.modules.booking.domain.exceptions.UnauthorizedBookingAccessException;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.domain.models.PagedResult;
import com.danasea.backend.modules.booking.presentation.handlers.BookingExceptionHandler;
import com.danasea.backend.shared.presentation.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VendorBookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetVendorBookingsUseCase getVendorBookingsUseCase;

    @InjectMocks
    private VendorBookingController vendorBookingController;

    private final UUID vendorUserId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vendorBookingController)
                .setControllerAdvice(new BookingExceptionHandler(), new GlobalExceptionHandler())
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                vendorUserId.toString(),
                "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_VENDOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/vendor/bookings returns 200 OK with paginated vendor items")
    void getVendorBookings_Success() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        VendorBookingItemResult itemResult = new VendorBookingItemResult(
                itemId,
                bookingId,
                UUID.randomUUID(),
                vendorId,
                UUID.randomUUID(),
                3,
                LocalDate.now().plusDays(2),
                LocalTime.of(14, 0),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(300000),
                BookingStatus.CONFIRMED,
                OffsetDateTime.now()
        );

        PagedResult<VendorBookingItemResult> pagedResult = new PagedResult<>(
                List.of(itemResult),
                0,
                10,
                1L,
                1
        );

        when(getVendorBookingsUseCase.execute(any(GetVendorBookingsQuery.class))).thenReturn(pagedResult);

        mockMvc.perform(get("/api/vendor/bookings")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].itemId").value(itemId.toString()))
                .andExpect(jsonPath("$.content[0].bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.content[0].vendorId").value(vendorId.toString()))
                .andExpect(jsonPath("$.content[0].quantity").value(3))
                .andExpect(jsonPath("$.content[0].bookingStatus").value("CONFIRMED"));

        verify(getVendorBookingsUseCase).execute(any(GetVendorBookingsQuery.class));
    }

    @Test
    @DisplayName("GET /api/vendor/bookings returns 403 FORBIDDEN when user has no vendor profile")
    void getVendorBookings_Forbidden_NoVendorProfile() throws Exception {
        when(getVendorBookingsUseCase.execute(any(GetVendorBookingsQuery.class)))
                .thenThrow(new UnauthorizedBookingAccessException("Vendor profile not found for user: " + vendorUserId));

        mockMvc.perform(get("/api/vendor/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED_BOOKING_ACCESS"));

        verify(getVendorBookingsUseCase).execute(any(GetVendorBookingsQuery.class));
    }
}
