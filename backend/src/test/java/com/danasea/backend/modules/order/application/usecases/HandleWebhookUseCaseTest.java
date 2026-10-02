package com.danasea.backend.modules.order.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.order.application.dtos.HandleWebhookCommand;
import com.danasea.backend.modules.order.application.dtos.WebhookProcessResult;
import com.danasea.backend.modules.order.domain.exceptions.PaymentVerificationException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.ports.BookingStatusUpdatePort;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.OrderEventPublisherPort;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("HandleWebhookUseCase Unit Tests")
class HandleWebhookUseCaseTest {

    @Mock
    private MasterOrderRepositoryPort masterOrderRepository;
    @Mock
    private SubOrderRepositoryPort subOrderRepository;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private BookingStatusUpdatePort bookingStatusUpdatePort;
    @Mock
    private OrderEventPublisherPort orderEventPublisherPort;

    private HandleWebhookUseCase useCase;

    private final UUID orderId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new HandleWebhookUseCase(
                masterOrderRepository,
                subOrderRepository,
                paymentGatewayPort,
                bookingStatusUpdatePort,
                orderEventPublisherPort
        );
    }

    @Test
    @DisplayName("Thành công: Xác thực chữ ký đúng -> Cập nhật MasterOrder PAID, SubOrder CONFIRMED, xác nhận Booking")
    void shouldProcessPaymentSuccessWebhookSuccessfully() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, bookingId, new BigDecimal("400.00"), OffsetDateTime.now(), "key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);

        SubOrder subOrder = new SubOrder();
        subOrder.setId(UUID.randomUUID());
        subOrder.setMasterOrderId(orderId);
        subOrder.setStatus(SubOrderStatus.PENDING);

        when(paymentGatewayPort.verifyWebhookSignature(any(), any())).thenReturn(true);
        when(subOrderRepository.findByMasterOrderId(orderId)).thenReturn(List.of(subOrder));

        HandleWebhookCommand command = new HandleWebhookCommand(
                PaymentProvider.SEPAY,
                Map.of("amount", "400.00"),
                "{\"amount\": 400.00}",
                "valid-hmac-sig",
                "event-123",
                UUID.randomUUID(),
                "tran-456",
                new BigDecimal("400.00"),
                PaymentStatus.SUCCESS
        );

        WebhookProcessResult result = useCase.execute(command, order);

        assertThat(result).isNotNull();
        assertThat(result.alreadyProcessed()).isFalse();
        assertThat(order.getStatus()).isEqualTo(MasterOrderStatus.PAID);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentOrderStatus.PAID);
        assertThat(subOrder.getStatus()).isEqualTo(SubOrderStatus.CONFIRMED);

        verify(bookingStatusUpdatePort).confirmBooking(bookingId, customerId);
        verify(orderEventPublisherPort).publishPaymentSuccessEvent(order);
    }

    @Test
    @DisplayName("Lỗi chữ ký sai (400): Ném PaymentVerificationException")
    void shouldThrowExceptionWhenSignatureIsInvalid() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, bookingId, new BigDecimal("400.00"), OffsetDateTime.now(), "key-1");
        order.setId(orderId);

        when(paymentGatewayPort.verifyWebhookSignature(any(), any())).thenReturn(false);

        HandleWebhookCommand command = new HandleWebhookCommand(
                PaymentProvider.SEPAY,
                Map.of(),
                "{}",
                "tampered-sig",
                "event-123",
                UUID.randomUUID(),
                "tran-456",
                new BigDecimal("400.00"),
                PaymentStatus.SUCCESS
        );

        assertThatThrownBy(() -> useCase.execute(command, order))
                .isInstanceOf(PaymentVerificationException.class)
                .hasMessageContaining("Invalid webhook signature");

        verify(masterOrderRepository, never()).save(any());
        verify(bookingStatusUpdatePort, never()).confirmBooking(any(), any());
    }

    @Test
    @DisplayName("Idempotency: Webhook gọi lại khi đơn hàng đã PAID -> Trả về alreadyProcessed = true (no-op)")
    void shouldReturnAlreadyProcessedWhenOrderAlreadyPaid() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, bookingId, new BigDecimal("400.00"), OffsetDateTime.now(), "key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PAID);

        when(paymentGatewayPort.verifyWebhookSignature(any(), any())).thenReturn(true);

        HandleWebhookCommand command = new HandleWebhookCommand(
                PaymentProvider.SEPAY,
                Map.of(),
                "{}",
                "valid-sig",
                "event-123",
                UUID.randomUUID(),
                "tran-456",
                new BigDecimal("400.00"),
                PaymentStatus.SUCCESS
        );

        WebhookProcessResult result = useCase.execute(command, order);

        assertThat(result.alreadyProcessed()).isTrue();
        verify(masterOrderRepository, never()).save(any());
        verify(bookingStatusUpdatePort, never()).confirmBooking(any(), any());
    }
}
