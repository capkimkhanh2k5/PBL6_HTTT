package com.danasea.backend.modules.ai.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.order.application.dtos.CancellationPreviewResult;

public interface CustomerOrderReadPort {
    record Item(UUID id, UUID serviceId, int quantity, BigDecimal unitPrice, BigDecimal subtotal, String status) {}
    record OwnedOrder(UUID id, String status, String paymentStatus, BigDecimal totalAmount, List<Item> items) {}
    OwnedOrder read(UUID userId, UUID orderId);
    CancellationPreviewResult previewCancellation(UUID userId, UUID orderId);
}
