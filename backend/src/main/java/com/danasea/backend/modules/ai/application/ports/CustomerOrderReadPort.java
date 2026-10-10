package com.danasea.backend.modules.ai.application.ports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.order.application.dtos.CancellationPreviewResult;

public interface CustomerOrderReadPort {
    record Item(UUID id, UUID serviceId, int quantity, BigDecimal unitPrice, BigDecimal subtotal, String status) {}
    record OwnedOrder(UUID id, String status, String paymentStatus, BigDecimal totalAmount, List<Item> items) {}
    OwnedOrder read(UUID userId, UUID orderId);
    CancellationPreviewResult previewCancellation(UUID userId, UUID orderId);
    default void validateChangeTarget(UUID userId, UUID orderId, UUID slotId, LocalDate requestedDate) {
        if (slotId != null) throw new IllegalArgumentException("Slot change preview is unavailable");
    }
    record Eligibility(boolean refundRequestAvailable, boolean changeRequestAvailable, String source, List<String> reasonCodes) {}
    default Eligibility eligibility(UUID userId, UUID orderId, CancellationPreviewResult preview) {
        return new Eligibility(false, false, "UNAVAILABLE", List.of("ELIGIBILITY_NOT_VERIFIED"));
    }
}
