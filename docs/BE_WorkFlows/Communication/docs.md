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
