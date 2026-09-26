package com.danasea.backend.modules.order.infrastructure.adapters;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.order.domain.ports.CommissionPolicyPort;
import com.danasea.backend.modules.vendor.application.api.VendorInternalApi;

@Component
public class CommissionPolicyAdapter implements CommissionPolicyPort {

    private final VendorInternalApi vendorInternalApi;

    public CommissionPolicyAdapter(VendorInternalApi vendorInternalApi) {
        this.vendorInternalApi = vendorInternalApi;
    }

    @Override
    public BigDecimal getCommissionRate(UUID vendorId) {
        // Ưu tiên fallback về cấu hình hệ thống mặc định 10% (0.10) per Mục 9.2.6
        return CommissionPolicyPort.DEFAULT_COMMISSION_RATE;
    }
}
