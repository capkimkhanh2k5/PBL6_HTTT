package com.danasea.backend.modules.report.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@DisplayName("TimeSeriesPeriodHelper Unit & Boundary Tests")
class TimeSeriesPeriodHelperTest {

    @Test
    @DisplayName("Sinh danh sách key theo DAY (bao gồm đầy đủ các ngày liên tiếp)")
    void shouldGenerateDailyPeriodKeys() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 5);

        List<String> keys = TimeSeriesPeriodHelper.generatePeriodKeys(from, to, GroupByPeriod.DAY);

        assertThat(keys)
                .containsExactly(
                        "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04", "2026-10-05");
    }

    @Test
    @DisplayName("Sinh danh sách key cho một ngày duy nhất (from == to)")
    void shouldGenerateSingleDayKey() {
        LocalDate date = LocalDate.of(2026, 10, 8);

        List<String> keys =
                TimeSeriesPeriodHelper.generatePeriodKeys(date, date, GroupByPeriod.DAY);

        assertThat(keys).containsExactly("2026-10-08");
    }

    @Test
    @DisplayName("Sinh danh sách key theo WEEK qua các tuần liên tiếp")
    void shouldGenerateWeeklyPeriodKeys() {
        LocalDate from = LocalDate.of(2026, 9, 28); // Tuần 40
        LocalDate to = LocalDate.of(2026, 10, 12); // Tuần 42

        List<String> keys = TimeSeriesPeriodHelper.generatePeriodKeys(from, to, GroupByPeriod.WEEK);

        assertThat(keys).containsExactly("2026-W40", "2026-W41", "2026-W42");
    }

    @Test
    @DisplayName("Sinh danh sách key theo QUARTER bao gồm đầy đủ 4 quý")
    void shouldGenerateQuarterlyPeriodKeys() {
        LocalDate from = LocalDate.of(2026, 1, 15);
        LocalDate to = LocalDate.of(2026, 11, 20);

        List<String> keys =
                TimeSeriesPeriodHelper.generatePeriodKeys(from, to, GroupByPeriod.QUARTER);

        assertThat(keys).containsExactly("2026-Q1", "2026-Q2", "2026-Q3", "2026-Q4");
    }

    @Test
    @DisplayName("Sinh danh sách key theo YEAR qua các năm liên tiếp")
    void shouldGenerateYearlyPeriodKeys() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        LocalDate to = LocalDate.of(2026, 3, 1);

        List<String> keys = TimeSeriesPeriodHelper.generatePeriodKeys(from, to, GroupByPeriod.YEAR);

        assertThat(keys).containsExactly("2024", "2025", "2026");
    }

    @Test
    @DisplayName(
            "Chuyển đổi OffsetDateTime sang múi giờ Asia/Ho_Chi_Minh (UTC+7) chuẩn xác tại biên"
                    + " giờ")
    void shouldFormatDateTimeKeyInVietnamTimezone() {
        // 2026-10-01 17:30 UTC -> 2026-10-02 00:30 UTC+7 (sang ngày mới theo giờ VN)
        OffsetDateTime utcDateTime = OffsetDateTime.of(2026, 10, 1, 17, 30, 0, 0, ZoneOffset.UTC);

        String dayKey = TimeSeriesPeriodHelper.formatDateTimeKey(utcDateTime, GroupByPeriod.DAY);
        assertThat(dayKey).isEqualTo("2026-10-02");

        // 2026-10-01 16:59 UTC -> 2026-10-01 23:59 UTC+7 (vẫn là ngày 01 theo giờ VN)
        OffsetDateTime beforeMidnight =
                OffsetDateTime.of(2026, 10, 1, 16, 59, 0, 0, ZoneOffset.UTC);
        assertThat(TimeSeriesPeriodHelper.formatDateTimeKey(beforeMidnight, GroupByPeriod.DAY))
                .isEqualTo("2026-10-01");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi from sau to hoặc giá trị null")
    void shouldThrowExceptionForInvalidDates() {
        LocalDate from = LocalDate.of(2026, 10, 10);
        LocalDate to = LocalDate.of(2026, 10, 5);

        assertThatThrownBy(
                        () ->
                                TimeSeriesPeriodHelper.generatePeriodKeys(
                                        from, to, GroupByPeriod.DAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("From date cannot be after to date");

        assertThatThrownBy(
                        () ->
                                TimeSeriesPeriodHelper.generatePeriodKeys(
                                        null, to, GroupByPeriod.DAY))
                .isInstanceOf(NullPointerException.class);
    }
}
