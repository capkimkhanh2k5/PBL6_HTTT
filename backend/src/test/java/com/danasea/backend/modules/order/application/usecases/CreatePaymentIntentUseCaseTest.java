package com.danasea.backend.modules.order.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.order.application.dtos.CreatePaymentIntentCommand;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePaymentIntentUseCase Unit Tests")
class CreatePaymentIntentUseCaseTest {

    @Mock
    private MasterOrderRepositoryPort masterOrderRepository;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    private CreatePaymentIntentUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CreatePaymentIntentUseCase(masterOrderRepository, paymentGatewayPort);
    }

    @Test
    @DisplayName("Thành công: Tạo payment intent cho đơn hàng PENDING_PAYMENT")
    void shouldCreatePaymentIntentSuccessfully() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("250.00"), OffsetDateTime.now(), "key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);

        when(masterOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentIntentResult expectedResult = new PaymentIntentResult(
                UUID.randomUUID(), orderId, PaymentProvider.SEPAY, new BigDecimal("250.00"),
                "https://payment.sepay.vn/pay/123", "https://qr.sepay.vn/123", OffsetDateTime.now().plusMinutes(15));
        when(paymentGatewayPort.createPaymentIntent(orderId, new BigDecimal("250.00"), PaymentProvider.SEPAY))
                .thenReturn(expectedResult);

        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(
                customerId, orderId, PaymentProvider.SEPAY, "idem-pay-1");
        PaymentIntentResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.provider()).isEqualTo(PaymentProvider.SEPAY);
        verify(paymentGatewayPort).createPaymentIntent(orderId, new BigDecimal("250.00"), PaymentProvider.SEPAY);
    }

    @Test
    @DisplayName("Chặn IDOR (403): Khách hàng không sở hữu đơn hàng này")
    void shouldThrowUnauthorizedWhenCustomerDoesNotOwnOrder() {
        UUID strangerId = UUID.randomUUID();
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("250.00"), OffsetDateTime.now(), "key-1");
        order.setId(orderId);

        when(masterOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(
                strangerId, orderId, PaymentProvider.SEPAY, "idem-pay-1");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("Trạng thái không hợp lệ (409): Đơn hàng đã ở trạng thái PAID")
    void shouldThrowInvalidStateWhenOrderAlreadyPaid() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("250.00"), OffsetDateTime.now(), "key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PAID);

        when(masterOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(
                customerId, orderId, PaymentProvider.SEPAY, "idem-pay-1");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("PENDING_PAYMENT");
    }
}
