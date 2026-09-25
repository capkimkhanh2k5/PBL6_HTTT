package com.danasea.backend.modules.order.domain.models;

/**
 * Trạng thái của đơn hàng thành phần (SubOrder).
 *
 * Bổ sung theo EPIC-07:
 * - PARTIALLY_REFUNDED: Đơn hàng được hoàn tiền một phần (sau khi giải quyết khiếu nại).
 * - CHECKED_IN: Khách hàng đã được nhân viên Vendor quét mã QR check-in tại điểm xuất phát.
 * - IN_PROGRESS: Chuyến đi/dịch vụ đang diễn ra.
 */
public enum SubOrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CHECKED_IN,
    IN_PROGRESS;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED || this == PARTIALLY_REFUNDED || this == REJECTED;
    }

    public boolean canCheckIn() {
        return this == CONFIRMED;
    }

    public boolean canDispute() {
        return this == COMPLETED || this == CONFIRMED;
    }
}
