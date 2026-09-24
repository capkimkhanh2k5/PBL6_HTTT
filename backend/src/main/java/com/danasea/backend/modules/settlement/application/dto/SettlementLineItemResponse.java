package com.danasea.backend.modules.settlement.application.dto;

import com.danasea.backend.modules.settlement.domain.models.LineItemExclusionReason;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record SettlementLineItemResponse(
    UUID id,
    UUID subOrderId,
    BigDecimal grossAmount,
    BigDecimal refundAmount,
    BigDecimal commissionRate,
    BigDecimal commissionAmount,
    BigDecimal netAmount,
    LineItemExclusionReason excludedReason,
    boolean isExcluded
) {}
