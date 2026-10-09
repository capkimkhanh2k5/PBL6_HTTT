package com.danasea.backend.modules.report.infrastructure.csv;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.report.application.ports.CsvExporterPort;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Trình xuất file CSV thuần Java tuân thủ nghiêm ngặt RFC 4180, đính kèm 3 byte tiền tố UTF-8 BOM
 * (0xEF, 0xBB, 0xBF) để tương thích hiển thị tiếng Việt trên Microsoft Excel.
 */
@Component
public class Rfc4180Utf8BomCsvExporter implements CsvExporterPort {

    public static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String CRLF = "\r\n";

    @Override
    public byte[] exportRevenueReport(List<RevenueReportItem> items, TimeRange timeRange) {
        StringBuilder sb = new StringBuilder();

        // Header dòng tiêu đề tiếng Việt có dấu rõ ràng
        appendRow(
                sb,
                "Kỳ báo cáo",
                "Tổng giá trị đặt (GMV)",
                "Giảm giá",
                "Thực thu từ khách",
                "Tiền hoàn lại",
                "Hoa hồng sàn",
                "Thực nhận Vendor",
                "Số đơn hàng");

        if (items != null) {
            for (RevenueReportItem item : items) {
                appendRow(
                        sb,
                        item.getPeriodKey(),
                        item.getGmv().toPlainString(),
                        item.getDiscountAmount().toPlainString(),
                        item.getCollectedCash().toPlainString(),
                        item.getRefunds().toPlainString(),
                        item.getPlatformCommission().toPlainString(),
                        item.getNetVendorPayout().toPlainString(),
                        String.valueOf(item.getOrderCount()));
            }
        }

        return toUtf8BomBytes(sb.toString());
    }

    @Override
    public byte[] exportBookingReport(List<BookingReportItem> items, TimeRange timeRange) {
        StringBuilder sb = new StringBuilder();

        appendRow(
                sb,
                "Kỳ báo cáo",
                "Tổng số đơn",
                "Đơn hoàn tất",
                "Đơn đã hủy",
                "Tỷ lệ hoàn tất (%)",
                "Tỷ lệ hủy (%)",
                "Hủy do khách",
                "Hủy do thời tiết",
                "Hủy do nhà cung cấp",
                "Hủy do ban quản trị",
                "Khác / Khiếu nại",
                "Chưa rõ lý do");

        if (items != null) {
            for (BookingReportItem item : items) {
                long customerCancel =
                        item.getCancellationCount(RefundReason.CUSTOMER_CANCEL)
                                + item.getCancellationCount(RefundReason.CUSTOMER_REQUEST);
                long weatherCancel = item.getCancellationCount(RefundReason.WEATHER);
                long vendorFaultCancel = item.getCancellationCount(RefundReason.VENDOR_FAULT);
                long adminOverrideCancel = item.getCancellationCount(RefundReason.ADMIN_OVERRIDE);
                long otherCancel =
                        item.getCancellationCount(RefundReason.DISPUTE)
                                + item.getCancellationCount(RefundReason.COMPENSATION);

                appendRow(
                        sb,
                        item.getPeriodKey(),
                        String.valueOf(item.getTotalOrders()),
                        String.valueOf(item.getCompletedOrders()),
                        String.valueOf(item.getCancelledOrders()),
                        String.format(Locale.ROOT, "%.2f", item.getCompletionRate()),
                        String.format(Locale.ROOT, "%.2f", item.getCancellationRate()),
                        String.valueOf(customerCancel),
                        String.valueOf(weatherCancel),
                        String.valueOf(vendorFaultCancel),
                        String.valueOf(adminOverrideCancel),
                        String.valueOf(otherCancel),
                        String.valueOf(item.getUnknownCancellationCount()));
            }
        }

        return toUtf8BomBytes(sb.toString());
    }

    @Override
    public byte[] exportVendorPerformanceReport(
            List<VendorPerformanceItem> items, TimeRange timeRange) {
        StringBuilder sb = new StringBuilder();

        appendRow(
                sb,
                "Mã đối tác",
                "Tên đối tác",
                "Tổng doanh thu",
                "Thực nhận",
                "Tổng đơn",
                "Đơn hoàn tất",
                "Đơn hủy",
                "Tỷ lệ hủy (%)",
                "Tỷ lệ lấp đầy (%)",
                "Đánh giá trung bình",
                "Số lượt đánh giá",
                "Mức độ cảnh báo",
                "Vấn đề phát hiện");

        if (items != null) {
            for (VendorPerformanceItem item : items) {
                String issuesFormatted =
                        item.getIssues() == null || item.getIssues().isEmpty()
                                ? "Không có"
                                : item.getIssues().stream()
                                        .map(VendorIssueType::name)
                                        .collect(Collectors.joining("; "));

                appendRow(
                        sb,
                        item.getVendorId().toString(),
                        safeSpreadsheetText(item.getBusinessName()),
                        item.getRevenue().toPlainString(),
                        item.getNetPayout().toPlainString(),
                        String.valueOf(item.getTotalOrders()),
                        String.valueOf(item.getCompletedOrders()),
                        String.valueOf(item.getCancelledOrders()),
                        String.format(Locale.ROOT, "%.2f", item.getCancellationRate()),
                        String.format(Locale.ROOT, "%.2f", item.getSlotOccupancyRate()),
                        item.getRating().toPlainString(),
                        String.valueOf(item.getRatingCount()),
                        item.getSeverity().name(),
                        issuesFormatted);
            }
        }

        return toUtf8BomBytes(sb.toString());
    }

    /** Nối một dòng bản ghi CSV, tự động escape các trường theo chuẩn RFC 4180. */
    private void appendRow(StringBuilder sb, String... fields) {
        for (int i = 0; i < fields.length; i++) {
            sb.append(escapeField(fields[i]));
            if (i < fields.length - 1) {
                sb.append(",");
            }
        }
        sb.append(CRLF);
    }

    /**
     * Escape field theo chuẩn RFC 4180: - Nếu giá trị chứa dấu phẩy (,), nháy kép ("), hoặc dấu
     * xuống dòng (\r, \n): bọc toàn bộ chuỗi trong cặp dấu nháy kép, và nhân đôi dấu nháy kép bên
     * trong (" -> "").
     */
    private static String safeSpreadsheetText(String field) {
        if (field == null) return "";
        String stripped = field.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0
                || field.startsWith("\t")
                || field.startsWith("\r")
                || field.startsWith("\n")) {
            return "'" + field;
        }
        return field;
    }

    public static String escapeField(String field) {
        if (field == null) {
            return "";
        }
        boolean needsQuotes =
                field.contains(",")
                        || field.contains("\"")
                        || field.contains("\n")
                        || field.contains("\r");

        if (needsQuotes) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /** Chuyển chuỗi CSV thành mảng byte có tiền tố UTF-8 BOM (0xEF, 0xBB, 0xBF). */
    private byte[] toUtf8BomBytes(String csvContent) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            baos.write(UTF8_BOM);
            baos.write(csvContent.getBytes(StandardCharsets.UTF_8));
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write CSV bytes", e);
        }
    }
}
