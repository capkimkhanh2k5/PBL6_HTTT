package com.danasea.backend.modules.order.infrastructure.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.booking.domain.events.BookingHoldExpiredEvent;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.OrderEventPublisherPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.DiscountCodeJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountCodeRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountRedemptionRepository;

@ExtendWith(MockitoExtension.class)
class OrderExpiryEventListenerTest {

    @Mock
    private MasterOrderRepositoryPort masterOrderRepository;

    @Mock
    private SubOrderRepositoryPort subOrderRepository;

    @Mock
    private OrderEventPublisherPort orderEventPublisher;

    private OrderExpiryEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderExpiryEventListener(masterOrderRepository, subOrderRepository, orderEventPublisher);
    }

    @Test
    @DisplayName("When order is in PENDING_PAYMENT, cancelling due to expiry cancels order and suborders")
    void handleBookingHoldExpired_WhenPendingPayment_ShouldCancelOrderAndSubOrders() {
        UUID bookingId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        MasterOrder order = new MasterOrder();
        order.setId(orderId);
        order.setBookingId(bookingId);
        order.setCustomerId(UUID.randomUUID());
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);

        SubOrder subOrder = new SubOrder();
        subOrder.setId(UUID.randomUUID());
        subOrder.setMasterOrderId(orderId);
        subOrder.setStatus(SubOrderStatus.PENDING);
        subOrder.setSubtotalAmount(new BigDecimal("100000"));

        when(masterOrderRepository.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(order));
        when(subOrderRepository.findByMasterOrderId(orderId)).thenReturn(List.of(subOrder));

        BookingHoldExpiredEvent event = new BookingHoldExpiredEvent(bookingId, OffsetDateTime.now());
        listener.handleBookingHoldExpired(event);

        ArgumentCaptor<MasterOrder> orderCaptor = ArgumentCaptor.forClass(MasterOrder.class);
        verify(masterOrderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(MasterOrderStatus.CANCELLED);

        verify(subOrderRepository).saveAll(any());
        verify(orderEventPublisher).publishOrderCancelledEvent(any(MasterOrder.class));
    }

    @Test
    @DisplayName("When order is already PAID (race condition), expiry event must NOT cancel the order")
    void handleBookingHoldExpired_WhenOrderAlreadyPaid_ShouldIgnoreExpiry() {
        UUID bookingId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        MasterOrder order = new MasterOrder();
        order.setId(orderId);
        order.setBookingId(bookingId);
        order.setCustomerId(UUID.randomUUID());
        order.setStatus(MasterOrderStatus.PAID);

        when(masterOrderRepository.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(order));

        BookingHoldExpiredEvent event = new BookingHoldExpiredEvent(bookingId, OffsetDateTime.now());
        listener.handleBookingHoldExpired(event);

        verify(masterOrderRepository, never()).save(any());
        verify(subOrderRepository, never()).saveAll(any());
        verify(orderEventPublisher, never()).publishOrderCancelledEvent(any());
        assertThat(order.getStatus()).isEqualTo(MasterOrderStatus.PAID);
    }

    @Test
    @DisplayName("When order is not found for bookingId, does nothing")
    void handleBookingHoldExpired_WhenOrderNotFound_ShouldDoNothing() {
        UUID bookingId = UUID.randomUUID();
        when(masterOrderRepository.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.empty());

        BookingHoldExpiredEvent event = new BookingHoldExpiredEvent(bookingId, OffsetDateTime.now());
        listener.handleBookingHoldExpired(event);

        verify(masterOrderRepository, never()).save(any());
        verify(subOrderRepository, never()).saveAll(any());
        verify(orderEventPublisher, never()).publishOrderCancelledEvent(any());
    }

    @Test
    @DisplayName("When order has discount code, cancelling due to expiry decrements quota and deletes redemption")
    void handleBookingHoldExpired_WhenDiscountApplied_ShouldRevertQuotaAndRedemption() {
        UUID bookingId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID discountCodeId = UUID.randomUUID();

        MasterOrder order = new MasterOrder();
        order.setId(orderId);
        order.setBookingId(bookingId);
        order.setCustomerId(UUID.randomUUID());
        order.setStatus(MasterOrderStatus.PENDING_PAYMENT);
        order.setDiscountCodeId(discountCodeId);

        JpaDiscountCodeRepository discountCodeRepo =
                mock(JpaDiscountCodeRepository.class);
        JpaDiscountRedemptionRepository redemptionRepo =
                mock(JpaDiscountRedemptionRepository.class);

        OrderExpiryEventListener listenerWithDiscount = new OrderExpiryEventListener(
                masterOrderRepository, subOrderRepository, orderEventPublisher, discountCodeRepo, redemptionRepo
        );

        when(masterOrderRepository.findByBookingIdForUpdate(bookingId)).thenReturn(Optional.of(order));
        when(subOrderRepository.findByMasterOrderId(orderId)).thenReturn(List.of());

        when(discountCodeRepo.findByIdForUpdate(discountCodeId)).thenReturn(Optional.of(
                new DiscountCodeJpaEntity()));
        when(redemptionRepo.releaseReservation(orderId, discountCodeId)).thenReturn(1);
        BookingHoldExpiredEvent event = new BookingHoldExpiredEvent(bookingId, OffsetDateTime.now());
        listenerWithDiscount.handleBookingHoldExpired(event);

        verify(discountCodeRepo).decrementUsedCount(discountCodeId);
        verify(redemptionRepo).releaseReservation(orderId, discountCodeId);
    }
}
