package com.danasea.backend.modules.order.presentation.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;
import com.danasea.backend.modules.order.presentation.dtos.AdminOrderSummaryResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderController Unit Tests")
class AdminOrderControllerTest {

    @Mock
    private OrderPaymentService orderPaymentService;

    @InjectMocks
    private AdminOrderController adminOrderController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminOrderController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/orders returns paginated admin order summary list with filters")
    void getOrders_ShouldReturnPaginatedList() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        AdminOrderSummaryResponse response = new AdminOrderSummaryResponse(
                orderId,
                bookingId,
                customerId,
                MasterOrderStatus.PAID,
                PaymentOrderStatus.PAID,
                new BigDecimal("1500000.00"),
                BigDecimal.ZERO,
                2,
                List.of(vendorId),
                OffsetDateTime.now().plusMinutes(15),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(orderPaymentService.getAdminOrders(
                eq(MasterOrderStatus.PAID),
                eq(PaymentOrderStatus.PAID),
                eq(customerId),
                eq(vendorId),
                any(),
                any(),
                any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/orders")
                        .param("status", "PAID")
                        .param("paymentStatus", "PAID")
                        .param("customerId", customerId.toString())
                        .param("vendorId", vendorId.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$.content[0].bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.content[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(1500000.00))
                .andExpect(jsonPath("$.content[0].totalItems").value(2))
                .andExpect(jsonPath("$.content[0].vendorIds[0]").value(vendorId.toString()));

        verify(orderPaymentService).getAdminOrders(
                eq(MasterOrderStatus.PAID),
                eq(PaymentOrderStatus.PAID),
                eq(customerId),
                eq(vendorId),
                any(),
                any(),
                any());
    }
}
