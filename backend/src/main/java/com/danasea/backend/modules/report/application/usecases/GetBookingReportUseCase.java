package com.danasea.backend.modules.report.application.usecases;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.report.application.ports.ReportDataPort;
import com.danasea.backend.modules.report.application.ports.SubOrderRecord;
import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * UseCase tổng hợp và phân rã báo cáo đơn đặt (Booking Report) theo chu kỳ thời gian (ngày, tuần,
 * quý, năm) múi giờ Việt Nam (Asia/Ho_Chi_Minh - UTC+7).
 *
 * <p>Đảm bảo: 1. Zero-filling đầy đủ tất cả các mốc thời gian không phát sinh đơn trong khoảng
 * [from, to]. 2. Phân rã số đơn hoàn tất, số đơn hủy và phân loại chi tiết theo enum RefundReason
 * (CUSTOMER_CANCEL, WEATHER, VENDOR_FAULT, ADMIN_OVERRIDE, v.v.).
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class GetBookingReportUseCase {

    private final ReportDataPort reportDataPort;

    public GetBookingReportUseCase(ReportDataPort reportDataPort) {
        this.reportDataPort =
                Objects.requireNonNull(reportDataPort, "reportDataPort cannot be null");
    }

    /**
     * Thống kê báo cáo đơn đặt toàn hệ thống hoặc theo từng Vendor.
     *
     * @param timeRange khoảng thời gian lọc (chuẩn UTC+7)
     * @param groupBy chu kỳ gom nhóm (DAY, WEEK, QUARTER, YEAR)
     * @param vendorId ID của vendor (nếu null: toàn hệ thống cho Admin)
     * @return danh sách các điểm dữ liệu BookingReportItem đã zero-fill theo thứ tự thời gian
     */
    public List<BookingReportItem> execute(
            TimeRange timeRange, GroupByPeriod groupBy, UUID vendorId) {
        Objects.requireNonNull(timeRange, "timeRange cannot be null");
        GroupByPeriod effectiveGroupBy = groupBy != null ? groupBy : GroupByPeriod.DAY;

        // 1. Sinh đầy đủ danh sách key chu kỳ để zero-fill
        List<String> periodKeys =
                TimeSeriesPeriodHelper.generatePeriodKeys(
                        timeRange.getFrom(), timeRange.getTo(), effectiveGroupBy);

        Map<String, BookingAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (String key : periodKeys) {
            accumulatorMap.put(key, new BookingAccumulator());
        }

        // 2. Trích xuất dữ liệu từ cơ sở dữ liệu
        List<SubOrderRecord> subOrders = reportDataPort.findSubOrders(timeRange, vendorId);
        Map<UUID, RefundReason> reasons =
                BookingCancellationClassifier.reasons(reportDataPort, subOrders);

        // 3. Gom nhóm SubOrders (Tổng đơn, Hoàn tất, Hủy)
        for (SubOrderRecord subOrder : subOrders) {
            String key =
                    TimeSeriesPeriodHelper.formatDateTimeKey(
                            subOrder.createdAt(), effectiveGroupBy);
            BookingAccumulator acc = accumulatorMap.get(key);
            if (acc != null) {
                acc.totalOrders++;
                if (subOrder.status() == SubOrderStatus.COMPLETED) {
                    acc.completedOrders++;
                } else if (BookingCancellationClassifier.isCancelled(subOrder, reasons)) {
                    acc.cancelledOrders++;
                    RefundReason reason = reasons.get(subOrder.id());
                    if (reason != null) acc.breakdown.merge(reason, 1L, Long::sum);
                }
            }
        }

        // 5. Xây dựng danh sách BookingReportItem kết quả
        List<BookingReportItem> result = new ArrayList<>(periodKeys.size());
        for (Map.Entry<String, BookingAccumulator> entry : accumulatorMap.entrySet()) {
            String key = entry.getKey();
            BookingAccumulator acc = entry.getValue();

            result.add(
                    new BookingReportItem(
                            key,
                            acc.totalOrders,
                            acc.completedOrders,
                            acc.cancelledOrders,
                            acc.breakdown));
        }

        return result;
    }

    public List<BookingReportItem> execute(TimeRange timeRange, GroupByPeriod groupBy) {
        return execute(timeRange, groupBy, null);
    }

    private static class BookingAccumulator {
        long totalOrders = 0;
        long completedOrders = 0;
        long cancelledOrders = 0;
        Map<RefundReason, Long> breakdown = new EnumMap<>(RefundReason.class);
    }
}
