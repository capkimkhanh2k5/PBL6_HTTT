package com.danasea.backend.modules.order.application.api;

import java.math.BigDecimal;
import java.util.List;

public interface AiPolicyReadApi {
    record RefundTier(String condition, BigDecimal percentage) {}
    record Snapshot(String scope, List<RefundTier> customerCancellation, BigDecimal weatherOrVendorFaultPercentage,
                    String source, String applicability) {}
    Snapshot current();
}
