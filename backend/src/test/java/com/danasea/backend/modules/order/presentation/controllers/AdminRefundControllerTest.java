package com.danasea.backend.modules.order.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

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

    private AdminRefundController controller;

    private final UUID refundId = UUID.randomUUID();
    private final UUID subOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new AdminRefundController(orderPaymentService);
    }

    @Test
    @DisplayName("Should query refunds page with filters")
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
