package com.danasea.backend.modules.order.application.dtos;

import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;

public record CreatePaymentIntentCommand(
        UUID customerId,
        UUID orderId,
        PaymentProvider provider,
        String idempotencyKey
) {}
