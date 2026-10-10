package com.danasea.backend.modules.order.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountCodeRepository;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaDiscountRedemptionRepository;

@Service
public class DiscountReservationService {
    private final JpaDiscountCodeRepository codes;
    private final JpaDiscountRedemptionRepository redemptions;

    public DiscountReservationService(JpaDiscountCodeRepository codes, JpaDiscountRedemptionRepository redemptions) {
        this.codes = codes;
        this.redemptions = redemptions;
    }

    @Transactional
    public void release(UUID orderId, UUID codeId) {
        if (codeId == null) {
            return;
        }
        codes.findByIdForUpdate(codeId).orElseThrow(() -> new IllegalStateException("Reserved discount code was not found."));
        // Only the transaction that removes the reservation may return its quota.
        if (redemptions.releaseReservation(orderId, codeId) > 0) {
            codes.decrementUsedCount(codeId);
        }
    }
}
