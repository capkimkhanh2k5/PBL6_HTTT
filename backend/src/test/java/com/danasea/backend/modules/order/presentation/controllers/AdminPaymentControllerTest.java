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
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.presentation.dtos.PaymentResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPaymentController Unit Tests")
class AdminPaymentControllerTest {

    @Mock
    private OrderPaymentService orderPaymentService;

    @InjectMocks
    private AdminPaymentController adminPaymentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminPaymentController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/payments returns paginated payment list")
    void getPayments_ShouldReturnPaginatedList() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                PaymentProvider.VNPAY,
                "vnp-tx-123",
                new BigDecimal("500000"),
                PaymentStatus.SUCCESS,
                "idem-1",
                "wh-event-1",
                "https://payment.url",
                "https://qr.url",
                OffsetDateTime.now().plusMinutes(15),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(orderPaymentService.getAdminPayments(eq(PaymentStatus.SUCCESS), eq(PaymentProvider.VNPAY), eq(orderId), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/payments")
                        .param("status", "SUCCESS")
                        .param("provider", "VNPAY")
                        .param("orderId", orderId.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(paymentId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].provider").value("VNPAY"));

        verify(orderPaymentService).getAdminPayments(eq(PaymentStatus.SUCCESS), eq(PaymentProvider.VNPAY), eq(orderId), any());
    }

    @Test
    @DisplayName("GET /api/admin/payments with advanced filters (vendor, customer, dates)")
    void getPayments_WithAdvancedFilters_ShouldReturnFilteredList() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                PaymentProvider.VNPAY,
                "vnp-tx-999",
                new BigDecimal("350000"),
                PaymentStatus.SUCCESS,
                "idem-9",
                "wh-event-9",
                "https://payment.url",
                "https://qr.url",
                OffsetDateTime.now().plusMinutes(15),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(orderPaymentService.getAdminPayments(
                eq(PaymentStatus.SUCCESS),
                eq(PaymentProvider.VNPAY),
                eq(orderId),
                eq(customerId),
                eq(vendorId),
                any(),
                any(),
                any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/payments")
                        .param("status", "SUCCESS")
                        .param("provider", "VNPAY")
                        .param("orderId", orderId.toString())
                        .param("customerId", customerId.toString())
                        .param("vendorId", vendorId.toString())
                        .param("fromDate", "2026-10-01T00:00:00Z")
                        .param("toDate", "2026-10-09T23:59:59Z")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(paymentId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));

        verify(orderPaymentService).getAdminPayments(
                eq(PaymentStatus.SUCCESS),
                eq(PaymentProvider.VNPAY),
                eq(orderId),
                eq(customerId),
                eq(vendorId),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("GET /api/admin/payments/{id} returns payment detail")
    void getPaymentDetail_ShouldReturnDetail() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                PaymentProvider.PAYPAL,
                "paypal-tx-123",
                new BigDecimal("100.00"),
                PaymentStatus.PENDING,
                "idem-2",
                null,
                "https://paypal.url",
                "https://paypal-qr.url",
                OffsetDateTime.now().plusMinutes(15),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(orderPaymentService.getAdminPaymentDetail(paymentId)).thenReturn(response);

        mockMvc.perform(get("/api/admin/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.provider").value("PAYPAL"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderPaymentService).getAdminPaymentDetail(paymentId);
    }
}
