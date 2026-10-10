package com.danasea.backend.modules.ai.application.ports;

import java.util.UUID;

public interface TransactionRiskReadPort {
    record Facts(UUID orderId, UUID customerId, long failedPayments24h, long orders1h,
                 long refundRequests7d, String paymentStatus) {}
    Facts read(UUID orderId);
}
