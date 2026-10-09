package com.danasea.backend.modules.order.infrastructure.listeners;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.danasea.backend.modules.booking.domain.events.BookingHoldExpiredEvent;
import com.danasea.backend.modules.order.application.DiscountReservationService;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.OrderEventPublisherPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountCodeRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountRedemptionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Listener nhận sự kiện hết hạn giữ chỗ (BookingHoldExpiredEvent) từ module Booking,
 * đóng vai trò Single Source of Truth cho cơ chế xử lý Payment Timeout đồng bộ giữa Booking và Order.
 */
@Component
@Slf4j
public class OrderExpiryEventListener {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final SubOrderRepositoryPort subOrderRepository;
    private final OrderEventPublisherPort orderEventPublisher;
    private final DiscountReservationService reservations;

    @Autowired
    public OrderExpiryEventListener(
            MasterOrderRepositoryPort masterOrderRepository,
            SubOrderRepositoryPort subOrderRepository,
            OrderEventPublisherPort orderEventPublisher,
            DiscountReservationService reservations) {
        this.masterOrderRepository = masterOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.reservations = reservations;
    }

    public OrderExpiryEventListener(MasterOrderRepositoryPort orders, SubOrderRepositoryPort subOrders,
            OrderEventPublisherPort events, JpaDiscountCodeRepository codes, JpaDiscountRedemptionRepository redemptions) {
        this(orders, subOrders, events, codes != null && redemptions != null
                ? new DiscountReservationService(codes, redemptions) : null);
    }

    public OrderExpiryEventListener(MasterOrderRepositoryPort orders, SubOrderRepositoryPort subOrders,
            OrderEventPublisherPort events) {
        this(orders, subOrders, events, (DiscountReservationService) null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBookingHoldExpired(BookingHoldExpiredEvent event) {
        if (event == null || event.bookingId() == null) {
            return;
        }

        UUID bookingId = event.bookingId();
        log.info("Received BookingHoldExpiredEvent for bookingId: {}", bookingId);

        masterOrderRepository.findByBookingIdForUpdate(bookingId).ifPresent(order -> {
            if (order.getStatus() == MasterOrderStatus.PENDING_PAYMENT) {
                log.info("Cancelling order {} due to booking hold expiry", order.getId());

                List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(order.getId());
                order.setSubOrders(subOrders);
                order.cancelDueToExpiry();

                if (reservations != null) {
                    reservations.release(order.getId(), order.getDiscountCodeId());
                }

                masterOrderRepository.save(order);
                subOrderRepository.saveAll(order.getSubOrders());
                orderEventPublisher.publishOrderCancelledEvent(order);

                log.info("Successfully cancelled order {} and sub-orders due to booking hold expiry", order.getId());
            } else {
                log.debug("Order {} for bookingId {} is in status {}, skipping timeout cancellation",
                        order.getId(), bookingId, order.getStatus());
            }
        });
    }
}
