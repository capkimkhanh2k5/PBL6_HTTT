package com.danasea.backend.modules.report.application.ports;

import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;

import java.util.List;

/**
 * Port khai báo hợp đồng xuất dữ liệu báo cáo sang định dạng file CSV. Đầu ra chuẩn UTF-8 có BOM
 * (0xEF, 0xBB, 0xBF) và tuân thủ RFC 4180.
 */
public interface CsvExporterPort {

    /**
     * Xuất báo cáo doanh thu theo chu kỳ sang mảng byte CSV.
     *
     * @param items danh sách dòng báo cáo doanh thu
     * @param timeRange khoảng thời gian lọc báo cáo
     * @return mảng byte CSV có UTF-8 BOM
     */
    byte[] exportRevenueReport(List<RevenueReportItem> items, TimeRange timeRange);

    /**
     * Xuất báo cáo đơn đặt theo chu kỳ sang mảng byte CSV.
     *
     * @param items danh sách dòng báo cáo đơn đặt
     * @param timeRange khoảng thời gian lọc báo cáo
     * @return mảng byte CSV có UTF-8 BOM
     */
    byte[] exportBookingReport(List<BookingReportItem> items, TimeRange timeRange);

    /**
     * Xuất báo cáo hiệu suất và chẩn đoán đối tác (Vendor) sang mảng byte CSV.
     *
     * @param items danh sách dòng hiệu suất từng vendor
     * @param timeRange khoảng thời gian lọc báo cáo
     * @return mảng byte CSV có UTF-8 BOM
     */
    byte[] exportVendorPerformanceReport(List<VendorPerformanceItem> items, TimeRange timeRange);
}
