package com.danasea.backend.modules.order.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.danasea.backend.modules.booking.infrastructure.persistence.repositories.JpaBookingRepository;
import com.danasea.backend.modules.order.application.OrderPaymentService;
import com.danasea.backend.modules.order.application.dtos.CreatePaymentIntentCommand;
import com.danasea.backend.modules.order.domain.exceptions.WaiverAcceptanceRequiredException;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.MissingWaiverItem;
import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import com.danasea.backend.modules.order.domain.models.PaymentStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.PaymentGatewayPort;
import com.danasea.backend.modules.order.domain.ports.PaymentIntentResult;
import com.danasea.backend.modules.order.domain.ports.ServiceWaiverLookupPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.MasterOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.PaymentJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaMasterOrderRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaPaymentRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.shared.i18n.LocalizedContentSelector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Waiver Guard Tests")
class PaymentWaiverGuardTest {

    @Mock private MasterOrderRepositoryPort masterOrderRepository;
    @Mock private PaymentGatewayPort paymentGatewayPort;
    @Mock private JpaPaymentRepository paymentRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private SubOrderRepositoryPort subOrderRepository;
    @Mock private ServiceWaiverLookupPort serviceWaiverLookupPort;

    @Mock private JpaBookingRepository jpaBookingRepository;
    @Mock private JpaMasterOrderRepository jpaMasterOrderRepository;
    @Mock private JpaSubOrderRepository jpaSubOrderRepository;

    private LocalizedContentSelector localizedContentSelector;
    private CreatePaymentIntentUseCase createPaymentIntentUseCase;
    private OrderPaymentService orderPaymentService;

    private final UUID customerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID subOrderId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        localizedContentSelector = new LocalizedContentSelector();

        createPaymentIntentUseCase =
                new CreatePaymentIntentUseCase(
                        masterOrderRepository,
                        paymentGatewayPort,
                        paymentRepository,
                        transactionManager,
                        subOrderRepository,
                        serviceWaiverLookupPort,
                        localizedContentSelector);

        orderPaymentService =
                new OrderPaymentService(
                        jpaBookingRepository,
                        jpaMasterOrderRepository,
                        jpaSubOrderRepository,
                        paymentRepository,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        createPaymentIntentUseCase,
                        null,
                        paymentGatewayPort,
                        transactionManager,
                        null);
        orderPaymentService.setServiceWaiverLookupPort(serviceWaiverLookupPort);
        orderPaymentService.setLocalizedContentSelector(localizedContentSelector);
    }

    private MasterOrder createPayableMasterOrder() {
        MasterOrder order =
                MasterOrder.createFromBooking(
                        customerId,
                        UUID.randomUUID(),
                        BigDecimal.valueOf(500000),
                        OffsetDateTime.now().plusMinutes(15),
                        "idem-key-1");
        order.setId(orderId);
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);
        return order;
    }

    private SubOrder createSubOrder(boolean required, boolean accepted) {
        SubOrder s = new SubOrder();
        s.setId(subOrderId);
        s.setMasterOrderId(orderId);
        s.setServiceId(serviceId);
        s.setWaiverRequired(required);
        s.setWaiverVersion(1);
        s.setWaiverContent("Cam kết tiếng Việt");
        s.setWaiverContentEn("English waiver");
        s.setWaiverAccepted(accepted);
        return s;
    }

    @Nested
    @DisplayName("CreatePaymentIntent - Guard chặn khởi tạo Intent")
    class CreatePaymentIntentGuardTests {

        @Test
        @DisplayName(
                "Chặn Intent: Đơn có sub-order bắt buộc chưa xác nhận -> Ném"
                        + " WaiverAcceptanceRequiredException và KHÔNG gọi cổng")
        void shouldBlockPaymentIntentWhenWaiverNotAccepted() {
            MasterOrder order = createPayableMasterOrder();
            SubOrder subOrder = createSubOrder(true, false);

            when(masterOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
            when(subOrderRepository.findByMasterOrderIdForUpdate(orderId))
                    .thenReturn(List.of(subOrder));
            when(serviceWaiverLookupPort.findWaiverSnapshot(serviceId))
                    .thenReturn(
                            Optional.of(
                                    new ServiceWaiverLookupPort.ServiceWaiverSnapshot(
                                            serviceId,
                                            "Tour Lặn Biển",
                                            true,
                                            1,
                                            "Cam kết tiếng Việt",
                                            "English waiver")));

            CreatePaymentIntentCommand cmd =
                    new CreatePaymentIntentCommand(
                            customerId, orderId, PaymentProvider.VNPAY, "idem-pay-intent-1");

            assertThatThrownBy(() -> createPaymentIntentUseCase.execute(cmd))
                    .isInstanceOf(WaiverAcceptanceRequiredException.class)
                    .satisfies(
                            ex -> {
                                WaiverAcceptanceRequiredException waiverEx =
                                        (WaiverAcceptanceRequiredException) ex;
                                assertThat(waiverEx.getOrderId()).isEqualTo(orderId);
                                assertThat(waiverEx.getMissingSubOrders()).hasSize(1);
                                MissingWaiverItem item = waiverEx.getMissingSubOrders().get(0);
                                assertThat(item.subOrderId()).isEqualTo(subOrderId);
                                assertThat(item.serviceId()).isEqualTo(serviceId);
                                assertThat(item.serviceName()).isEqualTo("Tour Lặn Biển");
                                assertThat(item.waiverVersion()).isEqualTo(1);
                                assertThat(item.required()).isTrue();
                                assertThat(item.waiverContent()).isEqualTo("Cam kết tiếng Việt");
                                assertThat(item.contentLanguage()).isEqualTo("VI");
                                assertThat(item.fallbackUsed()).isFalse();
                            });

            verify(paymentGatewayPort, never()).createPaymentIntent(any(), any(), any(), any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName(
                "Cho phép Intent: Đơn có sub-order bắt buộc nhưng ĐÃ XÁC NHẬN -> Khởi tạo intent"
                        + " thành công và gọi cổng")
        void shouldAllowPaymentIntentWhenWaiverAccepted() {
            MasterOrder order = createPayableMasterOrder();
            SubOrder subOrder = createSubOrder(true, true);

            when(masterOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
            when(subOrderRepository.findByMasterOrderIdForUpdate(orderId))
                    .thenReturn(List.of(subOrder));
            when(paymentRepository.findByMasterOrderIdAndIdempotencyKey(
                            orderId, "idem-pay-intent-2"))
                    .thenReturn(Optional.empty());

            UUID paymentId = UUID.randomUUID();
            when(paymentRepository.save(any(PaymentJpaEntity.class)))
                    .thenAnswer(
                            inv -> {
                                PaymentJpaEntity p = inv.getArgument(0);
                                if (p.getId() == null) {
                                    p.setId(paymentId);
                                }
                                Mockito.lenient()
                                        .when(paymentRepository.findByIdForUpdate(p.getId()))
                                        .thenReturn(Optional.of(p));
                                return p;
                            });

            when(paymentGatewayPort.createPaymentIntent(
                            eq(paymentId),
                            eq(orderId),
                            eq(order.getTotalAmount()),
                            eq(PaymentProvider.VNPAY)))
                    .thenReturn(
                            new PaymentIntentResult(
                                    paymentId,
                                    orderId,
                                    PaymentProvider.VNPAY,
                                    order.getTotalAmount(),
                                    "https://payment.vnpay.vn/pay",
                                    "https://payment.vnpay.vn/qr",
                                    OffsetDateTime.now().plusMinutes(15),
                                    "VNPAY123",
                                    order.getTotalAmount(),
                                    "VND",
                                    "20261010140000"));

            CreatePaymentIntentCommand cmd =
                    new CreatePaymentIntentCommand(
                            customerId, orderId, PaymentProvider.VNPAY, "idem-pay-intent-2");

            PaymentIntentResult result = createPaymentIntentUseCase.execute(cmd);

            assertThat(result).isNotNull();
            assertThat(result.paymentUrl()).isEqualTo("https://payment.vnpay.vn/pay");
            verify(paymentGatewayPort)
                    .createPaymentIntent(eq(paymentId), eq(orderId), any(), any());
        }

        @Test
        @DisplayName(
                "Cho phép Intent: Đơn KHÔNG yêu cầu cam kết (waiverRequired=false) -> Khởi tạo"
                        + " intent bình thường")
        void shouldAllowPaymentIntentWhenWaiverNotRequired() {
            MasterOrder order = createPayableMasterOrder();
            SubOrder subOrder = createSubOrder(false, false);

            when(masterOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
            when(subOrderRepository.findByMasterOrderIdForUpdate(orderId))
                    .thenReturn(List.of(subOrder));
            when(paymentRepository.findByMasterOrderIdAndIdempotencyKey(
                            orderId, "idem-pay-intent-3"))
                    .thenReturn(Optional.empty());

            UUID paymentId = UUID.randomUUID();
            when(paymentRepository.save(any(PaymentJpaEntity.class)))
                    .thenAnswer(
                            inv -> {
                                PaymentJpaEntity p = inv.getArgument(0);
                                if (p.getId() == null) {
                                    p.setId(paymentId);
                                }
                                Mockito.lenient()
                                        .when(paymentRepository.findByIdForUpdate(p.getId()))
                                        .thenReturn(Optional.of(p));
                                return p;
                            });

            when(paymentGatewayPort.createPaymentIntent(
                            eq(paymentId),
                            eq(orderId),
                            eq(order.getTotalAmount()),
                            eq(PaymentProvider.VNPAY)))
                    .thenReturn(
                            new PaymentIntentResult(
                                    paymentId,
                                    orderId,
                                    PaymentProvider.VNPAY,
                                    order.getTotalAmount(),
                                    "https://payment.vnpay.vn/pay",
                                    null,
                                    OffsetDateTime.now().plusMinutes(15),
                                    "VNPAY123",
                                    order.getTotalAmount(),
                                    "VND",
                                    "20261010140000"));

            CreatePaymentIntentCommand cmd =
                    new CreatePaymentIntentCommand(
                            customerId, orderId, PaymentProvider.VNPAY, "idem-pay-intent-3");

            PaymentIntentResult result = createPaymentIntentUseCase.execute(cmd);

            assertThat(result).isNotNull();
            verify(paymentGatewayPort)
                    .createPaymentIntent(eq(paymentId), eq(orderId), any(), any());
        }
    }

    @Nested
    @DisplayName("capturePayPalOrder - Guard chặn Capture PayPal")
    class PayPalCaptureGuardTests {

        @Test
        @DisplayName(
                "Chặn PayPal Capture: Đơn có sub-order bắt buộc chưa xác nhận -> Ném"
                        + " WaiverAcceptanceRequiredException và KHÔNG gọi PayPal Capture")
        void shouldBlockPayPalCaptureWhenWaiverNotAccepted() {
            UUID paymentId = UUID.randomUUID();

            PaymentJpaEntity payment = new PaymentJpaEntity();
            payment.setId(paymentId);
            payment.setMasterOrderId(orderId);
            payment.setProvider(PaymentProvider.PAYPAL);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setProviderOrderId("PAYPAL-ORDER-123");
            payment.setAmount(BigDecimal.valueOf(500000));
            payment.setProviderAmount(BigDecimal.valueOf(20.00));
            payment.setProviderCurrency("USD");
            payment.setExpiresAt(OffsetDateTime.now().plusMinutes(10));

            MasterOrderJpaEntity orderEntity = new MasterOrderJpaEntity();
            orderEntity.setId(orderId);
            orderEntity.setCustomerId(customerId);
            orderEntity.setStatus(MasterOrderStatus.PENDING_PAYMENT);
            orderEntity.setTotalAmount(BigDecimal.valueOf(500000));

            SubOrderJpaEntity subOrderEntity = new SubOrderJpaEntity();
            subOrderEntity.setId(subOrderId);
            subOrderEntity.setMasterOrderId(orderId);
            subOrderEntity.setServiceId(serviceId);
            subOrderEntity.setWaiverRequired(true);
            subOrderEntity.setWaiverAccepted(false);
            subOrderEntity.setWaiverContent("Cam kết VI");

            when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
            when(jpaMasterOrderRepository.findByIdForUpdate(orderId))
                    .thenReturn(Optional.of(orderEntity));
            when(jpaSubOrderRepository.findByMasterOrderIdForUpdate(orderId))
                    .thenReturn(List.of(subOrderEntity));

            assertThatThrownBy(
                            () ->
                                    orderPaymentService.capturePayPalOrder(
                                            customerId,
                                            paymentId,
                                            "PAYPAL-ORDER-123",
                                            "idem-capture-key-1"))
                    .isInstanceOf(WaiverAcceptanceRequiredException.class);

            verify(paymentGatewayPort, never()).captureOrder(any(), any(), any());
        }
    }
}
