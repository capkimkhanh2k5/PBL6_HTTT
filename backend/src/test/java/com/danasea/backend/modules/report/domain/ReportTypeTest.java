package com.danasea.backend.modules.report.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.danasea.backend.modules.report.domain.enums.ReportType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayName("ReportType Enum Tests")
class ReportTypeTest {

    @ParameterizedTest
    @CsvSource({
        "revenue, REVENUE",
        "REVENUE, REVENUE",
        "Revenue, REVENUE",
        "bookings, BOOKINGS",
        "BOOKINGS, BOOKINGS",
        "vendors, VENDORS",
        "VENDORS, VENDORS"
    })
    @DisplayName("Chuyển đổi chuỗi hợp lệ thành ReportType")
    void shouldParseValidStrings(String input, ReportType expected) {
        assertThat(ReportType.fromString(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Ném ngoại lệ khi chuỗi type null hoặc rỗng")
    void shouldThrowExceptionWhenNullOrEmpty(String input) {
        assertThatThrownBy(() -> ReportType.fromString(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Report type cannot be null or blank");
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi chuỗi type không được hỗ trợ")
    void shouldThrowExceptionForInvalidReportType() {
        assertThatThrownBy(() -> ReportType.fromString("unknown_type"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported report type");
    }
}
