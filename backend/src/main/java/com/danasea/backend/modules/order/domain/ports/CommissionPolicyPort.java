package com.danasea.backend.modules.order.domain.ports;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain Port xác định tỷ lệ hoa hồng cho vendor (Mục 9.2.6).
 * Mặc định fallback là 10% (0.10).
 */
public interface CommissionPolicyPort {

    BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.10");

    BigDecimal getCommissionRate(UUID vendorId);
}
