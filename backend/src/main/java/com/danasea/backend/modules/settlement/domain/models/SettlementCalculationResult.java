package com.danasea.backend.modules.settlement.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCalculationResult {
    private UUID vendorId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    @Builder.Default
    private BigDecimal totalGrossRevenue = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalRefundAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalCommission = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalNetPayout = BigDecimal.ZERO;

    @Builder.Default
    private List<SettlementLineItem> lineItems = new ArrayList<>();
}
