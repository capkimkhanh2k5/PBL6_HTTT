# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE SETTLEMENT (QUYẾT TOÁN DOANH THU)

> Module Settlement chịu trách nhiệm tổng hợp, tính toán đối soát và chốt doanh thu định kỳ giữa nền tảng Danasea và các đối tác dịch vụ (Vendor), xử lý khấu trừ hoa hồng, hoàn tiền và loại trừ các đơn đang vướng khiếu nại.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Settlement_Generate` | Sequence Diagram | Admin tạo bảng quyết toán DRAFT cho Vendor: kiểm tra khóa chống trùng lặp, lọc SubOrders theo khung thời gian trải nghiệm, khấu trừ hoàn tiền đã xử lý (`PROCESSED`), tự động loại trừ các đơn có khiếu nại đang mở (`OPEN`/`UNDER_REVIEW`) |
| 2 | `Settlement_Finalize` | Sequence Diagram | Admin phê duyệt chốt bảng quyết toán: Sử dụng Pessimistic Write Lock ngăn ngừa Race Condition, chuyển trạng thái từ `DRAFT` sang `FINALIZED` và ghi nhận lịch sử kiểm toán (Audit Log) |
| 3 | `Settlement_Vendor` | Sequence Diagram | Vendor tra cứu danh sách các kỳ quyết toán và xem chi tiết bảng đối soát kèm từng mục đơn hàng (Line Items), được kiểm soát truy cập an toàn qua IDOR |

---

## 1. Settlement_Generate.png — Admin Khởi Tạo Bảng Quyết Toán

**Lớp xử lý chính:** `com.danasea.backend.modules.settlement.application.usecases.GenerateSettlementUseCase`

**Luồng nghiệp vụ chi tiết:**
1. Quản trị viên gửi lệnh `POST /api/admin/settlements/generate` với tham số `{vendorId, periodStart, periodEnd}`.
2. Kiểm tra tính hợp lệ của mốc thời gian: `periodStart <= periodEnd`.
3. Kiểm tra khóa chống trùng lặp (**Idempotency Key**) được tổng hợp từ `(vendorId, periodStart, periodEnd)`. Nếu kỳ quyết toán này đã từng được tạo, hệ thống tái sử dụng hoặc cập nhật bản nháp tương ứng.
4. Truy vấn danh sách các đơn phụ `SubOrder` của Vendor có trạng thái `COMPLETED` hoặc `PARTIALLY_REFUNDED`.
5. Lọc những đơn có ngày trải nghiệm thực tế (`slot.date`) nằm trong khoảng thời gian `[periodStart, periodEnd]`.
6. Batch query danh sách các khoản hoàn tiền thành công (chỉ lấy trạng thái `PROCESSED`) và các khiếu nại chưa đóng (`OPEN`, `UNDER_REVIEW`).
7. **Động cơ tính toán đối soát (`SettlementCalculationEngine`):**
   - Với mỗi SubOrder, tính toán:
     - `grossAmount = subtotal - refundAmount`
     - `commissionAmount = grossAmount * commissionRate` (tỷ lệ hoa hồng nền tảng)
     - `netAmount = grossAmount - commissionAmount` (số tiền thực nhận của Vendor)
   - **Cơ chế phòng vệ tài chính:** Nếu SubOrder có khiếu nại đang mở, dòng đối soát sẽ được đánh dấu trạng thái **`EXCLUDED`** (kèm lý do `ACTIVE_DISPUTE`) và không được cộng dồn vào tổng tiền quyết toán của kỳ này.
8. Lưu trữ bảng quyết toán chính `SettlementJpaEntity` ở trạng thái `DRAFT` cùng các dòng chi tiết `SettlementLineItemJpaEntity`.
9. Phản hồi kết quả bảng quyết toán nháp cho Admin kiểm tra.

---

## 2. Settlement_Finalize.png — Admin Phê Duyệt Chốt Quyết Toán

**Lớp xử lý chính:** `com.danasea.backend.modules.settlement.application.usecases.FinalizeSettlementUseCase`

**Luồng nghiệp vụ chi tiết:**
1. Admin gửi yêu cầu `PATCH /api/admin/settlements/{id}/finalize`.
2. Áp dụng cơ chế khóa ghi bi quan (**Pessimistic Write Lock** qua `findByIdForUpdate`) để tránh xung đột dữ liệu khi nhiều quản trị viên cùng thao tác đồng thời.
3. Kiểm tra trạng thái hiện tại của Settlement:
   - Nếu bảng quyết toán đã ở trạng thái `FINALIZED` hoặc `PAID`, ném ngoại lệ `409 SettlementAlreadyFinalizedException`.
   - Nếu ở trạng thái `DRAFT`, cập nhật trạng thái mới thành `FINALIZED`.
4. Gọi `AuditLogInternalApi.recordAuditLog("FINALIZE_SETTLEMENT")` để lưu vết phục vụ công tác thanh tra kiểm toán tài chính.
5. Trả về kết quả quyết toán đã được phê duyệt thành công.

---

## 3. Settlement_Vendor.png — Vendor Tra Cứu Bảng Quyết Toán

**Lớp xử lý chính:** `GetSettlementsUseCase` & `GetSettlementDetailUseCase`

**Luồng nghiệp vụ chi tiết:**
1. **Xem danh sách các kỳ quyết toán:**
   - Vendor gọi `GET /api/vendor/settlements?page=0&size=10`.
   - Hệ thống tự động trích xuất `currentVendorId` từ thông tin phiên đăng nhập để phân trang danh sách quyết toán, đảm bảo tính riêng tư dữ liệu.
2. **Xem chi tiết đối soát từng đơn hàng:**
   - Vendor gọi `GET /api/vendor/settlements/{id}`.
   - **IDOR Check:** Kiểm tra `settlement.vendorId == currentVendorId`. Nếu phát hiện truy cập trái phép bảng quyết toán của vendor khác, ném ngoại lệ `403 Forbidden`.
   - Nạp thông tin tổng hợp kèm toàn bộ danh sách `SettlementLineItem` (thể hiện rõ từng đơn hàng đã hoàn tất, đơn bị trừ hoàn tiền hoặc đơn bị loại trừ do khiếu nại).
