package com.danasea.backend.modules.order.application.api;

import java.util.UUID;

public interface AiOrderRiskReadApi {
    record Facts(UUID orderId, UUID customerId, long failedPayments24h, long orders1h,
                 long refundRequests7d, String paymentStatus) {}
    Facts read(UUID orderId);
}
