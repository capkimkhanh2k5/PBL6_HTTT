package com.danasea.backend.modules.report.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.danasea.backend.modules.order.domain.models.RefundReason;
import com.danasea.backend.modules.report.domain.enums.VendorAlertSeverity;
import com.danasea.backend.modules.report.domain.enums.VendorIssueType;
import com.danasea.backend.modules.report.domain.models.BookingReportItem;
import com.danasea.backend.modules.report.domain.models.RevenueReportItem;
import com.danasea.backend.modules.report.domain.models.TimeRange;
import com.danasea.backend.modules.report.domain.models.VendorPerformanceItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@DisplayName("Rfc4180Utf8BomCsvExporter Unit & RFC 4180 Compliance Tests")
class Rfc4180Utf8BomCsvExporterTest {

    private Rfc4180Utf8BomCsvExporter exporter;
    private TimeRange timeRange;

    @BeforeEach
    void setUp() {
        exporter = new Rfc4180Utf8BomCsvExporter();
        timeRange = TimeRange.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
    }

    @Test
    @DisplayName(
            "Bắt buộc tiền tố 3 byte UTF-8 BOM (0xEF, 0xBB, 0xBF) cho tất cả các loại báo cáo xuất"
                    + " ra")
    void shouldPrependUtf8BomHeader() {
        byte[] revenueBytes = exporter.exportRevenueReport(Collections.emptyList(), timeRange);
        byte[] bookingBytes = exporter.exportBookingReport(Collections.emptyList(), timeRange);
        byte[] vendorBytes =
                exporter.exportVendorPerformanceReport(Collections.emptyList(), timeRange);

        assertThat(revenueBytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(bookingBytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(vendorBytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
    }

    @Test
    @DisplayName("Xuất báo cáo doanh thu với tiêu đề tiếng Việt và dữ liệu số chính xác")
    void shouldExportRevenueReportCorrectly() {
        RevenueReportItem item1 =
                new RevenueReportItem(
                        "2026-10-01",
                        BigDecimal.valueOf(10_000_000),
                        BigDecimal.valueOf(1_000_000),
                        BigDecimal.valueOf(9_000_000),
                        BigDecimal.valueOf(500_000),
                        BigDecimal.valueOf(900_000),
                        BigDecimal.valueOf(7_600_000),
                        12);

        byte[] bytes = exporter.exportRevenueReport(List.of(item1), timeRange);
        String csvString = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        assertThat(csvString)
                .contains(
                        "Kỳ báo cáo,Tổng giá trị đặt (GMV),Giảm giá,Thực thu từ khách,Tiền hoàn"
                                + " lại,Hoa hồng sàn,Thực nhận Vendor,Số đơn hàng");
        assertThat(csvString)
                .contains("2026-10-01,10000000,1000000,9000000,500000,900000,7600000,12");
        assertThat(csvString).contains("\r\n");
    }

    @Test
    @DisplayName("Xuất báo cáo đơn đặt với các cột phân rã theo lý do hủy và tỷ lệ phần trăm")
    void shouldExportBookingReportCorrectly() {
        Map<RefundReason, Long> breakdown =
                Map.of(
                        RefundReason.CUSTOMER_CANCEL, 3L,
                        RefundReason.WEATHER, 5L,
                        RefundReason.VENDOR_FAULT, 1L);
        BookingReportItem item = new BookingReportItem("2026-10-02", 20L, 11L, 9L, breakdown);

        byte[] bytes = exporter.exportBookingReport(List.of(item), timeRange);
        String csvString = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        assertThat(csvString)
                .contains(
                        "Kỳ báo cáo,Tổng số đơn,Đơn hoàn tất,Đơn đã hủy,Tỷ lệ hoàn tất (%),Tỷ lệ"
                            + " hủy (%),Hủy do khách,Hủy do thời tiết,Hủy do nhà cung cấp,Hủy do"
                            + " ban quản trị,Khác / Khiếu nại");
        assertThat(csvString).contains("2026-10-02,20,11,9,55.00,45.00,3,5,1,0,0");
    }

    @Test
    @DisplayName("Xuất báo cáo hiệu suất đối tác có thông tin chẩn đoán và mức độ cảnh báo")
    void shouldExportVendorPerformanceReportCorrectly() {
        UUID vendorId = UUID.randomUUID();
        VendorPerformanceItem item =
                new VendorPerformanceItem(
                        vendorId,
                        "Du thuyền Sông Hàn",
                        BigDecimal.valueOf(50_000_000),
                        BigDecimal.valueOf(45_000_000),
                        50,
                        40,
                        10,
                        20.0,
                        88.5,
                        BigDecimal.valueOf(4.5),
                        25,
                        VendorAlertSeverity.WARNING,
                        List.of(VendorIssueType.HIGH_CANCELLATION));

        byte[] bytes = exporter.exportVendorPerformanceReport(List.of(item), timeRange);
        String csvString = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        assertThat(csvString)
                .contains(
                        "Mã đối tác,Tên đối tác,Tổng doanh thu,Thực nhận,Tổng đơn,Đơn hoàn tất,Đơn"
                                + " hủy,Tỷ lệ hủy (%),Tỷ lệ lấp đầy (%),Đánh giá trung bình,Số lượt"
                                + " đánh giá,Mức độ cảnh báo,Vấn đề phát hiện");
        assertThat(csvString).contains(vendorId.toString());
        assertThat(csvString).contains("Du thuyền Sông Hàn");
        assertThat(csvString)
                .contains(
                        "50000000,45000000,50,40,10,20.00,88.50,4.5,25,WARNING,HIGH_CANCELLATION");
    }

    @Test
    @DisplayName(
            "Tuân thủ chuẩn RFC 4180 khi trường dữ liệu chứa dấu phẩy, nháy kép, và xuống dòng")
    void shouldEscapeSpecialCharactersPerRfc4180() {
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Đơn thường")).isEqualTo("Đơn thường");

        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Đà Nẵng, Việt Nam"))
                .isEqualTo("\"Đà Nẵng, Việt Nam\"");

        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Tour \"VIP\""))
                .isEqualTo("\"Tour \"\"VIP\"\"\"");

        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Dòng 1\nDòng 2"))
                .isEqualTo("\"Dòng 1\nDòng 2\"");

        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Dòng 1\r\nDòng 2"))
                .isEqualTo("\"Dòng 1\r\nDòng 2\"");
    }

    @Test
    @DisplayName("An toàn tuyệt đối khi danh sách báo cáo rỗng hoặc null")
    void shouldHandleEmptyAndNullListsGracefully() {
        byte[] revenueNull = exporter.exportRevenueReport(null, timeRange);
        assertThat(revenueNull).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(new String(revenueNull, StandardCharsets.UTF_8)).contains("Kỳ báo cáo");

        byte[] bookingNull = exporter.exportBookingReport(null, timeRange);
        assertThat(bookingNull).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);

        byte[] vendorNull = exporter.exportVendorPerformanceReport(null, timeRange);
        assertThat(vendorNull).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
    }
}
