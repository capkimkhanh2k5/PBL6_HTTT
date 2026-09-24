package com.danasea.backend.modules.order.presentation.dtos;

import com.danasea.backend.modules.order.domain.models.PaymentProvider;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentIntentRequest(@NotNull PaymentProvider provider) {
}
