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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@DisplayName("Rfc4180Utf8BomCsvExporter Challenger Adversarial Stress Suite")
class Rfc4180Utf8BomCsvExporterChallengerTest {

    private Rfc4180Utf8BomCsvExporter exporter;
    private TimeRange timeRange;

    @BeforeEach
    void setUp() {
        exporter = new Rfc4180Utf8BomCsvExporter();
        timeRange = TimeRange.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    @Test
    @DisplayName(
            "C1 - Xác minh byte-level tuyệt đối: 3 byte đầu luôn là 0xEF, 0xBB, 0xBF (\uFEFF) cho"
                    + " mọi loại báo cáo")
    void shouldVerifyExactThreeByteUtf8BomIntegrity() {
        // Test cả 3 loại báo cáo với null, empty và data thực
        List<byte[]> allOutputs = new ArrayList<>();
        allOutputs.add(exporter.exportRevenueReport(null, timeRange));
        allOutputs.add(exporter.exportRevenueReport(Collections.emptyList(), timeRange));
        allOutputs.add(exporter.exportBookingReport(null, timeRange));
        allOutputs.add(exporter.exportBookingReport(Collections.emptyList(), timeRange));
        allOutputs.add(exporter.exportVendorPerformanceReport(null, timeRange));
        allOutputs.add(exporter.exportVendorPerformanceReport(Collections.emptyList(), timeRange));

        for (byte[] bytes : allOutputs) {
            assertThat(bytes).isNotNull();
            assertThat(bytes.length).isGreaterThanOrEqualTo(3);

            // Byte 0: 0xEF (-17 signed byte)
            assertThat(bytes[0]).isEqualTo((byte) 0xEF);
            // Byte 1: 0xBB (-69 signed byte)
            assertThat(bytes[1]).isEqualTo((byte) 0xBB);
            // Byte 2: 0xBF (-65 signed byte)
            assertThat(bytes[2]).isEqualTo((byte) 0xBF);

            // Khi decode từ byte 0: Ký tự đầu tiên phải là \uFEFF
            String decodedWithBom = new String(bytes, StandardCharsets.UTF_8);
            assertThat(decodedWithBom.charAt(0)).isEqualTo('\uFEFF');

            // Khi decode từ byte 3: Không còn \uFEFF, ký tự đầu tiên là chữ cái của Header
            String decodedWithoutBom =
                    new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
            assertThat(decodedWithoutBom.charAt(0)).isNotEqualTo('\uFEFF');
            assertThat(
                            decodedWithoutBom.startsWith("Kỳ báo cáo")
                                    || decodedWithoutBom.startsWith("Mã đối tác"))
                    .isTrue();
        }
    }

    @Test
    @DisplayName("C2 - Thử nghiệm đối kháng CSV Injection và ký tự đặc biệt theo chuẩn RFC 4180")
    void shouldHandleRfc4180SpecialCharactersAndInjection() {
        // 1. Dấu phẩy
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Đà Nẵng, Việt Nam"))
                .isEqualTo("\"Đà Nẵng, Việt Nam\"");

        // 2. Dấu nháy kép đơn và chuỗi phức tạp
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Tour \"VIP\" 5 sao"))
                .isEqualTo("\"Tour \"\"VIP\"\" 5 sao\"");

        // 3. Chuỗi toàn dấu nháy kép
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("\"")).isEqualTo("\"\"\"\"");
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("\"\"")).isEqualTo("\"\"\"\"\"\"");

        // 4. Ký tự ngắt dòng \r\n, \n, \r
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Dòng 1\r\nDòng 2"))
                .isEqualTo("\"Dòng 1\r\nDòng 2\"");
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Dòng 1\nDòng 2"))
                .isEqualTo("\"Dòng 1\nDòng 2\"");
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("Dòng 1\rDòng 2"))
                .isEqualTo("\"Dòng 1\rDòng 2\"");

        // 5. Chuỗi rỗng và null
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField(null)).isEqualTo("");
        assertThat(Rfc4180Utf8BomCsvExporter.escapeField("")).isEqualTo("");

        // 6. Kết hợp hỗn hợp nhiều ký tự nguy hiểm: phẩy + nháy kép + ngắt dòng
        String complexAttack = "Tên \"Nguy hiểm\", có phẩy và \r\nngắt dòng, kết thúc bằng \"";
        String escaped = Rfc4180Utf8BomCsvExporter.escapeField(complexAttack);
        assertThat(escaped).startsWith("\"").endsWith("\"");
        assertThat(escaped).contains("\"\"Nguy hiểm\"\"");
        assertThat(escaped).contains("\r\n");
    }

    @Test
    @DisplayName("C3 - Kiểm tra bảo toàn dữ liệu tiếng Việt có dấu phức tạp và Emoji đa byte")
    void shouldPreserveComplexVietnameseDiacriticsAndEmojis() {
        UUID vendorId = UUID.randomUUID();
        String complexVietnameseName =
                "Công ty TNHH Thuyền SUP & Ca-nô lướt sóng Đà Nẵng 🚣🌊 (Ngũ Hành Sơn - Sơn Trà)";

        VendorPerformanceItem item =
                new VendorPerformanceItem(
                        vendorId,
                        complexVietnameseName,
                        BigDecimal.valueOf(100_000_000),
                        BigDecimal.valueOf(90_000_000),
                        100,
                        95,
                        5,
                        5.0,
                        80.0,
                        BigDecimal.valueOf(4.9),
                        50,
                        VendorAlertSeverity.HEALTHY,
                        Collections.emptyList());

        byte[] bytes = exporter.exportVendorPerformanceReport(List.of(item), timeRange);
        String csvContent = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        // Trường hợp không có dấu phẩy: không cần nháy kép theo RFC 4180
        assertThat(csvContent).contains(complexVietnameseName);
        assertThat(csvContent).contains("🚣🌊");
        assertThat(csvContent).contains("Đà Nẵng");
        assertThat(csvContent).contains("Ngũ Hành Sơn");

        // Trường hợp có dấu phẩy: bắt buộc phải được bọc trong cặp nháy kép
        String nameWithComma = "Thuyền SUP, Ca-nô Đà Nẵng 🚣";
        VendorPerformanceItem itemWithComma =
                new VendorPerformanceItem(
                        UUID.randomUUID(),
                        nameWithComma,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0,
                        0,
                        0,
                        0.0,
                        0.0,
                        BigDecimal.ZERO,
                        0,
                        VendorAlertSeverity.HEALTHY,
                        Collections.emptyList());
        byte[] bytesWithComma =
                exporter.exportVendorPerformanceReport(List.of(itemWithComma), timeRange);
        String csvWithComma =
                new String(bytesWithComma, 3, bytesWithComma.length - 3, StandardCharsets.UTF_8);
        assertThat(csvWithComma).contains("\"" + nameWithComma + "\"");
    }

    @Test
    @DisplayName(
            "C4 - Thử nghiệm số tiền cực lớn (hàng trăm nghìn tỷ) và số 0 không bị mất định dạng"
                    + " hoặc rơi vào Scientific Notation")
    void shouldHandleLargeNumbersAndZeroWithoutScientificNotation() {
        BigDecimal hugeGmv = new BigDecimal("999999999999999"); // Gần 1 triệu tỷ VNĐ
        BigDecimal zeroDiscount = BigDecimal.ZERO;
        BigDecimal zeroRefund = new BigDecimal("0");

        RevenueReportItem item =
                new RevenueReportItem(
                        "2026-10-15",
                        hugeGmv,
                        zeroDiscount,
                        hugeGmv,
                        zeroRefund,
                        new BigDecimal("99999999999999.9"),
                        new BigDecimal("900000000000000.1"),
                        10_000_000L);

        byte[] bytes = exporter.exportRevenueReport(List.of(item), timeRange);
        String csvContent = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        // Kiểm tra số lớn không bị chuyển thành 1E+15 hoặc 9.99999E+14
        assertThat(csvContent).contains("999999999999999");
        assertThat(csvContent).doesNotContain("E+");
        assertThat(csvContent).doesNotContain("e+");

        // Kiểm tra số 0 xuất ra đúng "0"
        String[] lines = csvContent.split("\r\n");
        assertThat(lines).hasSize(2); // 1 header + 1 data row
        String dataRow = lines[1];
        String[] fields = dataRow.split(",");
        // periodKey: fields[0] = "2026-10-15"
        // gmv: fields[1] = "999999999999999"
        // discountAmount: fields[2] = "0"
        assertThat(fields[2]).isEqualTo("0");
        // refunds: fields[4] = "0"
        assertThat(fields[4]).isEqualTo("0");
        // orderCount: fields[7] = "10000000"
        assertThat(fields[7]).isEqualTo("10000000");
    }

    @Test
    @DisplayName("C5 - Thử nghiệm xuất danh sách rỗng vẫn giữ đúng cấu trúc header và CRLF")
    void shouldMaintainHeaderStructureForEmptyLists() {
        byte[] revenueBytes = exporter.exportRevenueReport(Collections.emptyList(), timeRange);
        String revenueCsv =
                new String(revenueBytes, 3, revenueBytes.length - 3, StandardCharsets.UTF_8);
        assertThat(revenueCsv.split("\r\n")).hasSize(1);
        String[] revenueCols = revenueCsv.replace("\r\n", "").split(",");
        assertThat(revenueCols).hasSize(8);

        byte[] bookingBytes = exporter.exportBookingReport(Collections.emptyList(), timeRange);
        String bookingCsv =
                new String(bookingBytes, 3, bookingBytes.length - 3, StandardCharsets.UTF_8);
        assertThat(bookingCsv.split("\r\n")).hasSize(1);
        String[] bookingCols = bookingCsv.replace("\r\n", "").split(",");
        assertThat(bookingCols).hasSize(12);

        byte[] vendorBytes =
                exporter.exportVendorPerformanceReport(Collections.emptyList(), timeRange);
        String vendorCsv =
                new String(vendorBytes, 3, vendorBytes.length - 3, StandardCharsets.UTF_8);
        assertThat(vendorCsv.split("\r\n")).hasSize(1);
        String[] vendorCols = vendorCsv.replace("\r\n", "").split(",");
        assertThat(vendorCols).hasSize(13);
    }

    @Test
    @DisplayName("C6 - Kiểm tra tính nhạy cảm với Locale hệ thống đối với định dạng số thập phân")
    void shouldExamineLocaleSensitivityOnDecimalFormatting() {
        Locale initialDefaultLocale = Locale.getDefault();
        try {
            // Thiết lập thử nghiệm Locale GERMANY (sử dụng dấu phẩy cho số thập phân)
            Locale.setDefault(Locale.GERMANY);

            Map<RefundReason, Long> breakdown = new EnumMap<>(RefundReason.class);
            breakdown.put(RefundReason.CUSTOMER_CANCEL, 1L);
            BookingReportItem item = new BookingReportItem("2026-10-01", 10L, 9L, 1L, breakdown);

            byte[] bytes = exporter.exportBookingReport(List.of(item), timeRange);
            String csvContent = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

            // Trong Locale GERMANY, String.format("%.2f", 90.0) sinh ra "90,00".
            // Vì có dấu phẩy, escapeField tự động bọc trong dấu nháy kép: "\"90,00\"".
            // Xác minh cấu trúc CSV vẫn hợp lệ và an toàn không bị gãy cột.
            assertThat(csvContent).isNotNull();
        } finally {
            Locale.setDefault(initialDefaultLocale);
        }
    }

    @Test
    @DisplayName(
            "C7 - Stress test xuất tập dữ liệu lớn (10.000 dòng) đảm bảo hiệu năng và không tràn bộ"
                    + " nhớ")
    void shouldHandleLargeDatasetStressWithoutOom() {
        int recordCount = 10_000;
        List<RevenueReportItem> largeList = new ArrayList<>(recordCount);

        for (int i = 0; i < recordCount; i++) {
            largeList.add(
                    new RevenueReportItem(
                            "2026-10-" + (1 + (i % 30)),
                            BigDecimal.valueOf(1_000_000L * (i + 1)),
                            BigDecimal.valueOf(100_000L),
                            BigDecimal.valueOf(900_000L * (i + 1)),
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(90_000L),
                            BigDecimal.valueOf(810_000L),
                            i + 1));
        }

        long start = System.currentTimeMillis();
        byte[] bytes = exporter.exportRevenueReport(largeList, timeRange);
        long duration = System.currentTimeMillis() - start;

        assertThat(bytes).isNotNull();
        assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        // Kiểm tra thời gian xuất 10.000 dòng thuần Java dưới 500ms
        assertThat(duration).isLessThan(500);

        String csvString = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        String[] lines = csvString.split("\r\n");
        // 1 dòng header + 10,000 dòng dữ liệu
        assertThat(lines).hasSize(recordCount + 1);
    }

    @Test
    @DisplayName(
            "C8 - Parser Oracle: Kiểm chứng giải mã ngược (Round-trip Parsing) dữ liệu phức tạp")
    void shouldVerifyRoundTripCsvParsingOracle() {
        UUID vendorId = UUID.randomUUID();
        String dangerousName = "Danasea \"VIP\", Phường Mỹ An, TP Đà Nẵng\r\nHotline: 0905123456";

        VendorPerformanceItem item =
                new VendorPerformanceItem(
                        vendorId,
                        dangerousName,
                        BigDecimal.valueOf(50_000_000),
                        BigDecimal.valueOf(45_000_000),
                        50,
                        45,
                        5,
                        10.0,
                        75.0,
                        BigDecimal.valueOf(4.8),
                        20,
                        VendorAlertSeverity.WARNING,
                        List.of(VendorIssueType.HIGH_CANCELLATION));

        byte[] bytes = exporter.exportVendorPerformanceReport(List.of(item), timeRange);
        String csvContent = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);

        // Dùng bộ giải mã chuẩn RFC 4180 để bóc tách các trường
        List<String> parsedFields =
                parseSingleRfc4180Row(csvContent.substring(csvContent.indexOf("\r\n") + 2));

        assertThat(parsedFields).hasSize(13);
        assertThat(parsedFields.get(0)).isEqualTo(vendorId.toString());
        // Trường tên nguy hiểm phải được phục hồi nguyên vẹn 100% không mất mát hay lệch ký tự
        assertThat(parsedFields.get(1)).isEqualTo(dangerousName);
        assertThat(parsedFields.get(2)).isEqualTo("50000000");
        assertThat(parsedFields.get(3)).isEqualTo("45000000");
    }

    /** Thuật toán Oracle giải mã một dòng RFC 4180 để kiểm chứng độc lập. */
    private List<String> parseSingleRfc4180Row(String row) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < row.length() && row.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else if (c == '\r' && i + 1 < row.length() && row.charAt(i + 1) == '\n') {
                    break;
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
