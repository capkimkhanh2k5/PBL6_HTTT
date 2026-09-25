package com.danasea.backend.modules.order.presentation.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(@NotNull UUID bookingId) {
}
