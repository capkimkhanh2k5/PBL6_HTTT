package com.danasea.backend.modules.settlement.domain.models;

public enum LineItemExclusionReason {
    /** Không bị loại trừ, được tính toán doanh thu và hoa hồng bình thường */
    NONE,

    /** Đơn hàng đã hoàn tiền 100% (do thời tiết, vendor hủy, khách hủy sớm) */
    FULL_REFUND,

    /** Đơn hàng đang có khiếu nại chưa giải quyết (OPEN hoặc UNDER_REVIEW) */
    ACTIVE_DISPUTE,

    /** Đơn hàng chưa hoàn thành dịch vụ hoặc không hợp lệ */
    NOT_ELIGIBLE;

    public boolean isExcluded() {
        return this != NONE;
    }
}
