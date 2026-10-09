package com.danasea.backend.modules.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

@DisplayName("BookingReportItem Domain Model Tests")
class BookingReportItemTest {

    @Test
    @DisplayName("Thống kê đơn đặt và phân loại theo lý do hủy chính xác")
    void shouldTrackBookingsAndCancellationReasons() {
        Map<RefundReason, Long> reasons =
                Map.of(
                        RefundReason.WEATHER, 5L,
                        RefundReason.CUSTOMER_CANCEL, 3L,
                        RefundReason.VENDOR_FAULT, 1L,
                        RefundReason.ADMIN_OVERRIDE, 1L);

        BookingReportItem item =
                new BookingReportItem(
                        "2026-10-01",
                        50, // total
                        40, // completed
                        10, // cancelled
                        reasons);

        assertThat(item.getPeriodKey()).isEqualTo("2026-10-01");
        assertThat(item.getTotalOrders()).isEqualTo(50);
        assertThat(item.getCompletedOrders()).isEqualTo(40);
        assertThat(item.getCancelledOrders()).isEqualTo(10);
        assertThat(item.getCompletionRate()).isEqualTo(80.0);
        assertThat(item.getCancellationRate()).isEqualTo(20.0);

        assertThat(item.getCancellationCount(RefundReason.WEATHER)).isEqualTo(5);
        assertThat(item.getCancellationCount(RefundReason.CUSTOMER_CANCEL)).isEqualTo(3);
        assertThat(item.getCancellationCount(RefundReason.VENDOR_FAULT)).isEqualTo(1);
        assertThat(item.getCancellationCount(RefundReason.ADMIN_OVERRIDE)).isEqualTo(1);
        assertThat(item.getCancellationCount(RefundReason.DISPUTE)).isZero();
    }

    @Test
    @DisplayName("An toàn khi totalOrders = 0 (tránh chia cho 0)")
    void shouldHandleZeroOrdersSafely() {
        BookingReportItem item = BookingReportItem.empty("2026-W40");

        assertThat(item.getPeriodKey()).isEqualTo("2026-W40");
        assertThat(item.getTotalOrders()).isZero();
        assertThat(item.getCompletedOrders()).isZero();
        assertThat(item.getCancelledOrders()).isZero();
        assertThat(item.getCompletionRate()).isEqualTo(0.0);
        assertThat(item.getCancellationRate()).isEqualTo(0.0);
        assertThat(item.getCancellationReasonBreakdown()).isEmpty();
    }
}
