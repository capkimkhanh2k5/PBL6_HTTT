package com.danasea.backend.modules.booking.presentation.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collections;
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

import com.danasea.backend.modules.booking.application.dtos.BookingHoldItemResult;
import com.danasea.backend.modules.booking.application.dtos.BookingHoldResult;
import com.danasea.backend.modules.booking.application.usecases.CreateBookingHoldUseCase;
import com.danasea.backend.modules.booking.domain.exceptions.InsufficientInventoryException;
import com.danasea.backend.modules.booking.domain.exceptions.SlotNotAvailableException;
import com.danasea.backend.modules.booking.domain.models.BookingStatus;
import com.danasea.backend.modules.booking.presentation.handlers.BookingExceptionHandler;
import com.danasea.backend.shared.presentation.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingHoldControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateBookingHoldUseCase createBookingHoldUseCase;

    @InjectMocks
    private BookingHoldController bookingHoldController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingHoldController)
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
    @DisplayName("POST /api/bookings/hold returns 201 CREATED when hold is successful")
    void testHoldBookingSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        BookingHoldItemResult itemResult = new BookingHoldItemResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                slotId,
                2,
                LocalDate.now().plusDays(2),
                LocalTime.of(9, 0),
                new BigDecimal("150.00"),
                new BigDecimal("300.00")
        );

        BookingHoldResult result = new BookingHoldResult(
                bookingId,
                customerId,
                BookingStatus.HOLD,
                new BigDecimal("300.00"),
                now.plusMinutes(15),
                List.of(itemResult),
                now
        );

        when(createBookingHoldUseCase.execute(any())).thenReturn(result);

        String jsonPayload = """
                {
                    "items": [
                        {
                            "slotId": "%s",
                            "quantity": 2
                        }
                    ]
                }
                """.formatted(slotId);

        mockMvc.perform(post("/api/bookings/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("HOLD"))
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andExpect(jsonPath("$.items[0].slotId").value(slotId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("POST /api/bookings/hold returns 409 CONFLICT when slot has insufficient inventory")
    void testHoldBookingInsufficientInventory() throws Exception {
        UUID slotId = UUID.randomUUID();
        when(createBookingHoldUseCase.execute(any()))
                .thenThrow(new InsufficientInventoryException(slotId, 3, 1));

        String jsonPayload = """
                {
                    "items": [
                        {
                            "slotId": "%s",
                            "quantity": 3
                        }
                    ]
                }
                """.formatted(slotId);

        mockMvc.perform(post("/api/bookings/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_INVENTORY"));
    }

    @Test
    @DisplayName("POST /api/bookings/hold returns 400 BAD REQUEST when slot is unavailable")
    void testHoldBookingSlotUnavailable() throws Exception {
        UUID slotId = UUID.randomUUID();
        when(createBookingHoldUseCase.execute(any()))
                .thenThrow(new SlotNotAvailableException(slotId, "Slot is CLOSED"));

        String jsonPayload = """
                {
                    "items": [
                        {
                            "slotId": "%s",
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(slotId);

        mockMvc.perform(post("/api/bookings/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/bookings/hold returns 400 BAD REQUEST when request body is invalid")
    void testHoldBookingInvalidBody() throws Exception {
        String invalidPayload = """
                {
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/bookings/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
