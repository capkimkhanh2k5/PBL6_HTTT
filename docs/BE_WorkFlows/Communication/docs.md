# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE COMMUNICATION (TIN NHẮN TRAO ĐỔI CUSTOMER - VENDOR)

> Module Communication cung cấp hệ thống nhắn tin trực tiếp giữa Khách hàng (Customer) và Đối tác (Vendor) gắn liền với từng đơn hàng (`MasterOrder`), áp dụng cơ chế khóa bi quan (Pessimistic Locking) nhằm đảm bảo tính đơn điệu của chuỗi tin nhắn (Monotonic Sequencing), chống trùng lặp hội thoại đa luồng, hỗ trợ cơ chế REST polling tối ưu dựa trên con trỏ (`afterSequence`) và tự động cập nhật trạng thái đã đọc (Read Receipt) an toàn theo từng batch.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Communication_ConversationLifecycle` | Sequence Diagram | Khởi tạo hoặc truy xuất hội thoại: Khóa MasterOrder, kiểm tra phân quyền RBAC/IDOR, unique constraint chống trùng lặp hội thoại trong môi trường đồng thời cao. |
| 2 | `Communication_MessageSending_MonotonicSequencing` | Sequence Diagram | Gửi tin nhắn và cấp phát Sequence đơn điệu: Khóa bi quan bản ghi Conversation tới khi commit, tăng sequence tự động theo từng hội thoại, ngăn chặn race condition. |
| 3 | `Communication_CursorPolling_BatchReadReceipt` | Sequence Diagram | REST Polling qua Cursor và cập nhật Read Receipt: Polling theo `afterSequence`, trả về tin nhắn tăng dần theo sequence và chỉ đánh dấu đã đọc các tin nhắn đối phương nằm trong batch nhận được. |

---

## 1. Communication_ConversationLifecycle — Khởi Tạo & Truy Xuất Hội Thoại

**Lớp xử lý chính:** `com.danasea.backend.modules.communication.application.usecases.CreateOrGetConversationUseCase`

### Sơ đồ tuần tự (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client (Customer/Vendor)
    participant Ctrl as ConversationController
    participant UC as CreateOrGetConversationUseCase
    participant OrderPort as OrderInternalApi
    participant VendorPort as VendorInternalApi
    participant ConvRepo as JpaConversationRepository
    participant DB as PostgreSQL DB

    Client->>Ctrl: POST /api/conversations { masterOrderId, vendorId? }
    Ctrl->>UC: execute(request, currentUserId)
    
    rect rgb(240, 248, 255)
        note over UC,OrderPort: 1. Khóa MasterOrder và xác thực quyền tham gia
        UC->>OrderPort: findByIdForUpdate(masterOrderId)
        OrderPort->>DB: SELECT * FROM master_orders WHERE id = ? FOR UPDATE
        DB-->>OrderPort: masterOrder
        OrderPort-->>UC: masterOrder
        
        alt Current User là Customer
            UC->>UC: Xác minh masterOrder.customerId == currentUserId
            alt Không khớp Customer ID
                UC-->>Ctrl: 403 Forbidden (Không sở hữu đơn hàng)
            end
            UC->>UC: Xác định vendorId (từ request hoặc từ sub-order duy nhất)
        else Current User là Vendor
            UC->>VendorPort: findByUserId(currentUserId)
            VendorPort-->>UC: vendor
            UC->>UC: Xác minh sub-order của vendor thuộc masterOrder
            alt Không có sub-order liên quan
                UC-->>Ctrl: 403 Forbidden (Vendor không phụ trách đơn hàng)
            end
        end
    end

    rect rgb(245, 255, 250)
        note over UC,DB: 2. Tìm kiếm hoặc tạo mới kèm khóa chống trùng
        UC->>ConvRepo: findByCustomerIdAndVendorIdAndMasterOrderId(customerId, vendorId, masterOrderId)
        alt Hội thoại đã tồn tại
            ConvRepo-->>UC: conversation
            UC-->>Ctrl: 200 OK (ConversationResponse)
            Ctrl-->>Client: 200 OK
        else Chưa tồn tại
            UC->>ConvRepo: saveAndFlush(new Conversation)
            ConvRepo->>DB: INSERT INTO conversations (..., UNIQUE(customer_id, vendor_id, master_order_id))
            DB-->>ConvRepo: Saved Entity
            ConvRepo-->>UC: conversation
            UC-->>Ctrl: 201 Created (ConversationResponse)
            Ctrl-->>Client: 201 Created
        end
    end
```

### Quy tắc nghiệp vụ & Cơ chế bảo vệ:
1. **Khóa MasterOrder:** Khi một bên yêu cầu tạo hoặc mở chat, hệ thống khóa dòng `master_orders` liên quan trong transaction để serialize các request tạo chat đồng thời.
2. **Unique Constraint (Ràng buộc toàn vẹn):** Cột `(customer_id, vendor_id, master_order_id)` được gán unique constraint trong cơ sở dữ liệu (`uk_conversations_cust_vend_order`). Nếu 2 luồng cùng vượt qua bước kiểm tra, DB sẽ chặn đứng luồng thứ hai mà không tạo bản ghi rác.
3. **Đơn hàng đa Vendor:** Trường hợp `MasterOrder` gồm nhiều đơn con của nhiều Vendor khác nhau, request bắt buộc phải chỉ định rõ `vendorId` muốn trao đổi. Nếu không chỉ định, hệ thống trả về `400 Bad Request`.

---

## 2. Communication_MessageSending_MonotonicSequencing — Gửi Tin Nhắn & Chuỗi Thứ Tự Đơn Điệu

**Lớp xử lý chính:** `com.danasea.backend.modules.communication.application.usecases.SendMessageUseCase`

### Sơ đồ tuần tự (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor Sender as Sender (Customer/Vendor)
    participant Ctrl as ConversationController
    participant UC as SendMessageUseCase
    participant ConvRepo as JpaConversationRepository
    participant MsgRepo as JpaMessageRepository
    participant DB as PostgreSQL DB

    Sender->>Ctrl: POST /api/conversations/{id}/messages { content, attachmentUrl? }
    Ctrl->>UC: execute(conversationId, request, currentUserId)

    rect rgb(255, 245, 238)
        note over UC,DB: 1. Khóa bi quan Conversation để đồng bộ sequence
        UC->>ConvRepo: findByIdForUpdate(conversationId)
        ConvRepo->>DB: SELECT * FROM conversations WHERE id = ? FOR UPDATE
        DB-->>ConvRepo: conversation (Locked)
        ConvRepo-->>UC: conversation
        
        UC->>UC: Xác minh Sender là Customer hoặc Vendor của hội thoại (403 nếu sai)
    end

    rect rgb(240, 255, 240)
        note over UC,DB: 2. Cấp phát sequence tăng đơn điệu
        UC->>MsgRepo: findMaxSequenceByConversationId(conversationId)
        MsgRepo->>DB: SELECT COALESCE(MAX(sequence), 0) FROM messages WHERE conversation_id = ?
        DB-->>MsgRepo: currentMaxSequence
        MsgRepo-->>UC: currentMaxSequence
        
        UC->>UC: nextSequence = currentMaxSequence + 1
        
        UC->>MsgRepo: save(new Message(conversationId, senderId, content, nextSequence, isRead=false))
        MsgRepo->>DB: INSERT INTO messages (id, conversation_id, sender_id, content, sequence, is_read, ...)
        DB-->>MsgRepo: Saved Message
        MsgRepo-->>UC: savedMessage
    end

    note over UC,DB: 3. Commit Transaction & nhả khóa Conversation
    UC-->>Ctrl: 201 Created (MessageResponse kèm sequence)
    Ctrl-->>Sender: 201 Created { id, sequence, createdAt, ... }
```

### Đặc tính kỹ thuật:
- **Pessimistic Write Lock (`FOR UPDATE`):** Đảm bảo tại một thời điểm, chỉ một tin nhắn duy nhất được ghi vào một hội thoại cụ thể. Các tin nhắn gửi song song từ hai phía sẽ được xếp hàng tuần tự.
- **Monotonic Sequence (`sequence`):** Cấp phát số thứ tự tăng liên tục $1, 2, 3...$ không ngắt quãng và không trùng lặp cho từng cuộc hội thoại. Có ràng buộc `UNIQUE(conversation_id, sequence)` bảo vệ ở mức cơ sở dữ liệu.
- **Giới hạn nội dung:** Kiểm tra độ dài nội dung tin nhắn không rỗng và không vượt quá 5.000 ký tự.

---

## 3. Communication_CursorPolling_BatchReadReceipt — Polling & Đánh Dấu Đã Đọc Theo Batch

**Lớp xử lý chính:** `com.danasea.backend.modules.communication.application.usecases.GetMessagesUseCase`

### Sơ đồ tuần tự (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor Client as Polling Client (Customer/Vendor)
    participant Ctrl as ConversationController
    participant UC as GetMessagesUseCase
    participant ConvRepo as JpaConversationRepository
    participant MsgRepo as JpaMessageRepository
    participant DB as PostgreSQL DB

    Client->>Ctrl: GET /api/conversations/{id}/messages?afterSequence=X&size=50
    Ctrl->>UC: execute(conversationId, afterSequence, size, currentUserId)

    rect rgb(245, 245, 255)
        note over UC,ConvRepo: 1. Kiểm tra quyền truy cập hội thoại
        UC->>ConvRepo: findById(conversationId)
        ConvRepo-->>UC: conversation
        UC->>UC: Kiểm tra currentUserId thuộc hội thoại (403 Forbidden nếu IDOR)
    end

    rect rgb(255, 250, 240)
        note over UC,DB: 2. Truy vấn tin nhắn theo con trỏ sequence
        UC->>MsgRepo: findMessagesAfterSequence(conversationId, afterSequence, limit=size)
        MsgRepo->>DB: SELECT * FROM messages WHERE conversation_id = ? AND sequence > ? ORDER BY sequence ASC LIMIT ?
        DB-->>MsgRepo: List<Message> (batch)
        MsgRepo-->>UC: batch
    end

    rect rgb(240, 255, 240)
        note over UC,DB: 3. Cập nhật Read Receipt phạm vi an toàn (Batch Scope)
        UC->>UC: Lọc các tin nhắn chưa đọc mà senderId != currentUserId trong batch
        alt Có tin nhắn đối phương chưa đọc trong batch
            UC->>MsgRepo: markMessagesAsReadByIds(targetIds)
            MsgRepo->>DB: UPDATE messages SET is_read = true WHERE id IN (targetIds)
            DB-->>MsgRepo: Updated count
        end
    end

    UC-->>Ctrl: List<MessageResponse> (sắp xếp sequence tăng dần)
    Ctrl-->>Client: 200 OK [ { id, sequence, content, isRead: true, ... }, ... ]
```

### Hợp đồng Cursor Polling & Quy tắc Read Receipt:
1. **Khởi đầu polling:** Client truyền `afterSequence=0` ở request đầu tiên để nạp các tin nhắn ban đầu.
2. **Cập nhật Cursor:** Ở các request tiếp theo, client gửi `afterSequence = max(sequence)` của tin nhắn cuối cùng nhận được. Con trỏ chỉ được tăng sau khi client đã xử lý xong toàn bộ batch nhận về.
3. **Phạm vi Read Receipt (Batch Invariant):** 
   - Hệ thống **chỉ đánh dấu đã đọc các tin nhắn nằm trong batch được trả về**.
   - Không đánh dấu các tin nhắn chưa được nạp (nằm ngoài batch) hoặc các tin nhắn do chính người gọi gửi (`senderId == currentUserId`).
4. **Validation:** Nghiêm cấm truyền đồng thời cả 2 tham số `after` (timestamp) và `afterSequence` (trả về `400 Bad Request`). Giới hạn `size` tối đa là 100 tin nhắn/batch.

---

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
