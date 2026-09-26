package com.danasea.backend.modules.order.infrastructure.listeners;

import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.booking.domain.events.BookingHoldExpiredEvent;
import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.ports.MasterOrderRepositoryPort;
import com.danasea.backend.modules.order.domain.ports.OrderEventPublisherPort;
import com.danasea.backend.modules.order.domain.ports.SubOrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener nhận sự kiện hết hạn giữ chỗ (BookingHoldExpiredEvent) từ module Booking,
 * đóng vai trò Single Source of Truth cho Payment Timeout (Mục 9.2.10).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryEventListener {

    private final MasterOrderRepositoryPort masterOrderRepository;
    private final SubOrderRepositoryPort subOrderRepository;
    private final OrderEventPublisherPort orderEventPublisher;

    @EventListener
    @Transactional
    public void handleBookingHoldExpired(BookingHoldExpiredEvent event) {
        if (event == null || event.bookingId() == null) {
            return;
        }

        UUID bookingId = event.bookingId();
        log.info("Received BookingHoldExpiredEvent for bookingId: {}", bookingId);

        masterOrderRepository.findByBookingId(bookingId).ifPresent(order -> {
            if (order.getStatus() == MasterOrderStatus.PENDING_PAYMENT) {
                log.info("Cancelling order {} due to booking hold expiry", order.getId());

                List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(order.getId());
                order.setSubOrders(subOrders);
                order.cancelDueToExpiry();

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
