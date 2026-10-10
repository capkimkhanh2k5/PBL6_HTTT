package com.danasea.backend.modules.order.domain.services;

import com.danasea.backend.modules.order.domain.models.MasterOrderStatus;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;

public final class CustomerRefundEligibilityPolicy {
    private CustomerRefundEligibilityPolicy() {}
    public static boolean paidOrder(MasterOrderStatus status) {
        return status == MasterOrderStatus.PAID || status == MasterOrderStatus.PARTIALLY_COMPLETED || status == MasterOrderStatus.COMPLETED;
    }
    public static boolean refundableItem(SubOrderStatus status) {
        return status != null && status != SubOrderStatus.CANCELLED && status != SubOrderStatus.REFUNDED && status != SubOrderStatus.REJECTED;
    }
}
