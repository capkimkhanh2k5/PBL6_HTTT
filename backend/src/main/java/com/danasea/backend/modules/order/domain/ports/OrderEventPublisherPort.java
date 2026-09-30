package com.danasea.backend.modules.order.domain.ports;

import com.danasea.backend.modules.order.domain.models.MasterOrder;

/**
 * Domain Port phát sự kiện đơn hàng và thanh toán.
 */
public interface OrderEventPublisherPort {

    void publishOrderCreatedEvent(MasterOrder order);

    void publishPaymentSuccessEvent(MasterOrder order);

    void publishOrderCancelledEvent(MasterOrder order);
}
