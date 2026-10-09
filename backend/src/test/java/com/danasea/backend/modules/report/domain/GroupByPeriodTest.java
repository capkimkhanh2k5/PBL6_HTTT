package com.danasea.backend.modules.report.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayName("GroupByPeriod Enum Tests")
class GroupByPeriodTest {

    @ParameterizedTest
    @CsvSource({
        "day, DAY",
        "DAY, DAY",
        "Day, DAY",
        "week, WEEK",
        "WEEK, WEEK",
        "quarter, QUARTER",
        "QUARTER, QUARTER",
        "year, YEAR",
        "YEAR, YEAR"
    })
    @DisplayName("Chuyển đổi chuỗi hợp lệ thành GroupByPeriod")
    void shouldParseValidStrings(String input, GroupByPeriod expected) {
        assertThat(GroupByPeriod.fromString(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Giá trị null hoặc rỗng mặc định trả về DAY")
    void shouldDefaultToDayWhenNullOrEmpty(String input) {
        assertThat(GroupByPeriod.fromString(input)).isEqualTo(GroupByPeriod.DAY);
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi chuỗi groupBy không hợp lệ")
    void shouldThrowExceptionForInvalidGroupBy() {
        assertThatThrownBy(() -> GroupByPeriod.fromString("invalid_period"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported groupBy period");
    }
}
