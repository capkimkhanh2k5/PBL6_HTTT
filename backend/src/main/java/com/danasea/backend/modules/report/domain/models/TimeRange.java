package com.danasea.backend.modules.report.domain.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Value Object biểu diễn khoảng thời gian truy vấn báo cáo. Chuẩn hóa theo múi giờ Việt Nam
 * (Asia/Ho_Chi_Minh, UTC+7), bao phủ từ 00:00:00.000000000 ngày bắt đầu đến 23:59:59.999999999 ngày
 * kết thúc.
 */
public final class TimeRange {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LocalDate from;
    private final LocalDate to;
    private final OffsetDateTime startDateTime;
    private final OffsetDateTime endDateTime;

    private TimeRange(
            LocalDate from,
            LocalDate to,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime) {
        this.from = from;
        this.to = to;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    /**
     * Tạo TimeRange từ hai mốc ngày LocalDate.
     *
     * @param from ngày bắt đầu (không được null)
     * @param to ngày kết thúc (không được null và không được trước ngày bắt đầu)
     * @return đối tượng TimeRange chuẩn hóa
     * @throws IllegalArgumentException nếu ngày null hoặc from > to
     */
    public static TimeRange of(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("From date and to date cannot be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        if (ChronoUnit.DAYS.between(from, to) >= 3660) {
            throw new IllegalArgumentException("Report range cannot exceed 3660 days");
        }

        OffsetDateTime startDateTime = from.atStartOfDay(VIETNAM_ZONE).toOffsetDateTime();
        OffsetDateTime endDateTime =
                to.atTime(LocalTime.MAX).atZone(VIETNAM_ZONE).toOffsetDateTime();

        return new TimeRange(from, to, startDateTime, endDateTime);
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    /** SQL upper bound: use timestamp < endExclusiveDateTime. */
    public OffsetDateTime getEndExclusiveDateTime() {
        return to.plusDays(1).atStartOfDay(VIETNAM_ZONE).toOffsetDateTime();
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    /** Kiểm tra một thời điểm OffsetDateTime có nằm trong khoảng thời gian này hay không. */
    public boolean contains(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return !dateTime.isBefore(startDateTime) && dateTime.isBefore(getEndExclusiveDateTime());
    }

    /** Kiểm tra một ngày LocalDate có nằm trong khoảng thời gian này hay không. */
    public boolean contains(LocalDate date) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(from) && !date.isAfter(to);
    }

    /** Số ngày trong khoảng thời gian (bao gồm cả ngày bắt đầu và kết thúc). */
    public long getDaysCount() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeRange timeRange = (TimeRange) o;
        return Objects.equals(from, timeRange.from)
                && Objects.equals(to, timeRange.to)
                && Objects.equals(startDateTime, timeRange.startDateTime)
                && Objects.equals(endDateTime, timeRange.endDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, startDateTime, endDateTime);
    }

    @Override
    public String toString() {
        return "TimeRange{"
                + "from="
                + from
                + ", to="
                + to
                + ", startDateTime="
                + startDateTime
                + ", endDateTime="
                + endDateTime
                + '}';
    }
}
