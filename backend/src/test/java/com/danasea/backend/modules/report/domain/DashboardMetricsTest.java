package com.danasea.backend.modules.report.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.DashboardMetrics;
import com.danasea.backend.modules.report.domain.models.VendorDiagnosisAlert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@DisplayName("DashboardMetrics Domain Model Tests")
class DashboardMetricsTest {

    @Test
    @DisplayName("Tính toán tỷ lệ hoàn tất và tỷ lệ hủy chính xác")
    void shouldCalculateRatesAccurately() {
        VendorDiagnosisAlert alert =
                new VendorDiagnosisAlert(
                        UUID.randomUUID(),
                        "Vendor Test",
                        VendorIssueType.HIGH_CANCELLATION,
                        VendorAlertSeverity.WARNING,
                        "Tỷ lệ hủy cao",
                        25.0);

        DashboardMetrics metrics =
                DashboardMetrics.of(
                        BigDecimal.valueOf(100_000_000),
                        BigDecimal.valueOf(85_000_000),
                        100, // new/total orders
                        80, // completed
                        15, // cancelled
                        List.of(alert));

        assertThat(metrics.getTotalRevenue()).isEqualByComparingTo("100000000");
        assertThat(metrics.getNetRevenue()).isEqualByComparingTo("85000000");
        assertThat(metrics.getNewOrders()).isEqualTo(100);
        assertThat(metrics.getCompletedOrders()).isEqualTo(80);
        assertThat(metrics.getCancelledOrders()).isEqualTo(15);
        assertThat(metrics.getCompletionRate()).isEqualTo(80.0);
        assertThat(metrics.getCancellationRate()).isEqualTo(15.0);
        assertThat(metrics.getAlerts()).hasSize(1);
    }

    @Test
    @DisplayName("An toàn khi tổng số đơn = 0 (tránh lỗi chia cho 0)")
    void shouldHandleZeroOrdersWithoutDivisionByZero() {
        DashboardMetrics metrics =
                DashboardMetrics.of(BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, List.of());

        assertThat(metrics.getCompletionRate()).isEqualTo(0.0);
        assertThat(metrics.getCancellationRate()).isEqualTo(0.0);
        assertThat(metrics.getAlerts()).isEmpty();
    }

    @Test
    @DisplayName("Tạo DashboardMetrics rỗng")
    void shouldCreateEmptyDashboardMetrics() {
        DashboardMetrics metrics = DashboardMetrics.empty();

        assertThat(metrics.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.getNetRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.getNewOrders()).isZero();
        assertThat(metrics.getCompletedOrders()).isZero();
        assertThat(metrics.getCancelledOrders()).isZero();
        assertThat(metrics.getCompletionRate()).isEqualTo(0.0);
        assertThat(metrics.getCancellationRate()).isEqualTo(0.0);
        assertThat(metrics.getAlerts()).isEmpty();
    }

    @Test
    @DisplayName("Danh sách alerts là bất biến (Unmodifiable)")
    void shouldHaveUnmodifiableAlertsList() {
        DashboardMetrics metrics =
                DashboardMetrics.of(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0,
                        0,
                        0,
                        List.of(
                                new VendorDiagnosisAlert(
                                        UUID.randomUUID(),
                                        "V1",
                                        VendorIssueType.LOW_RATING,
                                        VendorAlertSeverity.WARNING,
                                        "Rating thấp",
                                        3.2)));

        assertThatThrownBy(
                        () ->
                                metrics.getAlerts()
                                        .add(
                                                new VendorDiagnosisAlert(
                                                        UUID.randomUUID(),
                                                        "V2",
                                                        VendorIssueType.OVERLOADED,
                                                        VendorAlertSeverity.WARNING,
                                                        "Overloaded",
                                                        98.0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
