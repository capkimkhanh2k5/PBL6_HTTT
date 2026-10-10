# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE COMMUNICATION (HỆ THỐNG THÔNG BÁO)

> Phân hệ Communication chịu trách nhiệm quản lý trung tâm thông báo người dùng (In-App Notification Center), cơ chế theo dõi trạng thái đã đọc (Read Tracking & Unread Count), phản ứng thời gian thực với các sự kiện thanh toán / hoàn tiền qua kiến trúc Event-Driven, và tác vụ định kỳ quét nhắc chuyến đi sắp khởi hành (Trip Reminder Scheduled Job) tích hợp cơ chế Push Notification.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `InApp_Notification_Lifecycle_And_ReadTracking_Flow` | Sequence Diagram | Quản lý thông báo người dùng (`/api/notifications`): Phân trang, đánh dấu đã đọc từng thông báo (chống IDOR), đánh dấu đọc tất cả (Idempotent Bulk Update) và đếm số lượng chưa đọc. |
| 2 | `Payment_And_Refund_Event_Notification_Flow` | Sequence Diagram | Kiến trúc Hướng Sự Kiện (Event-Driven): Lắng nghe `PaymentSuccessEvent` và `RefundCompletedEvent` để phát sinh thông báo tức thời cho Khách hàng và Đối tác. |
| 3 | `Scheduled_Trip_Reminder_Job_Flow` | Sequence Diagram | Tác vụ định kỳ nhắc chuyến (`TripReminderJob`): Quét các slot khởi hành trong vòng 24 giờ, chống gửi lặp qua khóa Idempotency, đồng bộ Mobile Push sau commit CSDL. |

---

## 1. InApp_Notification_Lifecycle_And_ReadTracking_Flow.mmd — Vòng Đời Thông Báo & Trạng Thái Đã Đọc

**Điểm truy cập API:**
- `GET /api/notifications`: Lấy danh sách thông báo của người dùng hiện tại (hỗ trợ phân trang `page`, `size`).
- `PATCH /api/notifications/{id}/read`: Đánh dấu một thông báo cụ thể là đã đọc.
- `PATCH /api/notifications/read-all`: Đánh dấu toàn bộ thông báo chưa đọc của người dùng là đã đọc.
- `GET /api/notifications/unread-count`: Đếm tổng số thông báo chưa đọc phục vụ hiển thị huy hiệu (Badge Counter).

**Quy tắc bảo mật & Toàn vẹn dữ liệu (R4):**
- **Xác thực danh tính:** Toàn bộ API yêu cầu người dùng đã đăng nhập (`@PreAuthorize("isAuthenticated()")`).
- **Phòng chống IDOR (Insecure Direct Object Reference):** Khi gọi `PATCH /api/notifications/{id}/read`, `MarkNotificationReadUseCase` kiểm tra nghiêm ngặt `notification.userId == currentUserId`. Nếu người dùng cố tình thao tác trên thông báo của người khác, hệ thống chặn đứng với mã lỗi `403 Forbidden` (`UnauthorizedNotificationAccessException`).
- **Phân tách trạng thái gửi và đọc:** Trạng thái `SENT` chỉ biểu thị thông báo đã được gửi thành công vào hộp thư. Trường `isRead` (`boolean`) và `readAt` (`OffsetDateTime`) thể hiện chính xác thời điểm người dùng thực sự đọc thông báo.
- **Tính Idempotent:** Gọi `PATCH /api/notifications/read-all` khi `unreadCount = 0` trả về `updatedCount = 0` một cách an toàn và không gây lỗi logic.

---

## 2. Payment_And_Refund_Event_Notification_Flow.mmd — Sự Kiện Thanh Toán & Hoàn Tiền Tức Thời

**Kiến trúc xử lý:** Phân tách hoàn toàn (Decoupled) thông qua Spring `ApplicationEventPublisher`. Các service lõi thanh toán không phụ thuộc trực tiếp vào module thông báo.

**Các luồng sự kiện (R5):**
1. **Sự kiện thanh toán thành công (`PaymentSuccessEvent`):**
   - Được bắn từ `OrderPaymentService` ngay sau khi cổng thanh toán xác nhận giao dịch thành công và `ConfirmBookingUseCase` đã hoàn tất.
   - `PaymentNotificationEventListener` tiếp nhận sự kiện:
     - Tạo thông báo In-App cho **Khách hàng**: Tiêu đề "Thanh toán thành công", nội dung chi tiết đơn hàng, liên kết tới `ORDER`.
     - Duyệt danh sách đơn con (`SubOrders`), trích xuất các `vendorId` duy nhất và gửi thông báo In-App cho từng **Đối tác**: "Có đơn đặt dịch vụ mới".
2. **Sự kiện hoàn tiền thành công (`RefundCompletedEvent`):**
   - Được bắn từ `RefundProcessingService` khi giao dịch bồi hoàn đạt trạng thái `PROCESSED`.
   - `RefundNotificationEventListener` tiếp nhận sự kiện và gửi thông báo In-App cho **Khách hàng** kèm số tiền hoàn đã được định dạng bản địa hóa.

---

## 3. Scheduled_Trip_Reminder_Job_Flow.mmd — Tác Vụ Quét Nhắc Chuyến Khởi Hành

**Thành phần thực thi:** `com.danasea.backend.modules.communication.infrastructure.jobs.TripReminderJob`

**Nguyên lý vận hành (R5):**
1. **Lịch biểu kích hoạt:** Chạy định kỳ mỗi giờ (`@Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")`).
2. **Cửa sổ thời gian khởi hành (Window Scan):**
   - `now = LocalDateTime.now(Asia/Ho_Chi_Minh)`
   - `horizon = now.plusHours(24)`
   - Quét toàn bộ các `service_slots` có thời gian xuất phát nằm trong đoạn `[now, horizon]`.
3. **Bộ lọc đơn hợp lệ:** Chỉ gửi nhắc nhở cho các `SubOrder` có trạng thái `CONFIRMED` và `MasterOrder` có trạng thái thanh toán `PAID`.
4. **Cơ chế chống gửi trùng lặp (Idempotency):**
   - Khóa duy nhất dạng: `trip:{slotId}:customer:{customerId}`.
   - CSDL sử dụng `UNIQUE CONSTRAINT (idempotency_key)` (từ migration `V27__notification_read_tracking_and_idempotency.sql`).
   - Nếu tác vụ chạy lại trong cùng một ngày, lệnh chèn bị bỏ qua (`DO NOTHING`) mà không tạo thêm thông báo dư thừa.
5. **Đồng bộ Mobile Push:**
   - Sử dụng `TransactionSynchronizationManager.registerSynchronization` với hook `afterCommit()`.
   - Đảm bảo chỉ khi giao dịch CSDL đã commit thành công 100%, lệnh gọi `PushNotificationPort.sendPush(...)` mới được kích hoạt, tránh gửi push giả khi có rollback CSDL.
