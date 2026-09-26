package com.danasea.backend.modules.order.infrastructure.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.domain.events.OrderCancelledEvent;
import com.danasea.backend.modules.order.domain.events.OrderCreatedEvent;
import com.danasea.backend.modules.order.domain.events.PaymentSuccessEvent;
import com.danasea.backend.modules.order.domain.models.MasterOrder;
import com.danasea.backend.modules.order.domain.ports.OrderEventPublisherPort;

@Component
public class OrderEventPublisherAdapter implements OrderEventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    public OrderEventPublisherAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publishOrderCreatedEvent(MasterOrder order) {
        if (order != null) {
            eventPublisher.publishEvent(new OrderCreatedEvent(order.getId(), order.getCustomerId(), order.getBookingId()));
        }
    }

    @Override
    public void publishPaymentSuccessEvent(MasterOrder order) {
        if (order != null) {
            eventPublisher.publishEvent(new PaymentSuccessEvent(order.getId(), order.getCustomerId(), order.getBookingId()));
        }
    }

    @Override
    public void publishOrderCancelledEvent(MasterOrder order) {
        if (order != null) {
            eventPublisher.publishEvent(new OrderCancelledEvent(order.getId(), order.getCustomerId(), order.getBookingId()));
        }
    }
}
