# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE AI CHAT ASSISTANT

> Folder này chứa **5 sơ đồ** mô tả toàn bộ luồng xử lý AI Chat, cơ chế bảo mật Moderation, quy trình xác nhận đặt chỗ (Re-validation) và chiến lược xoay API Key + Fallback Model.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `AI_Chat` | Sequence Diagram | Luồng tổng quan Chat AI (Rate Limit → LlamaGuard Moderation → LLM → Tool Calling → DB) |
| 2 | `Chat_ModerationFlow` | Sequence Diagram | Chi tiết: cắt chuỗi 1800 ký tự chống Padding, Tool Calling loop 5 lượt, Policy Guard thu hồi câu trả lời sai |
| 3 | `RevalidBooking` | Sequence Diagram | Xác nhận đặt chỗ: lấy Draft Card từ Redis → so sánh giá/slot thực tế → Alternative Card hoặc Success |
| 4 | `RevalidBookingFlow` | Flowchart | Logic phân nhánh chi tiết: PENDING check → slot check → price change → retry count ≥ 2 |
| 5 | `RollKeys_ModelFallBack` | Flowchart | Xoay 5 API Key Groq (ACTIVE/COOLDOWN/DISABLED) + Fallback Model khi lỗi 429/401/5xx |

---

## 1. AI_Chat.png — Luồng tổng quan AI Chat

**Thành phần:** `User (Frontend)` → `LocaleContextFilter` → `AssistantController` → `RedisRateLimiter` → `ModerationPort (LlamaGuard)` → `SystemPromptBuilder` → `ChatUseCase` → `GroqLlmClient` → `PostgreSQL`

**Luồng:**
1. Khách gửi tin nhắn qua `POST /api/assistant/chat` kèm header `Accept-Language: vi`.
2. `LocaleContextFilter` trích xuất và thiết lập ngữ cảnh ngôn ngữ (`SupportedLanguage.VI`).
3. `RedisRateLimiter` kiểm tra tần suất theo **Token Bucket + TrustTier**:
   - Vượt quota → HTTP 429 Too Many Requests.
   - Cho phép → tiếp tục.
4. `ModerationPort (LlamaGuard)` quét `isSafe(truncatedContent)` — phát hiện Prompt Injection / Jailbreak:
   - Độc hại → Ném `LocalizedException("ai.moderation.blocked")` trả về HTTP 400 với thông báo chuẩn hóa theo ngôn ngữ người dùng.
   - An toàn → tiếp tục.
5. `SystemPromptBuilder.buildBasePrompt(language)` nạp System Prompt tương ứng với ngôn ngữ đã phân giải.
6. `ChatUseCase`:
   - Load/Tạo Conversation History từ PostgreSQL theo ngôn ngữ.
   - Gọi `GroqLlmClient.generateResponse(history, language)`.
   - LLM trả văn bản hoặc **Lệnh gọi Tool**.
7. Lưu `AiMessage` & `AuditLog` → trả HTTP 200 cho User.

---

## 2. Chat_ModerationFlow.png — Chi tiết Moderation + Tool Calling

**Thành phần:** `Khách Hàng` → `AssistantController` → `RedisRateLimiter (2-Layer)` → `ChatUseCase` → `GroqModerationClient (Prompt Guard)` → `GroqLlmClient (Rotator 5 Keys)` → `ToolExecutor [ConversationAware]` → `Postgres & Redis`

**Luồng chi tiết:**
1. Kiểm tra hạn mức `IP + TrustTier` (2 tầng):
   - Bị từ chối / RESTRICTED → HTTP 429 Rate Limited.
   - Cho phép → tiếp tục.
2. **Cắt chuỗi max 1800 ký tự** — chống Padding Attack làm cạn kiệt tài nguyên xử lý ngữ cảnh.
3. `isSafe(truncatedContent)`:
   - **Unsafe:** Ghi Audit Log vi phạm → Ném `LocalizedException` trả về thông báo lỗi đa ngôn ngữ.
   - **Benign:** Lưu User message vào DB → tiếp tục.
4. **Vòng lặp Tool Calling (tối đa 5 lượt):**
   - LLM trả `ToolCall` → Backend thực thi Tool thông qua `ToolExecutor.execute(args, ToolExecutionContext{language, convId})` để đảm bảo kết quả truy vấn hoặc thông báo được chuẩn hóa đúng ngôn ngữ người dùng.
   - **Policy Guard:** Nếu LLM trả lời về chính sách mà chưa gọi `get_policy` → **THU HỒI & THAY THẾ** bằng thông báo chuẩn từ hệ thống.
   - LLM trả văn bản → lưu Assistant message & Audit Log.
5. Trả HTTP 200 OK + nội dung phản hồi hoàn chỉnh.

---

## 3. RevalidBooking.png — Xác nhận đặt chỗ (Sequence & Ràng buộc bảo mật)

**Thành phần:** `User` → `AssistantController` → `ChatHistoryService` → `ConfirmBookingUseCase` → `ConfirmationCardStorePort (Redis)` → `GetPublicServiceDetailUseCase (PostgreSQL)` → `CreateBookingHoldUseCase`

**Luồng:**
1. Khách bấm xác nhận → `POST /api/assistant/conversations/{id}/confirm` gửi kèm `{"cardId": "..."}`.
2. **Kiểm tra quyền sở hữu hội thoại (Ownership Guard):**
   - Controller gọi `ChatHistoryService.getConversationForUser(id, currentUserId)`.
   - Nếu hội thoại không tồn tại → HTTP 404 Not Found.
   - Nếu hội thoại thuộc người dùng khác → ném `AccessDeniedException` trả về **HTTP 403 Forbidden** (`ACCESS_DENIED`).
3. **Ràng buộc 3 bên (Card — Conversation — User):**
   - Controller gọi `confirmBookingUseCase.execute(cardId, userId, sessionId, expectedConversationId = id)`.
   - Lấy **Draft Card** từ Redis qua `cardStorePort.findById(cardId)`.
   - Kiểm tra: `card.getConversationId().equals(expectedConversationId)`. Nếu không khớp → ném `AccessDeniedException` (**HTTP 403 Forbidden**), chặn đứng tấn công dùng Card của hội thoại khác (Cross-conversation card hijacking).
4. **Truy vấn giá & chỗ trống thực tế** từ DB qua `GetPublicServiceDetailUseCase` → so sánh với Card:

| Kết quả so sánh | Xử lý |
|---|---|
| **Giá thay đổi / Hết chỗ** | Hủy Card cũ → Tạo Alternative Card (đề xuất thay thế) → trả `alternative_needed` |
| **Dữ liệu khớp** | Kích hoạt `CreateBookingHoldUseCase` tạo giữ chỗ phân tán (Redis Lua lock + DB Booking `HOLD` TTL 15p) → Đánh dấu Card `CONFIRMED` → Ghi Audit Log → trả `success` kèm `bookingId`, `holdExpiresAt` |
| **Lỗi bù trừ (Compensation)** | Nếu lưu Card thất bại sau khi tạo Hold, tự động gọi `CancelBookingHoldUseCase` giải phóng tồn kho ngay lập tức |

---

## 4. RevalidBookingFlow.png — Logic phân nhánh Re-validation (Flowchart)

**Cây quyết định chi tiết:**
1. **Xác thực quyền sở hữu Conversation:** `conversation.userId == currentUserId`?
   - Sai → **403 Forbidden**
   - Không tìm thấy → **404 Not Found**
   - Đúng → tiếp
2. **Tìm ConfirmationCard** theo cardId:
   - Không tìm thấy / hết hạn → ném `IllegalArgumentException`
   - Tìm thấy → tiếp
3. **Card thuộc về Conversation hiện tại?** (`card.conversationId == expectedConversationId`):
   - Sai → **403 Forbidden** (`AccessDeniedException`)
   - Đúng → tiếp
4. **Trạng thái Card = PENDING?**
   - Không → *"Thẻ không ở trạng thái PENDING"*
   - Có → tiếp
5. **Còn slot trống?**
   - Hết hẳn → `CANCELLED` → *"out_of_stock, gợi ý ngày khác"*
   - Còn → tiếp
6. **Giá/Slot có thay đổi?**
   - Không → Gọi `CreateBookingHoldUseCase` giữ chỗ → `CONFIRMED` → HTTP 200 thành công (`bookingId`, `holdExpiresAt`)
   - Có → kiểm tra retry
7. **Retry count ≥ 2?**
   - ≥ 2 → `CANCELLED` → *"Thay đổi quá nhiều lần, vui lòng đặt lại"*
   - < 2 → Tăng `retryCount` (Redis TTL:15p) → Tạo Card mới `PRICE_CHANGED / SLOT_UNAVAILABLE` → Hủy Card cũ → trả `alternative_needed`

---

## 5. RollKeys_ModelFallBack.png — Xoay API Key & Fallback Model

**Xử lý theo mã HTTP từ Groq API:**

| Mã HTTP | Ý nghĩa | Hành động |
|:---:|---|---|
| **200** | Thành công | Trả kết quả cho User |
| **429** | Quá tải | Key → **`COOLDOWN`** (1 phút) → KeyRotator lấy Key ACTIVE khác từ Redis → gửi lại |
| **401** | Sai/Hủy Key | Key → **`DISABLED`** vĩnh viễn → KeyRotator lấy Key khác → gửi lại |
| **5xx** | Lỗi Server Groq | Đổi sang **`GROQ_FALLBACK_MODEL`** → gửi lại |

**KeyRotator:** Quản lý 5 API Key Groq trên Redis, mỗi Key có 3 trạng thái: `ACTIVE` / `COOLDOWN` / `DISABLED`. Khi hết Key ACTIVE → chuyển fallback model đảm bảo dịch vụ **không bao giờ gián đoạn**.

---

## 🔗 Mối liên hệ giữa các sơ đồ

- **Sơ đồ 1** → Bức tranh tổng thể luồng Chat AI
- **Sơ đồ 2** → Đi sâu chi tiết: Moderation 2 tầng, Tool Calling loop 5 lượt, Policy Guard
- **Sơ đồ 3 & 4** → Cùng nghiệp vụ Re-validation, 2 góc nhìn (Sequence vs Flowchart)
- **Sơ đồ 5** → Đảm bảo `GroqLlmClient` luôn ổn định (phục vụ sơ đồ 1 & 2)
