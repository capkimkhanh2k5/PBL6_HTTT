package com.danasea.backend.modules.order.presentation.dtos;

import com.danasea.backend.modules.order.domain.models.RefundReason;

public record RefundRequest(RefundReason reason) {
}
