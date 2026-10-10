package com.danasea.backend.modules.order.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.RefundStatus;
import com.danasea.backend.modules.order.presentation.dtos.RefundDetailResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminRefundController Unit Tests")
class AdminRefundControllerTest {

    @Mock
    private OrderPaymentService orderPaymentService;

    @InjectMocks
    private AdminRefundController controller;

    private MockMvc mockMvc;

    private final UUID refundId = UUID.randomUUID();
    private final UUID subOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Should query refunds page with basic filters")
    void shouldQueryRefundsPage() {
        RefundDetailResponse item = new RefundDetailResponse(
                refundId,
                subOrderId,
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(100),
                RefundReason.CUSTOMER_CANCEL,
                RefundStatus.PROCESSED,
                PaymentProvider.PAYPAL,
                "REF-123",
                "CAPTURE-123",
                0,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        Page<RefundDetailResponse> page = new PageImpl<>(List.of(item));
        Pageable pageable = PageRequest.of(0, 20);

        when(orderPaymentService.getAdminRefunds(eq(RefundStatus.PROCESSED), eq(RefundReason.CUSTOMER_CANCEL), eq(subOrderId), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<RefundDetailResponse>> response = controller.getRefunds(
                RefundStatus.PROCESSED, RefundReason.CUSTOMER_CANCEL, subOrderId, pageable);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).id()).isEqualTo(refundId);
    }

    @Test
    @DisplayName("GET /api/admin/refunds with advanced filters (orderId, provider, vendorId, customerId, dates)")
    void getRefunds_WithAdvancedFilters_ShouldReturnFilteredList() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID vendorId = UUID.randomUUID();

        RefundDetailResponse item = new RefundDetailResponse(
                refundId,
                subOrderId,
                BigDecimal.valueOf(250000),
                null,
                RefundReason.CUSTOMER_CANCEL,
                RefundStatus.PROCESSED,
                PaymentProvider.VNPAY,
                "VNP-REF-999",
                "TXN-777",
                0,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(orderPaymentService.getAdminRefunds(
                eq(RefundStatus.PROCESSED),
                eq(RefundReason.CUSTOMER_CANCEL),
                eq(subOrderId),
                eq(PaymentProvider.VNPAY),
                eq(vendorId),
                eq(customerId),
                eq(orderId),
                any(),
                any(),
                any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/refunds")
                        .param("status", "PROCESSED")
                        .param("reason", "CUSTOMER_CANCEL")
                        .param("subOrderId", subOrderId.toString())
                        .param("orderId", orderId.toString())
                        .param("provider", "VNPAY")
                        .param("vendorId", vendorId.toString())
                        .param("customerId", customerId.toString())
                        .param("fromDate", "2026-10-01T00:00:00Z")
                        .param("toDate", "2026-10-09T23:59:59Z")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(refundId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PROCESSED"))
                .andExpect(jsonPath("$.content[0].provider").value("VNPAY"));

        verify(orderPaymentService).getAdminRefunds(
                eq(RefundStatus.PROCESSED),
                eq(RefundReason.CUSTOMER_CANCEL),
                eq(subOrderId),
                eq(PaymentProvider.VNPAY),
                eq(vendorId),
                eq(customerId),
                eq(orderId),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("Should return refund detail by ID")
    void shouldReturnRefundDetailById() {
        RefundDetailResponse detail = new RefundDetailResponse(
                refundId,
                subOrderId,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(50),
                RefundReason.WEATHER,
                RefundStatus.PENDING,
                PaymentProvider.VNPAY,
                null,
                "TXN-456",
                1,
                "Timeout",
                null,
                OffsetDateTime.now()
        );

        when(orderPaymentService.getAdminRefundDetail(refundId)).thenReturn(detail);

        ResponseEntity<RefundDetailResponse> response = controller.getRefundDetail(refundId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(refundId);
        assertThat(response.getBody().provider()).isEqualTo(PaymentProvider.VNPAY);
        assertThat(response.getBody().retryCount()).isEqualTo(1);
    }
}
