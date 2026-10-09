package com.danasea.backend.modules.report.application.usecases;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.report.application.ports.ReportDataPort;
import com.danasea.backend.modules.report.application.ports.ServiceSlotRecord;
import com.danasea.backend.modules.report.application.ports.SubOrderRecord;
import com.danasea.backend.modules.report.application.ports.VendorRecord;
import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.DashboardMetrics;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorDiagnosisAlert;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * UseCase tổng hợp chỉ số hoạt động thời gian thực của riêng 1 Vendor (Vendor Dashboard Metrics).
 *
 * <p>Tính toán: - Tổng doanh thu (GMV), Doanh thu thực nhận (Net Vendor Payout). - Số lượng đơn
 * mới, đơn hoàn tất, đơn hủy. - Tỷ lệ hoàn tất và tỷ lệ hủy. - Phát hiện và cảnh báo bất thường:
 * quá tải slot dịch vụ (>= 95% công suất), tỷ lệ hủy đơn cao (> 20%).
 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class GetVendorDashboardUseCase {

    private final ReportDataPort reportDataPort;

    public GetVendorDashboardUseCase(ReportDataPort reportDataPort) {
        this.reportDataPort =
                Objects.requireNonNull(reportDataPort, "reportDataPort cannot be null");
    }

    /**
     * Lấy chỉ số Dashboard của Vendor trong khoảng thời gian TimeRange.
     *
     * @param vendorId ID của Vendor (bắt buộc)
     * @param timeRange khoảng thời gian lọc (nếu null: mặc định từ đầu tháng hiện tại đến hôm nay)
     * @return DashboardMetrics dành riêng cho Vendor
     */
    public DashboardMetrics execute(UUID vendorId, TimeRange timeRange) {
        Objects.requireNonNull(vendorId, "vendorId cannot be null");

        TimeRange effectiveRange = timeRange != null ? timeRange : defaultTimeRange();

        // 1. Trích xuất dữ liệu vận hành của Vendor
        List<SubOrderRecord> subOrders = reportDataPort.findSubOrders(effectiveRange, vendorId);
        List<ServiceSlotRecord> slots = reportDataPort.findServiceSlots(effectiveRange, vendorId);

        Optional<VendorRecord> vendorOpt = reportDataPort.findVendorById(vendorId);
        String businessName = vendorOpt.map(VendorRecord::businessName).orElse("Vendor");

        Map<UUID, RefundReason> cancellationReasons =
                BookingCancellationClassifier.reasons(reportDataPort, subOrders);
        // 2. Thống kê số lượng đơn
        long newOrders = subOrders.size();
        long completedOrders =
                subOrders.stream().filter(s -> s.status() == SubOrderStatus.COMPLETED).count();
        long cancelledOrders =
                subOrders.stream()
                        .filter(
                                s ->
                                        BookingCancellationClassifier.isCancelled(
                                                s, cancellationReasons))
                        .count();

        List<ReportFinancialLedger.Entry> financialEntries =
                ReportFinancialLedger.load(reportDataPort, effectiveRange, vendorId);
        BigDecimal totalRevenue =
                financialEntries.stream()
                        .map(ReportFinancialLedger.Entry::gmv)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netRevenue =
                financialEntries.stream()
                        .map(ReportFinancialLedger.Entry::payout)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Phát hiện cảnh báo vận hành
        List<VendorDiagnosisAlert> alerts = new ArrayList<>();

        // Cảnh báo tỷ lệ hủy cao
        if (newOrders > 0) {
            double cancellationRate = (double) cancelledOrders / newOrders * 100.0;
            if (cancellationRate > 20.0) {
                VendorAlertSeverity severity =
                        cancellationRate > 40.0
                                ? VendorAlertSeverity.CRITICAL
                                : VendorAlertSeverity.WARNING;
                alerts.add(
                        new VendorDiagnosisAlert(
                                vendorId,
                                businessName,
                                VendorIssueType.HIGH_CANCELLATION,
                                severity,
                                String.format(
                                        "Tỷ lệ hủy đơn cao bất thường: %.2f%%", cancellationRate),
                                cancellationRate));
            }
        }

        // Cảnh báo quá tải công suất slot
        int totalCapacity =
                slots.stream().mapToInt(ServiceSlotRecord::capacity).filter(c -> c > 0).sum();
        int totalBooked =
                slots.stream().mapToInt(ServiceSlotRecord::bookedCount).filter(b -> b > 0).sum();

        if (totalCapacity > 0) {
            double slotOccupancyRate = (double) totalBooked / totalCapacity * 100.0;
            if (slotOccupancyRate >= 95.0) {
                alerts.add(
                        new VendorDiagnosisAlert(
                                vendorId,
                                businessName,
                                VendorIssueType.OVERLOADED,
                                VendorAlertSeverity.CRITICAL,
                                String.format(
                                        "Các khung giờ dịch vụ đang quá tải công suất: %.2f%%",
                                        slotOccupancyRate),
                                slotOccupancyRate));
            }
        }

        return DashboardMetrics.of(
                totalRevenue, netRevenue, newOrders, completedOrders, cancelledOrders, alerts);
    }

    public DashboardMetrics execute(UUID vendorId) {
        return execute(vendorId, defaultTimeRange());
    }

    private TimeRange defaultTimeRange() {
        LocalDate today = LocalDate.now(TimeRange.VIETNAM_ZONE);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        return TimeRange.of(startOfMonth, today);
    }
}
