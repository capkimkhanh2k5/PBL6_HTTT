package com.danasea.backend.modules.settlement.domain.models;

public enum SettlementStatus {
    /** Bản nháp vừa được tạo, có thể chỉnh sửa hoặc tính toán lại */
    DRAFT,

    /** Đã chốt sổ đối soát, không thể chỉnh sửa hay tính lại (bất biến) */
    FINALIZED,

    /** Đã thực hiện giải ngân/chuyển khoản cho vendor */
    PAID,

    /** Trạng thái chờ xử lý (tương thích ngược) */
    PENDING;

    public boolean isFinalized() {
        return this == FINALIZED || this == PAID;
    }

    public boolean isImmutable() {
        return isFinalized();
    }

    public boolean canRegenerate() {
        return this == DRAFT || this == PENDING;
    }

    public boolean isPaid() {
        return this == PAID;
    }
}
