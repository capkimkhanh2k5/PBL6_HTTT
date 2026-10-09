package com.danasea.backend.modules.report.application.usecases;

import com.danasea.backend.modules.report.application.ports.ReportDataPort;
import com.danasea.backend.modules.report.domain.enums.GroupByPeriod;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * UseCase tổng hợp và phân rã báo cáo doanh thu time-series theo chu kỳ (ngày, tuần, quý, năm) múi
 * giờ Việt Nam (Asia/Ho_Chi_Minh - UTC+7).
 *
 * <p>Đảm bảo: 1. Zero-filling đầy đủ tất cả các mốc thời gian không có giao dịch trong khoảng
 * [from, to]. 2. Bảo đảm phương trình tài chính toàn vẹn: - Collected Cash = GMV - Discount Amount
 * - Net Vendor Payout = Collected Cash - Platform Commission - Refunds
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class GetRevenueReportUseCase {

    private final ReportDataPort reportDataPort;

    public GetRevenueReportUseCase(ReportDataPort reportDataPort) {
        this.reportDataPort =
                Objects.requireNonNull(reportDataPort, "reportDataPort cannot be null");
    }

    /**
     * Thống kê báo cáo doanh thu toàn hệ thống hoặc theo từng Vendor.
     *
     * @param timeRange khoảng thời gian lọc (chuẩn UTC+7)
     * @param groupBy chu kỳ gom nhóm (DAY, WEEK, QUARTER, YEAR)
     * @param vendorId ID của vendor (nếu null: toàn hệ thống cho Admin)
     * @return danh sách các điểm dữ liệu RevenueReportItem đã zero-fill theo thứ tự thời gian
     */
    public List<RevenueReportItem> execute(
            TimeRange timeRange, GroupByPeriod groupBy, UUID vendorId) {
        Objects.requireNonNull(timeRange, "timeRange cannot be null");
        GroupByPeriod effectiveGroupBy = groupBy != null ? groupBy : GroupByPeriod.DAY;

        // 1. Sinh đầy đủ danh sách key chu kỳ để zero-fill
        List<String> periodKeys =
                TimeSeriesPeriodHelper.generatePeriodKeys(
                        timeRange.getFrom(), timeRange.getTo(), effectiveGroupBy);

        Map<String, RevenueAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (String key : periodKeys) {
            accumulatorMap.put(key, new RevenueAccumulator());
        }

        for (ReportFinancialLedger.Entry entry :
                ReportFinancialLedger.load(reportDataPort, timeRange, vendorId)) {
            String key =
                    TimeSeriesPeriodHelper.formatDateTimeKey(entry.occurredAt(), effectiveGroupBy);
            RevenueAccumulator acc = accumulatorMap.get(key);
            if (acc == null) continue;
            acc.gmv = acc.gmv.add(entry.gmv());
            acc.discount = acc.discount.add(entry.discount());
            acc.cash = acc.cash.add(entry.cash());
            acc.refunds = acc.refunds.add(entry.refunds());
            acc.commission = acc.commission.add(entry.commission());
            if (entry.payment()) acc.paidOrders.add(entry.subOrderId());
        }

        // 6. Xây dựng kết quả RevenueReportItem cho từng chu kỳ
        List<RevenueReportItem> result = new ArrayList<>(periodKeys.size());
        for (Map.Entry<String, RevenueAccumulator> entry : accumulatorMap.entrySet()) {
            String key = entry.getKey();
            RevenueAccumulator acc = entry.getValue();

            BigDecimal collectedCash = acc.cash;
            BigDecimal netPayout = collectedCash.subtract(acc.commission).subtract(acc.refunds);

            result.add(
                    new RevenueReportItem(
                            key,
                            acc.gmv,
                            acc.discount,
                            collectedCash,
                            acc.refunds,
                            acc.commission,
                            netPayout,
                            acc.paidOrders.size()));
        }

        return result;
    }

    public List<RevenueReportItem> execute(TimeRange timeRange, GroupByPeriod groupBy) {
        return execute(timeRange, groupBy, null);
    }

    private static class RevenueAccumulator {
        BigDecimal gmv = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;
        Set<UUID> paidOrders = new HashSet<>();
    }
}
