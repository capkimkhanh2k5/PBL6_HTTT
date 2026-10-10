# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE BOOKING (GIỮ CHỖ & KHÓA TỒN KHO)

> Module Booking chịu trách nhiệm xử lý luồng đặt chỗ dịch vụ du lịch biển, cơ chế khóa chỗ phân tán trên Redis (Distributed Inventory Lock) chống vượt hạn mức (Overbooking), tự động giải phóng chỗ khi quá hạn (TTL 15 phút) và chính sách hủy chỗ theo mốc thời gian.

| # | Sơ đồ | Loại | Mã nguồn Mermaid (.mmd) | Nội dung |
|---|-------|------|--------------------------|----------|
| 1 | `Booking_Hold_InventoryLock_DF` | Decision Flowchart | [`codeFlows/Decision_Flowchart/Booking_Hold_InventoryLock_DF.mmd`](codeFlows/Decision_Flowchart/Booking_Hold_InventoryLock_DF.mmd) | Cây quyết định kiểm tra Auth, Payload, tồn kho DB PostgreSQL và khóa phân tán Redis Lua Script nguyên tử |
| 2 | `Booking_Cancel_And_Refund_Window_DF` | Decision Flowchart | [`codeFlows/Decision_Flowchart/Booking_Cancel_And_Refund_Window_DF.mmd`](codeFlows/Decision_Flowchart/Booking_Cancel_And_Refund_Window_DF.mmd) | Cây quyết định hủy booking: IDOR Guard, phân nhánh đơn HOLD vs CONFIRMED, chính sách mốc 24h (>24h hoàn 100%, <=24h hoàn 0%) |
| 3 | `Booking_Hold_InventoryLock_SD` | Sequence Diagram | [`codeFlows/Sequence_Diagram/Booking_Hold_InventoryLock_SD.mmd`](codeFlows/Sequence_Diagram/Booking_Hold_InventoryLock_SD.mmd) | Khách hàng khởi tạo giữ chỗ (`POST /api/bookings/hold`): Kiểm tra slot, khóa nguyên tử nhiều slot trên Redis qua Lua Script (`SETNX`/TTL 15 phút), tạo bản ghi Booking trạng thái `HOLD` |
| 4 | `Booking_Hold_Cleanup_And_Expiry_SD` | Sequence Diagram | [`codeFlows/Sequence_Diagram/Booking_Hold_Cleanup_And_Expiry_SD.mmd`](codeFlows/Sequence_Diagram/Booking_Hold_Cleanup_And_Expiry_SD.mmd) | Scheduled Background Job (`BookingHoldCleanupJob`): Quét định kỳ các booking `HOLD` / `PENDING_PAYMENT` đã hết hạn TTL, giải phóng Redis lock, hủy booking và bắn sự kiện `BookingHoldExpiredEvent` |
| 5 | `Booking_Cancel_And_Refund_Window_SD` | Sequence Diagram | [`codeFlows/Sequence_Diagram/Booking_Cancel_And_Refund_Window_SD.mmd`](codeFlows/Sequence_Diagram/Booking_Cancel_And_Refund_Window_SD.mmd) | Khách hàng chủ động hủy booking (`PATCH /api/bookings/{id}/cancel`): Chống IDOR (403), áp dụng quy tắc mốc 24h, giải phóng tồn kho slot tương ứng |

---

## 1. Booking_Hold_InventoryLock_SD.png — Khởi Tạo Giữ Chỗ & Khóa Tồn Kho Redis

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu giữ chỗ qua API `POST /api/bookings/hold` kèm danh sách các slot `{slotId, quantity}` và tiêu đề `Idempotency-Key`.
2. **Kiểm tra tồn kho thực tế:** `CreateBookingHoldUseCase` duyệt qua từng service slot và gọi `ServiceSlotPort.getSlotCapacity(slotId)` từ cơ sở dữ liệu PostgreSQL. Nếu số lượng yêu cầu vượt quá chỗ trống thực tế (`capacity - bookedCount`), ném ngay ngoại lệ `409 Conflict (SlotCapacityExceededException)`.
3. **Khóa tồn kho phân tán nguyên tử:**
   - Gọi `RedisInventoryLockAdapter.acquireHolds(bookingId, items, ttl = 15 phút)`.
   - Thực thi **Redis Lua Script** đảm bảo tính nguyên tử (Atomic): Kiểm tra key `slot:{id}:held`, tăng số lượng giữ và thiết lập thời gian sống (TTL) 15 phút.
   - Nếu bất kỳ slot nào trong danh sách bị khách khác cạnh tranh giữ trước, rollback toàn bộ các slot đã giữ trong lượt và trả về `409 Conflict (InventoryLockFailedException)`.
4. **Lưu bản ghi Booking trạng thái `HOLD`:**
   - Khi đã khóa Redis thành công, hệ thống lưu bản ghi `BookingJpaEntity` với trạng thái ban đầu là `HOLD`, ghi nhận thời điểm hết hạn `holdExpiresAt = now + 15m`.
5. Phản hồi thông tin giữ chỗ `201 Created` kèm `bookingId`, danh sách items và mốc thời gian hết hạn thanh toán cho khách hàng.

---

## 2. Booking_Hold_Cleanup_And_Expiry_SD.png — Quét Hết Hạn & Giải Phóng Chỗ Tự Động

**Luồng nghiệp vụ chi tiết:**
1. Scheduled Background Job (`BookingHoldCleanupJob`) kích hoạt định kỳ mỗi **1 phút**.
2. **Truy vấn danh sách quá hạn:** Gọi `JpaBookingRepository.findExpiredHolds(now, ['HOLD', 'PENDING_PAYMENT'])` để lấy danh sách các booking đã quá thời hạn 15 phút mà chưa được thanh toán thành công.
3. **Giải phóng Redis Lock:** Với từng booking quá hạn, gọi `RedisInventoryLockAdapter.releaseHolds(...)` để thực thi script nguyên tử giảm `held count` trong Redis tương ứng với các slot.
4. **Cập nhật trạng thái Booking:** Cập nhật trạng thái booking sang `CANCELLED`.
5. **Kích hoạt sự kiện liên module (`BookingHoldExpiredEvent`):**
   - Bắn sự kiện sang Spring Application Event Bus.
   - `OrderExpiryEventListener` tiếp nhận sự kiện: kiểm tra `MasterOrder` tương ứng, nếu vẫn ở trạng thái `PENDING_PAYMENT` thì lập tức chuyển trạng thái sang `CANCELLED` để giải phóng đơn hàng; nếu đơn đã kịp `PAID` qua webhook thì an toàn bỏ qua.

---

## 3. Booking_Cancel_And_Refund_Window_SD.png — Khách Hàng Hủy Booking & Quy Tắc Mốc 24H

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu hủy booking qua `PATCH /api/bookings/{id}/cancel` kèm lý do `{reason}`.
2. **Kiểm tra quyền sở hữu (IDOR Check):** Xác minh `booking.customerId == currentUserId` hoặc người dùng có vai trò `ROLE_ADMIN`. Nếu vi phạm quyền truy cập, chặn ngay với mã lỗi `403 Forbidden`.
3. **Phân nhánh xử lý theo trạng thái Booking:**
   - **Trường hợp Booking ở trạng thái `HOLD` (Chưa thanh toán):**
     - Giải phóng số lượng giữ tạm thời trong Redis Cache (`releaseHolds`).
     - Cập nhật trạng thái booking thành `CANCELLED`.
     - Số tiền hoàn lại là `0 VNĐ` (do khách chưa thanh toán).
   - **Trường hợp Booking ở trạng thái `CONFIRMED` (Đã thanh toán):**
     - Đánh giá khoảng cách thời gian từ thời điểm hủy tới giờ bắt đầu trải nghiệm dịch vụ:
       - **Hủy SỚM (> 24 giờ trước khởi hành):** Đủ điều kiện hoàn tiền 100%. Giảm `bookedCount` của slot tồn kho để người khác có thể đặt, cập nhật booking sang `CANCELLED`, ghi nhận tỷ lệ hoàn `100%`. Kích hoạt `financialRefundPort.initiateRefund(...)` thông qua `BookingCancellationFinancialAdapter` để tạo bản ghi `Refund (PENDING)` chuyển sang `RefundProcessingService` thực thi hoàn tiền với cổng thanh toán.
       - **Hủy TRỄ (<= 24 giờ trước khởi hành):** Áp dụng quy tắc chính sách, khách bị mất 100% tiền vé (tỷ lệ hoàn `0%`). Vẫn giảm `bookedCount` để giải phóng slot tồn kho, cập nhật booking sang `CANCELLED`.
4. Trả về kết quả hủy booking thành công `200 OK` kèm tỷ lệ hoàn và số tiền hoàn tương ứng.
