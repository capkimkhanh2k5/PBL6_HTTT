package com.danasea.backend.modules.order.domain.models;

/**
 * Trạng thái dòng tiền của MasterOrder (cột master_orders.payment_status).
 * Phân biệt với MasterOrderStatus (vòng đời fulfillment đơn hàng).
 * Gồm 4 giá trị bất biến:
 * - UNPAID: Chưa thanh toán
 * - PAID: Đã thanh toán thành công
 * - REFUNDED: Đã hoàn tiền cho khách (hủy sớm >= 24h)
 * - NO_REFUND: Hủy trễ (< 24h) mất toàn bộ tiền, không hoàn
 */
public enum PaymentOrderStatus {
    UNPAID,
    PAID,
    REFUNDED,
    NO_REFUND
}
