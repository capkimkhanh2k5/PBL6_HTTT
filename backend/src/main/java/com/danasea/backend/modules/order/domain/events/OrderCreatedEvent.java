package com.danasea.backend.modules.order.domain.events;

import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, UUID customerId, UUID bookingId) {}
