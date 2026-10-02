package com.danasea.backend.modules.order.domain.events;

import java.util.UUID;

public record PaymentSuccessEvent(UUID orderId, UUID customerId, UUID bookingId) {}
