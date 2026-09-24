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

**Thành phần:** `User (Frontend)` → `AssistantController` → `RedisRateLimiter` → `ModerationPort (LlamaGuard)` → `ChatUseCase` → `GroqLlmClient` → `PostgreSQL`

**Luồng:**
1. Khách gọi `POST /api/assistant/chat` gửi tin nhắn
2. `RedisRateLimiter` kiểm tra tần suất theo **Token Bucket + TrustTier**:
   - Vượt quota → HTTP 429
   - Cho phép → tiếp tục
3. `ModerationPort (LlamaGuard)` quét `isSafe(message)` — phát hiện Prompt Injection / Jailbreak:
   - Độc hại → HTTP 400 từ chối
   - An toàn → tiếp tục
4. `ChatUseCase`:
   - Load/Tạo Conversation History từ PostgreSQL
   - Gọi `GroqLlmClient.generateResponse(History + Tools + Message)`
   - LLM trả văn bản hoặc **Lệnh gọi Tool**
5. Lưu `AiMessage` & `AuditLog` → trả HTTP 200 cho User

---

## 2. Chat_ModerationFlow.png — Chi tiết Moderation + Tool Calling

**Thành phần:** `Khách Hàng` → `AssistantController` → `RedisRateLimiter (2-Layer)` → `ChatUseCase` → `GroqModerationClient (Prompt Guard)` → `GroqLlmClient (Rotator 5 Keys)` → `GetPolicyTool / ServiceTools` → `Postgres & Redis`

**Luồng chi tiết:**
1. Kiểm tra hạn mức `IP + TrustTier` (2 tầng):
   - Bị từ chối / RESTRICTED → 429
   - Cho phép → tiếp tục
2. **Cắt chuỗi max 1800 ký tự** — chống Padding Attack
3. `isSafe(truncatedContent)`:
   - **Unsafe:** Ghi Audit Log vi phạm → quăng ngoại lệ → *"Vi phạm chính sách an toàn"*
   - **Benign:** Lưu User message → tiếp tục
4. **Vòng lặp Tool Calling (tối đa 5 lượt):**
   - LLM trả `ToolCall` (vd: `get_policy`) → Backend thực thi Tool → truy vấn DB → trả JSON
   - **Policy Guard:** Nếu LLM trả lời về chính sách mà chưa gọi `get_policy` → **THU HỒI & THAY THẾ** bằng thông báo chuẩn
   - LLM trả văn bản → lưu Assistant message & Audit Log
5. Trả 200 OK + nội dung trả lời

---

## 3. RevalidBooking.png — Xác nhận đặt chỗ (Sequence)

**Thành phần:** `User` → `ConfirmBookingUseCase` → `Redis (Draft Cards)` → `Service/Slot DB`

**Luồng:**
1. Khách bấm xác nhận → `POST /confirm (cardId)`
2. Lấy **Draft Card** từ Redis
3. Truy vấn **giá & chỗ trống hiện tại** từ DB → so sánh với Card:

| Kết quả so sánh | Xử lý |
|---|---|
| **Giá thay đổi / Hết chỗ** | Hủy Card cũ → Tạo Alternative Card (đề xuất thay thế) → trả `ALTERNATIVE_SUGGESTED` |
| **Dữ liệu khớp** | Tạo Booking chờ thanh toán → Xóa Draft Card (chống Replay Attack) → trả `SUCCESS` |

---

## 4. RevalidBookingFlow.png — Logic phân nhánh Re-validation (Flowchart)

**Cây quyết định:**
1. **Tìm ConfirmationCard** theo cardId
2. **Trạng thái = PENDING?**
   - Không → *"Thẻ không hợp lệ / Hết hạn"*
   - Có → tiếp
3. **Còn slot trống?**
   - Hết → `CANCELLED` → *"out_of_stock, gợi ý ngày khác"*
   - Còn → tiếp
4. **Giá/Slot có thay đổi?**
   - Không → `CONFIRMED & Giữ chỗ` → HTTP 200 thành công
   - Có → kiểm tra retry
5. **Retry count ≥ 2?**
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
