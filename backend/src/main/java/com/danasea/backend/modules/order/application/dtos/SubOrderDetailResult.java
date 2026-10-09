package com.danasea.backend.modules.order.application.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.modules.order.domain.models.SubOrder;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;

public record SubOrderDetailResult(
        UUID id,
        UUID bookingItemId,
        UUID masterOrderId,
        UUID vendorId,
        UUID serviceId,
        UUID slotId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal vendorDiscountAmount,
        BigDecimal platformDiscountAmount,
        BigDecimal finalAmount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal vendorPayoutAmount,
        SubOrderStatus status,
        Boolean waiverAccepted,
        OffsetDateTime vendorNotifiedAt,
        OffsetDateTime createdAt
) {
    public SubOrderDetailResult(
            UUID id,
            UUID bookingItemId,
            UUID masterOrderId,
            UUID vendorId,
            UUID serviceId,
            UUID slotId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotalAmount,
            BigDecimal commissionRate,
            BigDecimal commissionAmount,
            BigDecimal vendorPayoutAmount,
            SubOrderStatus status,
            Boolean waiverAccepted,
            OffsetDateTime vendorNotifiedAt,
            OffsetDateTime createdAt
    ) {
        this(id, bookingItemId, masterOrderId, vendorId, serviceId, slotId, quantity, unitPrice, subtotalAmount,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, subtotalAmount,
                commissionRate, commissionAmount, vendorPayoutAmount, status, waiverAccepted, vendorNotifiedAt, createdAt);
    }

    public static SubOrderDetailResult fromDomain(SubOrder subOrder) {
        if (subOrder == null) {
            return null;
        }
        return new SubOrderDetailResult(
                subOrder.getId(),
                subOrder.getBookingItemId(),
                subOrder.getMasterOrderId(),
                subOrder.getVendorId(),
                subOrder.getServiceId(),
                subOrder.getSlotId(),
                subOrder.getQuantity(),
                subOrder.getUnitPrice(),
                subOrder.getSubtotalAmount(),
                subOrder.getDiscountAmount() != null ? subOrder.getDiscountAmount() : BigDecimal.ZERO,
                subOrder.getVendorDiscountAmount() != null ? subOrder.getVendorDiscountAmount() : BigDecimal.ZERO,
                subOrder.getPlatformDiscountAmount() != null ? subOrder.getPlatformDiscountAmount() : BigDecimal.ZERO,
                subOrder.getFinalAmount(),
                subOrder.getCommissionRate(),
                subOrder.getCommissionAmount(),
                subOrder.getVendorPayoutAmount(),
                subOrder.getStatus(),
                subOrder.getWaiverAccepted(),
                subOrder.getVendorNotifiedAt(),
                subOrder.getCreatedAt()
        );
    }
}
