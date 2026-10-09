# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE REPORT & DASHBOARD (THỐNG KÊ & BÁO CÁO KINH DOANH)

> Module Report & Dashboard chịu trách nhiệm tính toán, tổng hợp các chỉ số hoạt động thời gian thực (Real-time Dashboard), phân rã doanh thu và đơn hàng theo chuỗi thời gian (Time-series Analytics), chẩn đoán điểm nghẽn hiệu suất đối tác (Vendor Diagnostics) và cung cấp bộ máy xuất dữ liệu CSV chuẩn hóa theo múi giờ Việt Nam (`Asia/Ho_Chi_Minh` UTC+7) cho cả Quản trị viên (Admin) và Đối tác (Vendor).

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Report_Architecture_And_DataFlow` | Architecture & Data Flow | Kiến trúc tổng thể Clean Architecture của Module Report: Phân tách rõ ràng giữa Tầng Presentation (Controllers & DTOs), Application (6 UseCases & TimeSeriesHelper), Domain (Models, Enums, Ports) và Infrastructure (JpaReportDataAdapter & Rfc4180Utf8BomCsvExporter). |
| 2 | `Report_TimeSeries_Analytics` | Sequence Diagram | Quy trình tổng hợp báo cáo doanh thu và đơn hàng theo chuỗi thời gian: Chuẩn hóa múi giờ UTC+7 và biên ngày [00:00:00 - 23:59:59], phân rã 5 chỉ số tài chính, phân loại lý do hủy đơn và tự động lấp đầy chu kỳ trống (Zero-filling). |
| 3 | `Report_Vendor_Performance_Diagnostics` | Sequence Diagram & Decision Engine | Động cơ phân tích hiệu suất và chẩn đoán đối tác: Tính toán GMV, Net Payout, tỷ lệ hủy, tỷ lệ lấp đầy chỗ (`slot occupancy rate`), điểm đánh giá thực tế, tự động phát hiện 4 nhóm vấn đề (`HIGH_CANCELLATION`, `LOW_RATING`, `OVERLOADED`, `UNDERPERFORMING`) và phân cấp cảnh báo (`HEALTHY`, `WARNING`, `CRITICAL`). |
| 4 | `Report_Export_And_VendorIsolation` | Sequence Diagram | Cơ chế cô lập đa người thuê (Multi-tenant Vendor Isolation) và xuất dữ liệu CSV chuẩn RFC 4180: Chống tấn công IDOR bằng cách trích xuất danh tính trực tiếp từ Security Context, gắn 3-byte UTF-8 BOM (`0xEF, 0xBB, 0xBF`) để Excel hiển thị tiếng Việt hoàn hảo. |

---

## 1. Report_Architecture_And_DataFlow.png — Kiến Trúc & Dòng Dữ Liệu Module Report

**Package gốc:** `com.danasea.backend.modules.report`

**Phân tầng kiến trúc Clean Architecture:**
1. **Tầng Presentation (`presentation`):**
   - `AdminController`: Cập nhật endpoint `GET /api/admin/dashboard` trả về số liệu Dashboard thật và duy trì thuộc tính `status: "ADMIN_ACCESS_GRANTED"` để đảm bảo tương thích ngược 100% với các client và test suite kế thừa.
   - `VendorDashboardController`: Cung cấp `GET /api/vendor/dashboard` hiển thị KPIs vận hành thời gian thực của chính đối tác.
   - `AdminReportController`: Cung cấp các API báo cáo toàn diện cho Admin: `GET /api/admin/reports/revenue`, `/bookings`, `/vendors`, `/export`.
   - `VendorReportController`: Cung cấp các API báo cáo cho Vendor: `GET /api/vendor/reports/revenue`, `/bookings`, `/export`.
   - Các DTOs strongly-typed đóng gói dữ liệu và hỗ trợ Bean Validation: `AdminDashboardResponse`, `VendorDashboardResponse`, `RevenueReportResponse`, `BookingReportResponse`, `VendorPerformanceResponse`, `ReportAlertDto`.
2. **Tầng Application (`application`):**
   - 6 UseCases nghiệp vụ cốt lõi:
     - `GetAdminDashboardUseCase`: Tổng hợp chỉ số toàn sàn thời gian thực & cảnh báo tỷ lệ hủy/quá tải.
     - `GetVendorDashboardUseCase`: Tổng hợp chỉ số riêng cho từng Vendor thời gian thực & cảnh báo hiệu suất.
     - `GetRevenueReportUseCase`: Thống kê tài chính time-series (GMV, Cash, Refunds, Commission, Net Payout) theo chu kỳ (`day`, `week`, `quarter`, `year`).
     - `GetBookingReportUseCase`: Thống kê đơn đặt time-series phân loại chi tiết theo lý do hủy (`CUSTOMER_CANCEL`, `WEATHER`, `VENDOR_FAULT`, `ADMIN_OVERRIDE`, v.v.).
     - `GetVendorPerformanceReportUseCase`: Phân tích hiệu quả từng đối tác (doanh thu, đơn, tỷ lệ hủy, slot occupancy rate, rating) kèm chẩn đoán thông minh (`HEALTHY`, `WARNING`, `CRITICAL`).
     - `ExportReportCsvUseCase`: Điều phối trích xuất và xuất dữ liệu báo cáo dạng CSV.
   - Tiện ích xử lý chu kỳ: `TimeSeriesPeriodHelper` tự động gom nhóm theo ngày/tuần/quý/năm và lấp đầy giá trị 0 (Zero-filling) cho các khoảng thời gian trống.
3. **Tầng Domain (`domain`):**
   - Models & Value Objects: `TimeRange` (chuẩn hóa múi giờ UTC+7 và biên ngày an toàn), `DashboardMetrics`, `RevenueReportItem`, `BookingReportItem`, `VendorPerformanceItem`, `VendorDiagnosisAlert`.
   - Enums: `GroupByPeriod` (`DAY`, `WEEK`, `QUARTER`, `YEAR`), `ReportType` (`REVENUE`, `BOOKINGS`, `VENDORS`), `VendorAlertSeverity` (`HEALTHY`, `WARNING`, `CRITICAL`), `VendorIssueType`.
   - Domain Ports: `ReportDataPort` (cổng trích xuất dữ liệu tổng hợp), `CsvExporterPort` (cổng định dạng CSV).
4. **Tầng Infrastructure (`infrastructure`):**
   - `JpaReportDataAdapter`: Hiện thực `ReportDataPort` bằng các truy vấn JPQL/SQL DB-agnostic, tham số hóa an toàn chống SQL injection, tương thích hoàn toàn trên cả PostgreSQL (Production) và H2 (In-memory testing).
   - `Rfc4180Utf8BomCsvExporter`: Hiện thực `CsvExporterPort` thuần Java, tuân thủ RFC 4180 và tự động đính kèm 3-byte UTF-8 BOM (`\uFEFF`).

---

## 2. Report_TimeSeries_Analytics.png — Báo Cáo Chuỗi Thời Gian Doanh Thu & Đơn Hàng

**Lớp xử lý chính:** `GetRevenueReportUseCase`, `GetBookingReportUseCase`, `TimeSeriesPeriodHelper`, `JpaReportDataAdapter`

**Luồng nghiệp vụ chi tiết:**
1. Người dùng (Admin hoặc Vendor) gửi yêu cầu: `GET /api/{role}/reports/revenue` hoặc `GET /api/{role}/reports/bookings` kèm các tham số `from`, `to`, `groupBy` (`day`, `week`, `quarter`, `year`).
2. **Kiểm tra quyền hạn & Múi giờ:**
   - Spring Security xác thực quyền (`ADMIN` hoặc `VENDOR`).
   - Nếu là Vendor, hệ thống tự động trích xuất `vendorId` từ Security Context; nếu là Admin, cho phép truyền `vendorId` tùy chọn hoặc để trống để xem toàn sàn.
   - `TimeRange.of(from, to)` kiểm tra ràng buộc `from <= to` (nếu vi phạm ném lỗi `400 Bad Request`). Chuyển đổi an toàn theo múi giờ `Asia/Ho_Chi_Minh` (UTC+7):
     - `startDateTime = from 00:00:00.000000+07:00`
     - `endDateTime = to 23:59:59.999999+07:00`
3. **Trích xuất dữ liệu qua Port:**
   - Truy vấn danh sách `SubOrder`, `Payment` và `Refund` trong khoảng thời gian xác định qua `ReportDataPort`.
4. **Tính toán đối soát tài chính (Revenue Report):**
   - Đảm bảo trọn vẹn 5 chỉ số tài chính cốt lõi:
     - `GMV = SUM(subtotalAmount)` của các đơn đã xác nhận/thanh toán.
     - `Tiền thực thu (Collected Cash) = GMV - Giảm giá (Discount)`.
     - `Hoàn tiền (Refunds) = SUM(refund.amount)` của các khoản hoàn tiền thành công.
     - `Hoa hồng sàn (Commission) = SUM(commissionAmount)`.
     - `Số thực nhận của Vendor (Net Vendor Payout) = Tiền thực thu - Hoa hồng sàn - Hoàn tiền Vendor chịu`.
5. **Phân loại đơn đặt & Lý do hủy (Booking Report):**
   - Đếm tổng số đơn phát sinh trong kỳ, số đơn hoàn thành (`COMPLETED`), số đơn hủy (`CANCELLED`/`REFUNDED`/`REJECTED`).
   - Phân rã chi tiết số đơn hủy theo từng lý do cụ thể trong `RefundReason`:
     - `CUSTOMER_CANCEL`: Khách hàng chủ động hủy.
     - `WEATHER`: Hủy do điều kiện thời tiết xấu hoặc cảnh báo an toàn biển.
     - `VENDOR_FAULT`: Lỗi từ nhà cung cấp dịch vụ.
     - `ADMIN_OVERRIDE`: Quản trị viên can thiệp xử lý.
     - `DISPUTE` / `COMPENSATION`: Bồi hoàn hoặc xử lý khiếu nại.
6. **Gom nhóm chu kỳ & Zero-filling:**
   - `TimeSeriesPeriodHelper` sinh đầy đủ các khung thời gian liên tục theo định dạng `GroupByPeriod` (ví dụ: `2026-09-01`, `2026-W36`, `2026-Q3`, `2026`).
   - Với các chu kỳ không có phát sinh giao dịch, hệ thống tự động gán giá trị mặc định bằng `0` (Zero-fill) thay vì bỏ qua, đảm bảo biểu đồ phân tích trên frontend hiển thị liên tục, mượt mà và không bị đứt đoạn.
7. Trả về `RevenueReportResponse` hoặc `BookingReportResponse` với mã HTTP 200 OK.

---

## 3. Report_Vendor_Performance_Diagnostics.png — Phân Tích Hiệu Suất & Động Cơ Chẩn Đoán Đối Tác

**Lớp xử lý chính:** `GetVendorPerformanceReportUseCase`, `VendorPerformanceItem`

**Luồng nghiệp vụ chi tiết:**
1. Quản trị viên gửi yêu cầu: `GET /api/admin/reports/vendors?from=&to=&vendorId=`.
2. Hệ thống xác định danh sách đối tác cần phân tích: toàn bộ các vendor đã được duyệt (`APPROVED`) hoặc một vendor cụ thể nếu có chỉ định.
3. Truy vấn đồng thời các tập dữ liệu liên quan trong kỳ:
   - Các đơn phụ `SubOrder` của vendor.
   - Các khoản hoàn tiền `Refund`.
   - Các suất hoạt động `ServiceSlot` (lấy `capacity` và `bookedCount`).
   - Các đánh giá `Review` thực tế từ khách hàng (lấy `rating`).
4. **Tính toán chỉ số vận hành chi tiết:**
   - Tính tổng doanh thu gộp (GMV), tiền thực nhận (Net Vendor Payout).
   - Tính tỷ lệ hủy: $\text{cancellationRate} = \frac{\text{Số đơn hủy}}{\text{Tổng số đơn}} \times 100\%$.
   - Tính tỷ lệ lấp đầy chỗ (Slot Occupancy Rate): $\text{slotOccupancyRate} = \frac{\sum \text{Suất đã đặt}}{\sum \text{Tổng công suất}} \times 100\%$ (có cơ chế phòng vệ chống lỗi chia cho 0 khi công suất bằng 0).
   - Tính điểm đánh giá chất lượng trung bình (Rating) và số lượt đánh giá.
5. **Động cơ chẩn đoán thông minh (Rule-based Diagnostic Engine):**
   - Đánh giá tự động các vấn đề vận hành (`VendorIssueType`):
     - `HIGH_CANCELLATION`: Khi tỷ lệ hủy đơn $> 20\%$.
     - `LOW_RATING`: Khi điểm đánh giá $< 3.5$ với tối thiểu 3 lượt đánh giá.
     - `OVERLOADED`: Khi tỷ lệ lấp đầy chỗ $\ge 95\%$ (nguy cơ quá tải hoặc thiếu chỗ).
     - `UNDERPERFORMING`: Khi tỷ lệ lấp đầy $< 20\%$ và tổng số đơn đặt $< 5$.
   - Phân cấp mức độ nghiêm trọng (`VendorAlertSeverity`):
     - **`CRITICAL` (Báo động đỏ):** Tỷ lệ hủy $> 40\%$ hoặc điểm đánh giá $< 2.5$ (với từ 5 lượt đánh giá trở lên) — Cần Admin can thiệp đình chỉ hoặc kiểm tra ngay.
     - **`WARNING` (Cảnh báo vàng):** Có ít nhất 1 vấn đề thuộc 4 nhóm trên — Cần nhắc nhở đối tác cải thiện chất lượng hoặc điều chỉnh công suất.
     - **`HEALTHY` (Xanh an toàn):** Hoạt động kinh doanh ổn định, chất lượng tốt.
6. Trả về `VendorPerformanceResponse` giúp ban quản trị đưa ra quyết định kinh doanh chính xác thay vì chỉ cộng tổng số liệu tĩnh.

---

## 4. Report_Export_And_VendorIsolation.png — Cơ Chế Cô Lập Đa Người Thuê & Xuất Báo Cáo CSV

**Lớp xử lý chính:** `ExportReportCsvUseCase`, `Rfc4180Utf8BomCsvExporter`, `VendorReportController`, `AdminReportController`

**Luồng nghiệp vụ chi tiết:**
1. **Kiểm soát bảo mật cô lập đa người thuê (Multi-tenant Vendor Isolation):**
   - Khi Vendor gọi `GET /api/vendor/reports/export`:
     - Bộ lọc bảo mật kiểm tra `@PreAuthorize("hasRole('VENDOR')")`.
     - Tuyệt đối **không nhận tham số `vendorId` từ client query string**.
     - `SecurityUtils.getCurrentUserId()` trích xuất `userId` từ token JWT đã xác thực.
     - `VendorInternalApi.findByUserId(userId)` tìm kiếm hồ sơ Vendor tương ứng.
     - Ràng buộc cứng `vendorId` vào phạm vi truy vấn của Use Case, loại trừ 100% nguy cơ tấn công **IDOR (Insecure Direct Object References)**.
   - Khi Admin gọi `GET /api/admin/reports/export`:
     - Được phép xuất báo cáo hợp nhất toàn sàn hoặc lọc theo `vendorId` của từng đối tác.
2. **Kích hoạt Use Case xuất dữ liệu:**
   - Dựa trên tham số `type` (`revenue`, `bookings`, `vendors`), UseCase gọi các phương thức tương ứng trên `ReportDataPort` để lấy tập dữ liệu đầy đủ.
3. **Bộ máy định dạng CSV chuẩn RFC 4180 & UTF-8 BOM (`Rfc4180Utf8BomCsvExporter`):**
   - **Xử lý mã hóa tiếng Việt:** Gắn tiền tố **3 byte UTF-8 BOM (`0xEF, 0xBB, 0xBF`)** vào đầu mảng byte. Điều này cho phép Microsoft Excel trên Windows/macOS tự động nhận diện bảng mã UTF-8 và hiển thị tiếng Việt có dấu chuẩn xác mà không bị vỡ font.
   - **Tuân thủ chuẩn RFC 4180:**
     - Các trường dữ liệu có chứa dấu phẩy (`,`), dấu nháy kép (`"`) hoặc dấu xuống dòng được bao bọc tự động trong dấu nháy kép và escape `""`.
     - Phân cách các dòng dữ liệu bằng chuẩn ngắt dòng CRLF (`\r\n`).
4. **Phản hồi tải xuống tệp:**
   - Thiết lập các HTTP Headers chuẩn:
     - `Content-Type: text/csv; charset=UTF-8`
     - `Content-Disposition: attachment; filename="report-{type}-{from}-{to}.csv"`
   - Trả về dòng byte dữ liệu cho trình duyệt người dùng tự động kích hoạt tải xuống tệp CSV hoàn chỉnh.
