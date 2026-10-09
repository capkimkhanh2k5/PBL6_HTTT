package com.danasea.backend.modules.report.domain.enums;

/** Chu kỳ gom nhóm cho báo cáo thời gian (Time-series grouping period). */
public enum GroupByPeriod {
    DAY,
    WEEK,
    QUARTER,
    YEAR;

    /**
     * Chuyển đổi an toàn từ chuỗi (không phân biệt hoa thường). Mặc định là DAY nếu null hoặc rỗng.
     */
    public static GroupByPeriod fromString(String value) {
        if (value == null || value.isBlank()) {
            return DAY;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported groupBy period: "
                            + value
                            + ". Supported values: day, week, quarter, year");
        }
    }
}
