package com.danasea.backend.modules.report.application.usecases;

import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.models.TimeRange;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Tiện ích hỗ trợ phân rã chu kỳ và sinh mốc thời gian (Zero-Filling Period Helper) theo múi giờ
 * Việt Nam (Asia/Ho_Chi_Minh, UTC+7).
 */
public final class TimeSeriesPeriodHelper {

    private TimeSeriesPeriodHelper() {}

    /**
     * Sinh danh sách tất cả các key chu kỳ từ ngày bắt đầu đến ngày kết thúc theo thứ tự thời gian.
     * Đảm bảo không bỏ sót bất kỳ mốc thời gian nào (phục vụ zero-filling trọn vẹn).
     *
     * @param from ngày bắt đầu
     * @param to ngày kết thúc
     * @param groupBy chu kỳ gom nhóm (DAY, WEEK, QUARTER, YEAR)
     * @return danh sách chuỗi đại diện cho từng kỳ
     */
    public static List<String> generatePeriodKeys(
            LocalDate from, LocalDate to, GroupByPeriod groupBy) {
        Objects.requireNonNull(from, "from date cannot be null");
        Objects.requireNonNull(to, "to date cannot be null");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        GroupByPeriod period = groupBy != null ? groupBy : GroupByPeriod.DAY;
        Set<String> keys = new LinkedHashSet<>();

        LocalDate current = from;
        while (!current.isAfter(to)) {
            keys.add(formatDateKey(current, period));
            current = current.plusDays(1);
        }

        return new ArrayList<>(keys);
    }

    /** Định dạng một ngày LocalDate sang key chu kỳ theo chuẩn GroupByPeriod. */
    public static String formatDateKey(LocalDate date, GroupByPeriod groupBy) {
        Objects.requireNonNull(date, "date cannot be null");
        GroupByPeriod period = groupBy != null ? groupBy : GroupByPeriod.DAY;

        return switch (period) {
            case DAY -> date.toString();
            case WEEK -> {
                int weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR);
                int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                yield String.format(Locale.ROOT, "%d-W%02d", weekBasedYear, week);
            }
            case QUARTER -> {
                int year = date.getYear();
                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                yield String.format(Locale.ROOT, "%d-Q%d", year, quarter);
            }
            case YEAR -> String.valueOf(date.getYear());
        };
    }

    /**
     * Chuyển đổi một mốc thời điểm OffsetDateTime sang key chu kỳ theo múi giờ Việt Nam (UTC+7).
     */
    public static String formatDateTimeKey(OffsetDateTime dateTime, GroupByPeriod groupBy) {
        Objects.requireNonNull(dateTime, "dateTime cannot be null");
        LocalDate localDate = dateTime.atZoneSameInstant(TimeRange.VIETNAM_ZONE).toLocalDate();
        return formatDateKey(localDate, groupBy);
    }
}
