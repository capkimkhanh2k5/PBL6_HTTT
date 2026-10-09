package com.danasea.backend.modules.report.domain.enums;

/** Loại báo cáo hỗ trợ truy xuất và xuất file CSV. */
public enum ReportType {
    REVENUE,
    BOOKINGS,
    VENDORS;

    /** Chuyển đổi an toàn từ chuỗi (case-insensitive). */
    public static ReportType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Report type cannot be null or blank");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported report type: "
                            + value
                            + ". Supported values: revenue, bookings, vendors");
        }
    }
}
