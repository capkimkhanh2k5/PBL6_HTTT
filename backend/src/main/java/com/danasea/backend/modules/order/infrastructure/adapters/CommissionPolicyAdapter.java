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
        // Mặc định áp dụng tỷ lệ hoa hồng chuẩn của hệ thống (10%)
        return CommissionPolicyPort.DEFAULT_COMMISSION_RATE;
    }
}
