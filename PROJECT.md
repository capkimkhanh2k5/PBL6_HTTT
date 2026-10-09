# Project: Danasea Business Reports & Statistics Dashboard

## Architecture
Hệ thống Thống kê & Báo cáo kinh doanh Danasea được xây dựng thành một Module độc lập tuân thủ nghiêm ngặt mô hình Clean Architecture & Modular Monolith của dự án:
- **Package root**: `com.danasea.backend.modules.report`
- **Tầng Domain (`com.danasea.backend.modules.report.domain`)**:
  - `models`: `DashboardMetrics`, `RevenueReportItem`, `BookingReportItem`, `VendorPerformanceItem`, `TimePeriod`, `VendorDiagnosisAlert`.
  - `enums`: `GroupByPeriod` (`DAY`, `WEEK`, `QUARTER`, `YEAR`), `ReportType` (`REVENUE`, `BOOKINGS`, `VENDORS`), `VendorAlertSeverity` (`HEALTHY`, `WARNING`, `CRITICAL`), `VendorIssueType`.
- **Tầng Application (`com.danasea.backend.modules.report.application`)**:
  - `ports`: `ReportDataPort` (cổng trích xuất dữ liệu tổng hợp), `CsvExporterPort` (cổng xuất CSV).
  - `usecases`:
    - `GetAdminDashboardUseCase`: Tổng hợp chỉ số toàn sàn thời gian thực + cảnh báo bất thường.
    - `GetVendorDashboardUseCase`: Tổng hợp chỉ số riêng cho vendor thời gian thực + cảnh báo lấp đầy/hủy.
    - `GetRevenueReportUseCase`: Thống kê tài chính time-series (GMV, Cash, Refunds, Commission, Net Payout) theo chu kỳ (day, week, quarter, year) múi giờ `Asia/Ho_Chi_Minh`.
    - `GetBookingReportUseCase`: Thống kê đơn đặt time-series phân loại chi tiết theo lý do hủy (CUSTOMER_CANCEL, WEATHER, VENDOR_FAULT, ADMIN_OVERRIDE, v.v.).
    - `GetVendorPerformanceReportUseCase`: Phân tích hiệu suất từng đối tác (doanh thu, đơn, tỷ lệ hủy, slot occupancy rate, rating) và phân loại chẩn đoán thông minh.
    - `ExportReportCsvUseCase`: Xuất báo cáo định dạng CSV chuẩn RFC 4180 có UTF-8 BOM.
- **Tầng Infrastructure (`com.danasea.backend.modules.report.infrastructure`)**:
  - `persistence`: `JpaReportDataAdapter` thực thi các câu truy vấn JPQL/SQL kết hợp Java time-series grouping, đảm bảo tương thích 100% trên cả H2 (in-memory test) và PostgreSQL (production).
  - `csv`: `Rfc4180Utf8BomCsvExporter` thuần Java không phụ thuộc thư viện ngoài, xuất BOM `0xEF, 0xBB, 0xBF`.
- **Tầng Presentation (`com.danasea.backend.modules.report.presentation`)**:
  - `AdminDashboardController` / cập nhật `AdminController`: Cung cấp `GET /api/admin/dashboard` (bảo tồn thuộc tính `status: "ADMIN_ACCESS_GRANTED"` để tương thích các test bảo mật RBAC).
  - `VendorDashboardController`: Cung cấp `GET /api/vendor/dashboard`.
  - `AdminReportController`: Cung cấp `GET /api/admin/reports/revenue`, `GET /api/admin/reports/bookings`, `GET /api/admin/reports/vendors`, `GET /api/admin/reports/export`.
  - `VendorReportController`: Cung cấp `GET /api/vendor/reports/revenue`, `GET /api/vendor/reports/bookings`, `GET /api/vendor/reports/export`.
  - `dtos`: Strongly-typed DTOs cho các tham số và phản hồi.

---

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F1 | Admin Real Dashboard Metrics | Thay thế mock tại `GET /api/admin/dashboard` bằng dữ liệu thực (doanh thu, đơn mới, tỷ lệ hoàn tất, tỷ lệ hủy, cảnh báo đơn vị quá tải / hủy cao bất thường), giữ trường `status: ADMIN_ACCESS_GRANTED`. | M3 | R1 |
| F2 | Vendor Real Dashboard Metrics | Cung cấp `GET /api/vendor/dashboard` với các chỉ số hoạt động thời gian thực của chính vendor đó, cảnh báo quá tải slot và tỷ lệ hủy. | M3 | R1 |
| F3 | Multi-tenant & Vendor Isolation | Đảm bảo Vendor chỉ truy cập dữ liệu của chính mình qua `SecurityUtils.getCurrentUserId()` + `VendorInternalApi.findByUserId()`, cấm truyền vendorId từ client. Admin có thể xem toàn sàn hoặc filter theo vendorId. | M3 | R1, R2, R3, R4 |
| F4 | Time-Series Revenue Analytics (Admin & Vendor) | `GET /api/admin/reports/revenue` và `GET /api/vendor/reports/revenue` hỗ trợ `from`, `to`, `groupBy=day\|week\|quarter\|year`. Phân rã GMV, Collected Cash, Refunds, Commission, Net Vendor Payout. | M2 | R2 |
| F5 | Time-Series Booking Analytics (Admin & Vendor) | `GET /api/admin/reports/bookings` và `GET /api/vendor/reports/bookings` hỗ trợ `from`, `to`, `groupBy`. Tổng số đơn, số hoàn thành, số hủy và phân loại chi tiết theo lý do hủy (`CUSTOMER_CANCEL`, `WEATHER`, `VENDOR_FAULT`, `ADMIN_OVERRIDE`, v.v.). | M2 | R2 |
| F6 | Asia/Ho_Chi_Minh Timezone Integrity | Đồng nhất múi giờ `Asia/Ho_Chi_Minh` (UTC+7). Mốc ghi nhận thanh toán `PaymentStatus.SUCCESS`. Khoảng lọc bao phủ trọn vẹn từ 00:00:00.000000 đến 23:59:59.999999. Zero-filling đầy đủ các chu kỳ trống. | M2 | R2 |
| F7 | Financial Math & Formula Reconciliation | Bảo đảm toàn vẹn tài chính: `Collected Cash = GMV - Discount`; `Net Vendor Payout = Collected Cash - Platform Commission - Vendor Refunds`. | M1 | R2, AC |
| F8 | Vendor Performance & Diagnostic Insights | `GET /api/admin/reports/vendors` phân tích doanh thu, số đơn, tỷ lệ hủy, slot occupancy rate (`SUM(booked)/SUM(capacity)`), rating/review. Cơ chế chẩn đoán thông minh (`HEALTHY`, `WARNING`, `CRITICAL`). | M2 | R3 |
| F9 | Standardized UTF-8 BOM CSV Export | `GET /api/admin/reports/export` và `GET /api/vendor/reports/export` hỗ trợ `type=revenue\|bookings\|vendors`, xuất file CSV chuẩn RFC 4180 có UTF-8 BOM (`\uFEFF`), hỗ trợ Excel tiếng Việt không lỗi font. | M2 | R4 |
| F10 | API Contract & Error Handling | Tất cả endpoint trả về HTTP 200 JSON chuẩn, validation lỗi trả về HTTP 400 qua `GlobalExceptionHandler`, header CSV đúng `Content-Type: text/csv; charset=UTF-8` và `Content-Disposition`. | M3 | AC |
| F11 | API Inventory Guard Synchronization | Cập nhật số lượng endpoint trong `BackendApplicationTests` tương ứng với số lượng endpoint mới bổ sung để bảo toàn test kiểm soát inventory. | M3 | Survey Explorer 3 |
| F12 | Full Programmatic Verification Suite | 100% Unit Tests & Integration Tests (MockMvc) kiểm chứng phân nhóm thời gian, hủy/hoàn tiền, RBAC phân quyền, Vendor Isolation, CSV format. `./mvnw test` pass 100%. | M4 | AC |

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Domain Models & Persistence Layer | Định nghĩa Domain Models, Enums, `ReportDataPort`, và `JpaReportDataAdapter` truy vấn dữ liệu từ `sub_orders`, `payments`, `refunds`, `service_slots`, `vendors`, `reviews`. Kiểm chứng công thức tài chính. | None | DONE |
| M2 | Business Logic, Diagnostics & CSV Export | Cài đặt toàn bộ UseCases (`GetAdminDashboardUseCase`, `GetVendorDashboardUseCase`, `GetRevenueReportUseCase`, `GetBookingReportUseCase`, `GetVendorPerformanceReportUseCase`, `ExportReportCsvUseCase`), thuật toán phân nhóm time-series zero-filling múi giờ UTC+7, và tiện ích xuất CSV chuẩn UTF-8 BOM. Viết Unit Tests đầy đủ. | M1 | DONE |
| M3 | Presentation, Controllers & Security Isolation | Cài đặt các Controllers, DTOs, Request Validation, xử lý `@PreAuthorize`, cơ chế giải quyết `vendorId` bảo đảm Vendor Isolation. Cập nhật `AdminController.dashboard` (giữ `status: ADMIN_ACCESS_GRANTED`), cập nhật `BackendApplicationTests`. Viết MockMvc Unit Tests cho toàn bộ endpoints. | M2 | DONE |
| M4 | E2E Testing, RBAC Verification & Final Pass | Chạy toàn bộ test suite dự án (`./mvnw test`), kiểm thử tích hợp bảo mật, cô lập dữ liệu đa người thuê (Multi-tenant), kiểm thử biên ngày tháng và xuất file CSV. Đảm bảo 100% tests pass. | M3 | DONE |

---

## Interface Contracts
### Module Report ↔ Security & Vendor Module
- **Vendor Resolution**:
  - `SecurityUtils.getCurrentUserId()` -> `Optional<UUID> userId`.
  - `VendorInternalApi.findByUserId(userId)` -> `Optional<Vendor>`.
  - Nếu không tìm thấy Vendor: ném `AccessDeniedException("Vendor profile not found for current user")` (HTTP 403).
- **Time Filter Contract**:
  - Input: `LocalDate from, LocalDate to`.
  - Conversion:
    - `startDateTime = from.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime()` (00:00:00+07:00).
    - `endDateTime = to.atTime(LocalTime.MAX).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime()` (23:59:59.999999+07:00).
  - Validation: Nếu `from.isAfter(to)` -> ném `IllegalArgumentException("From date cannot be after to date")` (HTTP 400).
- **CSV Export Contract**:
  - Param: `String type` (`revenue`, `bookings`, `vendors`), `LocalDate from`, `LocalDate to`.
  - Header: `Content-Type: text/csv; charset=UTF-8`, `Content-Disposition: attachment; filename="report-{type}-{from}-{to}.csv"`.
  - Body: `byte[]` bắt đầu bằng `0xEF, 0xBB, 0xBF`.

---

## Code Layout
- `backend/src/main/java/com/danasea/backend/modules/report/`:
  - `domain/models/`: `DashboardMetrics.java`, `RevenueReportItem.java`, `BookingReportItem.java`, `VendorPerformanceItem.java`, `TimePeriod.java`, `VendorDiagnosisAlert.java`.
  - `domain/enums/`: `GroupByPeriod.java`, `ReportType.java`, `VendorAlertSeverity.java`.
  - `application/ports/`: `ReportDataPort.java`, `CsvExporterPort.java`.
  - `application/usecases/`:
    - `GetAdminDashboardUseCase.java`
    - `GetVendorDashboardUseCase.java`
    - `GetRevenueReportUseCase.java`
    - `GetBookingReportUseCase.java`
    - `GetVendorPerformanceReportUseCase.java`
    - `ExportReportCsvUseCase.java`
  - `infrastructure/persistence/`: `JpaReportDataAdapter.java`, `SpringDataReportOrderRepository.java` (hoặc truy vấn qua EntityManager/Criteria).
  - `infrastructure/csv/`: `Rfc4180Utf8BomCsvExporter.java`.
  - `presentation/controllers/`:
    - `AdminDashboardController.java` (hoặc cập nhật `AdminController.java`)
    - `VendorDashboardController.java`
    - `AdminReportController.java`
    - `VendorReportController.java`
  - `presentation/dtos/`: `AdminDashboardResponse.java`, `VendorDashboardResponse.java`, `RevenueReportResponse.java`, `BookingReportResponse.java`, `VendorPerformanceResponse.java`, các DTOs chi tiết.
- `backend/src/test/java/com/danasea/backend/modules/report/`:
  - `domain/`: Unit tests cho domain logic và financial math.
  - `application/`: Unit tests cho tất cả UseCases và CSV Exporter.
  - `presentation/`: MockMvc standalone tests cho các Controller.
  - `integration/`: Security & Vendor isolation tests.

## Review fixes — 09/10/2026

Reports and dashboards share cash/refund ledger events: immutable payment `paid_at`, processed refund `processed_at`, signed commission/payout adjustments, exact cash allocation across vendors, and exclusive date boundaries. Booking reports classify creation cohorts and count each cancellation once, with persisted reasons and an explicit unknown count. CSV neutralizes business-name formulas. Range limit: 3660 inclusive days. See `backend/docs/reports-dashboard-contract.md` for historical-data limitations and the complete API contract.
