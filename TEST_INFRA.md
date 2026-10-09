# E2E Test Infra: Danasea Reports & Statistics Dashboard

## Test Philosophy
- Kiểm thử hướng yêu cầu (Requirement-driven), kiểm thử đa tầng (Multi-tier testing: Category-Partition, Boundary Value Analysis, Pairwise Combinatorial, Real-World Scenarios).
- Đảm bảo tính toán tài chính chính xác tuyệt đối, không sai lệch số học hoặc làm tròn số tiền tệ.
- Bảo đảm đa người thuê (Multi-tenant) và Vendor Isolation an toàn 100%.

## Feature Inventory
| # | Feature | Source (requirement) | Tier 1 (Coverage) | Tier 2 (Boundary) | Tier 3 (Pairwise) |
|---|---------|---------------------|:-----------------:|:-----------------:|:-----------------:|
| F1 | Admin Dashboard Metrics | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F2 | Vendor Dashboard Metrics | ORIGINAL_REQUEST §R1 | 5 | 5 | ✓ |
| F3 | Multi-tenant & Vendor Isolation | ORIGINAL_REQUEST §R1, §AC | 5 | 5 | ✓ |
| F4 | Time-Series Revenue Analytics | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| F5 | Time-Series Booking Analytics | ORIGINAL_REQUEST §R2 | 5 | 5 | ✓ |
| F6 | Asia/Ho_Chi_Minh Timezone (UTC+7) | ORIGINAL_REQUEST §R2, §AC | 5 | 5 | ✓ |
| F7 | Financial Math & Formula Integrity | ORIGINAL_REQUEST §R2, §AC | 5 | 5 | ✓ |
| F8 | Vendor Performance & Insights | ORIGINAL_REQUEST §R3 | 5 | 5 | ✓ |
| F9 | Standardized UTF-8 BOM CSV Export | ORIGINAL_REQUEST §R4 | 5 | 5 | ✓ |
| F10 | API Contract & Error Responses | ORIGINAL_REQUEST §AC | 5 | 5 | ✓ |
| F11 | API Inventory Guard Sync | Explorer Survey 3 | 5 | 2 | ✓ |

## Test Architecture
- **Runner**: `./mvnw test`
- **Unit Testing**: JUnit 5 + Mockito + AssertJ cho Domain models, UseCases và CSV Exporter.
- **Controller Slice Testing**: `MockMvcBuilders.standaloneSetup` + Mockito + Jackson JsonPath cho HTTP verification nhanh và chuẩn xác.
- **Integration & Security Testing**: Spring Test + `@WithMockUser` / `SecurityUtils` + Testcontainers Postgres & H2 in-memory.
- **File Output Format**: CSV RFC 4180 đính kèm tiền tố 3 byte BOM `\uFEFF` (`0xEF, 0xBB, 0xBF`).

## Test Tiers & Scenarios

### Tier 1 - Feature Coverage (≥5 per feature)
- Happy-path test cho từng endpoint độc lập.
- Thống kê doanh thu theo day/week/quarter/year.
- Thống kê đơn hoàn thành, hủy theo lý do.
- Xuất CSV cho 3 loại (revenue, bookings, vendors).

### Tier 2 - Boundary & Corner Cases (≥5 per feature)
- Khoảng ngày `from` = `to` (chỉ 1 ngày duy nhất).
- Khoảng ngày không phát sinh bất kỳ đơn hàng nào -> kiểm tra zero-filling data points.
- Giao dịch diễn ra tại `23:59:59.999` hoặc `00:00:00.001` giờ Việt Nam -> kiểm tra không bị nhảy sang ngày khác.
- Đơn hàng bị hủy do thời tiết (`WEATHER`) -> kiểm tra hoàn 100%, sàn không thu hoa hồng, vendor payout = 0.
- Đơn hàng hủy do khách (`CUSTOMER_CANCEL`) trước 2h -> tiền hoàn 0đ, chia theo tỷ lệ hoa hồng.
- Slot có `capacity = 0` hoặc chưa có người đặt -> tỷ lệ lấp đầy = `0.0%`, không chia cho 0 (ZeroDivisionError).
- Tham số không hợp lệ: `from` sau `to`, `groupBy` sai cú pháp, `type` export không hỗ trợ -> trả về HTTP 400 `INVALID_INPUT`.

### Tier 3 - Cross-Feature Combinations (Pairwise Coverage)
- Vendor A gọi API xuất CSV doanh thu -> kiểm tra chỉ xuất đúng đơn của Vendor A trong kỳ chỉ định.
- Admin gọi API thống kê doanh thu có truyền `vendorId` -> lọc đúng theo vendor; không truyền `vendorId` -> tổng hợp toàn sàn.
- Kiểm tra tính toán đồng thời: Đơn có mã giảm giá (voucher sàn) kết hợp hoàn tiền một phần -> đối soát đúng GMV, Cash, Commission, Payout.

### Tier 4 - Real-World Application Scenarios
1. **Kịch bản Mùa Bão (Weather Disruption)**: Đột ngột 50 đơn hàng bị hủy do `WEATHER` trong ngày mưa bão -> Kiểm tra Dashboard phản ánh đúng tỷ lệ hủy tăng vọt, phát sinh cảnh báo bất thường `HIGH_CANCELLATION`, tài chính hoàn 100% không trừ phí vendor.
2. **Kịch bản Cao Điểm Lễ Hội (Peak Season Overload)**: Các slot của vendor đạt 98% công suất -> Cảnh báo `OVERLOADED` kích hoạt, tỷ lệ lấp đầy slot phản ánh chính xác.
3. **Kịch bản Đối Soát Tài Chính Đa Đối Tác (Monthly Settlement Cross-check)**: 3 vendor khác nhau phát sinh tổng cộng 100 đơn hàng qua nhiều cổng thanh toán (VNPAY, PAYPAL) với các mức chiết khấu và hoàn tiền khác nhau -> Tổng doanh thu toàn sàn khớp chính xác 100% với tổng doanh thu từng đối tác cộng lại.
4. **Kịch bản Xuất Báo Cáo Kế Toán Excel**: Kế toán tải file CSV qua endpoint `/api/admin/reports/export?type=revenue` và mở bằng Excel -> Tiếng Việt có dấu ("Doanh thu", "Hoa hồng") hiển thị mượt mà không lỗi font, các cột phân tách đúng chuẩn.
5. **Kịch bản Kiểm Tra Đột Nhập Trái Phép (Security Penetration)**: Vendor B cố tình truyền `vendorId` của Vendor A hoặc gọi API admin -> Bị từ chối HTTP 403 Forbidden ngay lập tức.

## Verified review fixes — 09/10/2026

`./mvnw clean verify`: BUILD SUCCESS, 1875 tests, 0 failures/errors, 135 skipped (existing external/infrastructure exclusions). Report module: 288 tests, 0 failures/errors/skipped. `ReportFinancialIntegrationTest`: 14 cases using PostgreSQL 16, an isolated schema, real Flyway migrations and Hibernate validation. Migration integration verifies V17 → V22 and payment timestamp backfill. Financial periods use paid_at/processed_at and exclusive upper boundaries; refund-only periods retain signed adjustments. See the report contract for booking cohort definitions and legacy timestamp limitations.
