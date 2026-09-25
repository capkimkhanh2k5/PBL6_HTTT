# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE CHECK-IN (QR CODE)

> Module Check-In cung cấp cơ chế tạo và xác thực mã QR an toàn phục vụ khách hàng làm thủ tục check-in trải nghiệm dịch vụ tại thực địa với đối tác/vendor.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `CheckIn_GenerateQR` | Sequence Diagram | Khách hàng khởi tạo mã QR check-in: Dual-resolution (hỗ trợ cả SubOrder ID và MasterOrder ID), kiểm tra IDOR, ràng buộc trạng thái đơn hàng và ký số HMAC-SHA256 |
| 2 | `CheckIn_VendorVerify` | Sequence Diagram | Vendor/Nhân viên quét và xác thực mã QR: Kiểm tra tính toàn vẹn chữ ký HMAC (constant-time), chống tấn công phát lại (Anti-Replay), kiểm tra khung giờ hợp lệ (`CheckinWindowPolicy`) và cập nhật trạng thái đơn hàng |

---

## 1. CheckIn_GenerateQR.png — Khách Hàng Tạo Mã QR Check-in

**Lớp xử lý chính:** `com.danasea.backend.modules.checkin.application.usecases.GenerateCheckinQrUseCase`

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu qua API: `GET /api/bookings/{identifier}/qr-code`.
2. **Dual-Resolution:** `identifier` truyền vào có thể là mã định danh của `SubOrder` hoặc `MasterOrder`. UseCase sẽ phân giải và tìm đúng SubOrder tương ứng.
3. **IDOR Check:** Kiểm tra quyền sở hữu đơn hàng (`masterOrder.customerId == currentUserId`). Nếu không khớp, chặn truy cập với lỗi `403 UnauthorizedCheckinAccessException`.
4. **Validate trạng thái:** Đơn phụ bắt buộc phải ở trạng thái `CONFIRMED`. Nếu đơn đang ở trạng thái `PENDING`, `CANCELLED` hoặc `REFUNDED`, hệ thống từ chối tạo mã QR với `400 InvalidSubOrderStateException`.
5. **Ký số bảo mật (HMAC-SHA256):** `QrTokenSigner` thực hiện ký số payload chứa thông tin SubOrder và thời gian hết hạn (`expiryTime`) để chống giả mạo token.
6. Lưu trữ thông tin token vào bảng `checkin_tokens` (`CheckinTokenJpaEntity`) và trả về token kèm URL mã QR cho khách hàng.

---

## 2. CheckIn_VendorVerify.png — Vendor Quét & Xác Thực Check-in

**Lớp xử lý chính:** `com.danasea.backend.modules.checkin.application.usecases.VerifyCheckinUseCase`

**Luồng nghiệp vụ chi tiết:**
1. Nhân viên của Vendor sử dụng ứng dụng quét mã QR và gửi yêu cầu `POST /api/vendor/checkin/verify` kèm chuỗi `{qrToken}`.
2. **Kiểm tra chữ ký (Constant-time verification):** `QrTokenSigner.verifySignature()` xác thực tính hợp lệ của chữ ký HMAC. Nếu token bị can thiệp chỉnh sửa, trả về ngay `400 InvalidQrSignatureException`.
3. **Kiểm tra trạng thái & Vòng đời Token:**
   - Token không tồn tại trong hệ thống: `404 CheckinTokenNotFoundException`.
   - Token đã được sử dụng trước đó (`used == true`): `409 QrTokenAlreadyUsedException` (ngăn chặn hành vi dùng lại mã vé).
   - Token đã quá hạn thời gian: `400 QrTokenExpiredException`.
4. **Vendor Ownership Check:** Đối chiếu mã `vendorId` của slot dịch vụ với `staffVendorId` của nhân viên thực hiện quét để đảm bảo vendor chỉ check-in được dịch vụ thuộc quyền quản lý của mình.
5. **Khung giờ check-in (`CheckinWindowPolicy`):** Xác minh thời điểm quét QR có nằm trong cửa sổ cho phép (trước và sau giờ bắt đầu slot dịch vụ). Nếu quá sớm hoặc quá trễ, trả về `400 InvalidCheckinStateException`.
6. **Hoàn tất Check-in:**
   - Đánh dấu token đã sử dụng (`token.used = true`, ghi nhận `usedAt` và `usedByStaffId`).
   - Cập nhật trạng thái đơn phụ: `SubOrder.status = CHECKED_IN`.
   - Phản hồi kết quả check-in thành công cho Vendor.
