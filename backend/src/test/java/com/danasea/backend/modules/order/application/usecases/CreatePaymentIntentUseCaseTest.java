package com.danasea.backend.modules.order.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.danasea.backend.modules.order.application.dtos.CreatePaymentIntentCommand;
import com.danasea.backend.modules.order.domain.exceptions.InvalidOrderStateException;
import com.danasea.backend.modules.order.domain.exceptions.UnauthorizedOrderAccessException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.PaymentJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaPaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePaymentIntentUseCase Unit Tests")
class CreatePaymentIntentUseCaseTest {

    @Mock
    private MasterOrderRepositoryPort masterOrderRepository;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;
    @Mock
    private JpaPaymentRepository paymentRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private CreatePaymentIntentUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        useCase = new CreatePaymentIntentUseCase(masterOrderRepository, paymentGatewayPort, paymentRepository, transactionManager);
    }

    @Test
    @DisplayName("Thành công: Lưu bản ghi Payment PENDING, gọi Gateway và cập nhật URL/QR")
    void shouldCreatePaymentIntentAndPersistPaymentSuccessfully() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("250.00"), OffsetDateTime.now().plusMinutes(15), "key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);

        when(masterOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByMasterOrderIdAndIdempotencyKey(orderId, "idem-pay-1")).thenReturn(Optional.empty());

        UUID generatedPaymentId = UUID.randomUUID();
        when(paymentRepository.save(any(PaymentJpaEntity.class))).thenAnswer(invocation -> {
            PaymentJpaEntity p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(generatedPaymentId);
            }
            Mockito.lenient().when(paymentRepository.findByIdForUpdate(p.getId())).thenReturn(Optional.of(p));
            return p;
        });

        PaymentIntentResult expectedResult = new PaymentIntentResult(
                generatedPaymentId, orderId, PaymentProvider.PAYPAL, new BigDecimal("250.00"),
                "https://www.sandbox.paypal.com/checkoutnow?token=123", "https://www.sandbox.paypal.com/checkoutnow?token=123", OffsetDateTime.now().plusMinutes(15));
        when(paymentGatewayPort.createPaymentIntent(generatedPaymentId, orderId, new BigDecimal("250.00"), PaymentProvider.PAYPAL))
                .thenReturn(expectedResult);

        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(
                customerId, orderId, PaymentProvider.PAYPAL, "idem-pay-1");
        PaymentIntentResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo(generatedPaymentId);
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.provider()).isEqualTo(PaymentProvider.PAYPAL);
        verify(paymentGatewayPort).createPaymentIntent(generatedPaymentId, orderId, new BigDecimal("250.00"), PaymentProvider.PAYPAL);
    }

    @Test
    @DisplayName("Idempotency: Trả về intent hiện có nếu cùng Idempotency-Key và chưa hết hạn")
    void shouldReturnExistingPaymentIntentWhenSameIdempotencyKey() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("250.00"), OffsetDateTime.now().plusMinutes(15), "key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);

        UUID existingPaymentId = UUID.randomUUID();
        PaymentJpaEntity existingPayment = new PaymentJpaEntity();
        existingPayment.setId(existingPaymentId);
        existingPayment.setMasterOrderId(orderId);
        existingPayment.setProvider(PaymentProvider.PAYPAL);
        existingPayment.setAmount(new BigDecimal("250.00"));
        existingPayment.setStatus(PaymentStatus.PENDING);
        existingPayment.setIdempotencyKey("idem-pay-1");
        existingPayment.setPaymentUrl("https://www.sandbox.paypal.com/checkoutnow?token=123");
        existingPayment.setExpiresAt(OffsetDateTime.now().plusMinutes(10));

        when(masterOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByMasterOrderIdAndIdempotencyKey(orderId, "idem-pay-1"))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.findByIdForUpdate(existingPayment.getId())).thenReturn(Optional.of(existingPayment));

        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(
                customerId, orderId, PaymentProvider.PAYPAL, "idem-pay-1");
        PaymentIntentResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo(existingPaymentId);
        assertThat(result.paymentUrl()).isEqualTo("https://www.sandbox.paypal.com/checkoutnow?token=123");

        // Gateway không bị gọi lại
        verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Idempotency xung đột (409): Cùng Idempotency-Key nhưng khác PaymentProvider")
    void shouldThrowInvalidStateWhenIdempotencyKeyUsedWithDifferentProvider() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("250.00"), OffsetDateTime.now().plusMinutes(15), "key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);

        PaymentJpaEntity existingPayment = new PaymentJpaEntity();
        existingPayment.setId(UUID.randomUUID());
        existingPayment.setMasterOrderId(orderId);
        existingPayment.setProvider(PaymentProvider.VNPAY);
        existingPayment.setStatus(PaymentStatus.PENDING);
        existingPayment.setIdempotencyKey("idem-pay-1");

        when(masterOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByMasterOrderIdAndIdempotencyKey(orderId, "idem-pay-1"))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.findByIdForUpdate(existingPayment.getId())).thenReturn(Optional.of(existingPayment));

        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(
                customerId, orderId, PaymentProvider.PAYPAL, "idem-pay-1");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("another provider");
    }

    @Test
    @DisplayName("Idempotency hết hạn (409): Intent cũ đã quá thời gian expiresAt -> Chuyển FAILED và ném exception")
    void shouldThrowInvalidStateWhenPaymentIntentExpired() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("250.00"), OffsetDateTime.now().plusMinutes(15), "key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);

        PaymentJpaEntity existingPayment = new PaymentJpaEntity();
        existingPayment.setId(UUID.randomUUID());
        existingPayment.setMasterOrderId(orderId);
        existingPayment.setProvider(PaymentProvider.PAYPAL);
        existingPayment.setStatus(PaymentStatus.PENDING);
        existingPayment.setIdempotencyKey("idem-pay-1");
        existingPayment.setExpiresAt(OffsetDateTime.now().minusMinutes(5));

        when(masterOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByMasterOrderIdAndIdempotencyKey(orderId, "idem-pay-1"))
                .thenReturn(Optional.of(existingPayment));
        when(paymentRepository.findByIdForUpdate(existingPayment.getId())).thenReturn(Optional.of(existingPayment));

        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(
                customerId, orderId, PaymentProvider.PAYPAL, "idem-pay-1");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("expired");

        assertThat(existingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(existingPayment);
    }

    @Test
    @DisplayName("Chặn IDOR (403): Khách hàng không sở hữu đơn hàng này")
    void shouldThrowUnauthorizedWhenCustomerDoesNotOwnOrder() {
        UUID strangerId = UUID.randomUUID();
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("250.00"), OffsetDateTime.now().plusMinutes(15), "key-1");
        order.setId(orderId);

        when(masterOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(
                strangerId, orderId, PaymentProvider.PAYPAL, "idem-pay-1");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("Trạng thái không hợp lệ (409): Đơn hàng đã ở trạng thái PAID")
    void shouldThrowInvalidStateWhenOrderAlreadyPaid() {
        MasterOrder order = MasterOrder.createFromBooking(
                customerId, UUID.randomUUID(), new BigDecimal("250.00"), OffsetDateTime.now().plusMinutes(15), "key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PAID);

        when(masterOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        CreatePaymentIntentCommand command = new CreatePaymentIntentCommand(
                customerId, orderId, PaymentProvider.PAYPAL, "idem-pay-1");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("PENDING_PAYMENT");
    }
}
