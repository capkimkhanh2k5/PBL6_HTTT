package com.danasea.backend.modules.report.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.danasea.backend.modules.report.domain.models.TimeRange;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@DisplayName("TimeRange Domain Value Object Tests")
class TimeRangeTest {

    private static final ZoneOffset VN_OFFSET = ZoneOffset.ofHours(7);

    @Test
    @DisplayName("Tạo TimeRange hợp lệ với múi giờ Asia/Ho_Chi_Minh (UTC+7)")
    void shouldCreateValidTimeRangeWithVietnamTimezone() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 8);

        TimeRange timeRange = TimeRange.of(from, to);

        assertThat(timeRange.getFrom()).isEqualTo(from);
        assertThat(timeRange.getTo()).isEqualTo(to);

        // startDateTime phải là 00:00:00+07:00
        OffsetDateTime start = timeRange.getStartDateTime();
        assertThat(start.getYear()).isEqualTo(2026);
        assertThat(start.getMonthValue()).isEqualTo(10);
        assertThat(start.getDayOfMonth()).isEqualTo(1);
        assertThat(start.getHour()).isZero();
        assertThat(start.getMinute()).isZero();
        assertThat(start.getSecond()).isZero();
        assertThat(start.getOffset()).isEqualTo(VN_OFFSET);

        // endDateTime phải là 23:59:59.999999999+07:00
        OffsetDateTime end = timeRange.getEndDateTime();
        assertThat(end.getYear()).isEqualTo(2026);
        assertThat(end.getMonthValue()).isEqualTo(10);
        assertThat(end.getDayOfMonth()).isEqualTo(8);
        assertThat(end.getHour()).isEqualTo(23);
        assertThat(end.getMinute()).isEqualTo(59);
        assertThat(end.getSecond()).isEqualTo(59);
        assertThat(end.getOffset()).isEqualTo(VN_OFFSET);

        assertThat(timeRange.getDaysCount()).isEqualTo(8);
    }

    @Test
    @DisplayName("TimeRange cho một ngày duy nhất (from = to)")
    void shouldSupportSingleDayRange() {
        LocalDate singleDay = LocalDate.of(2026, 10, 5);
        TimeRange timeRange = TimeRange.of(singleDay, singleDay);

        assertThat(timeRange.getFrom()).isEqualTo(singleDay);
        assertThat(timeRange.getTo()).isEqualTo(singleDay);
        assertThat(timeRange.getDaysCount()).isEqualTo(1);
        assertThat(timeRange.getStartDateTime().getDayOfMonth()).isEqualTo(5);
        assertThat(timeRange.getEndDateTime().getDayOfMonth()).isEqualTo(5);
    }

    @Test
    @DisplayName("Ném ngoại lệ IllegalArgumentException khi from sau to")
    void shouldThrowExceptionWhenFromIsAfterTo() {
        LocalDate from = LocalDate.of(2026, 10, 8);
        LocalDate to = LocalDate.of(2026, 10, 1);

        assertThatThrownBy(() -> TimeRange.of(from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("From date cannot be after to date");
    }

    @Test
    @DisplayName("Ném ngoại lệ IllegalArgumentException khi from hoặc to bị null")
    void shouldThrowExceptionWhenDatesAreNull() {
        LocalDate validDate = LocalDate.of(2026, 10, 1);

        assertThatThrownBy(() -> TimeRange.of(null, validDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("From date and to date cannot be null");

        assertThatThrownBy(() -> TimeRange.of(validDate, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("From date and to date cannot be null");
    }

    @Test
    @DisplayName("Kiểm tra phương thức contains đối với OffsetDateTime")
    void shouldCheckContainsForOffsetDateTime() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 3);
        TimeRange timeRange = TimeRange.of(from, to);

        // Đúng thời điểm bắt đầu và kết thúc
        assertThat(timeRange.contains(timeRange.getStartDateTime())).isTrue();
        assertThat(timeRange.contains(timeRange.getEndDateTime())).isTrue();

        // Ở giữa
        OffsetDateTime middle = OffsetDateTime.of(2026, 10, 2, 12, 0, 0, 0, VN_OFFSET);
        assertThat(timeRange.contains(middle)).isTrue();

        // Ngoài khoảng (trước start và sau end)
        OffsetDateTime before = timeRange.getStartDateTime().minusNanos(1);
        OffsetDateTime after = timeRange.getEndDateTime().plusNanos(1);
        assertThat(timeRange.contains(before)).isFalse();
        assertThat(timeRange.contains(after)).isFalse();
        assertThat(timeRange.contains((OffsetDateTime) null)).isFalse();
    }

    @Test
    @DisplayName("Kiểm tra phương thức contains đối với LocalDate")
    void shouldCheckContainsForLocalDate() {
        TimeRange timeRange = TimeRange.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

        assertThat(timeRange.contains(LocalDate.of(2026, 10, 1))).isTrue();
        assertThat(timeRange.contains(LocalDate.of(2026, 10, 3))).isTrue();
        assertThat(timeRange.contains(LocalDate.of(2026, 10, 5))).isTrue();
        assertThat(timeRange.contains(LocalDate.of(2026, 9, 30))).isFalse();
        assertThat(timeRange.contains(LocalDate.of(2026, 10, 6))).isFalse();
        assertThat(timeRange.contains((LocalDate) null)).isFalse();
    }

    @Test
    @DisplayName("Kiểm tra tính nhất quán equals và hashCode")
    void shouldCheckEqualsAndHashCode() {
        TimeRange r1 = TimeRange.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        TimeRange r2 = TimeRange.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        TimeRange r3 = TimeRange.of(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 5));

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1).isNotEqualTo(r3);
    }
}
