package com.danasea.backend.modules.dispute.domain.models;

/**
 * Trạng thái vòng đời của một khiếu nại (Dispute).
 */
public enum DisputeStatus {
    OPEN,
    UNDER_REVIEW,
    RESOLVED_REFUND,
    RESOLVED_REJECTED,
    RESOLVED_PARTIAL;

    /**
     * Kiểm tra xem Dispute có đang active (chờ giải quyết) hay không.
     */
    public boolean isActive() {
        return this == OPEN || this == UNDER_REVIEW;
    }

    /**
     * Kiểm tra xem Dispute đã kết thúc phân xử hay chưa.
     */
    public boolean isResolved() {
        return this == RESOLVED_REFUND || this == RESOLVED_REJECTED || this == RESOLVED_PARTIAL;
    }
}
