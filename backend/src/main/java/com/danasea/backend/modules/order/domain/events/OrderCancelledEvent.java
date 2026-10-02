package com.danasea.backend.modules.order.domain.events;

import java.util.UUID;

public record OrderCancelledEvent(UUID orderId, UUID customerId, UUID bookingId) {}
