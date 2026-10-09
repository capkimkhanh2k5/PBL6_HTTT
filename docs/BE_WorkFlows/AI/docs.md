# 📖 TÓM TẮT CÁC SƠ ĐỒ — HỆ THỐNG DANASEA AI (HYBRID ARCHITECTURE & WORKFLOWS)

> Thư mục này chứa **10 sơ đồ kỹ thuật** mô tả toàn diện kiến trúc AI Lai (Groq LLM + Quyet Small Worker), luồng Chat Assistant, cơ chế bảo mật Moderation, quy trình đặt chỗ Re-validation, và toàn bộ 10 tính năng Customer AI mới cùng hệ thống thẩm định rủi ro giao dịch/kiểm duyệt của Quản trị viên.

| # | Sơ đồ | Loại | Nội dung cốt lõi |
|---|-------|------|-------------------|
| 1 | `AI_Chat` | Sequence Diagram | Luồng tổng quan Chat AI (Rate Limit → LlamaGuard Moderation → LLM → Tool Calling → DB) |
| 2 | `Chat_ModerationFlow` | Sequence Diagram | Chi tiết bảo vệ: cắt chuỗi 1800 ký tự chống Padding, Tool Calling loop 5 lượt, Policy Guard thu hồi câu trả lời sai |
| 3 | `RevalidBooking` | Sequence Diagram | Xác nhận đặt chỗ: lấy Draft Card từ Redis → so sánh giá/slot thực tế → Alternative Card hoặc Success |
| 4 | `RevalidBookingFlow` | Flowchart | Logic phân nhánh chi tiết: PENDING check → slot check → price change → retry count ≥ 2 |
| 5 | `RollKeys_ModelFallBack` | Flowchart | Xoay 5 API Key Groq (ACTIVE/COOLDOWN/DISABLED) + Fallback Model khi lỗi 429/401/5xx |
| 6 | `AI_Hybrid_Architecture` | Architecture Diagram | Kiến trúc phân tầng 3 lớp Hybrid AI: Groq Cloud LLM + Quyet Offline Worker + Spring Boot Backend + PostgreSQL V21 + Redis |
| 7 | `Customer_AI_Discovery_Flow` | Sequence Diagram | Luồng Smart Search, Recommendation & Nearby Discovery: Query Parser, Pre-filter catalog, Quyet Intent/Relevance, Weather Check |
| 8 | `Itinerary_Planning_And_Replan_Flow` | Sequence Diagram | Lập lịch trình du lịch thông minh: Bounded Greedy, đệm di chuyển 15m + 25km/h, Re-plan với Optimistic Locking (`@Version`) |
| 9 | `Content_Assessment_And_Transaction_Risk_Flow` | Sequence Diagram | Kiểm duyệt văn bản (Regex + Quyet Moderation), Đánh giá rủi ro giao dịch (Velocity 3/5/3), Hàng đợi xét duyệt Admin |
| 10 | `AI_Travel_Assistant_MultiTool_Flow` | Sequence Diagram | Trợ lý du lịch đa công cụ (15 tools), tạo phiếu đặt chỗ Native Option Card trên Redis & Re-validation 3 chiều |

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

## 6. AI_Hybrid_Architecture.png — Kiến trúc phân tầng 3 lớp Hybrid AI

**Thành phần:** `Client Apps (React/Flutter)` → `Security Gateway (JWT, Locale, RateLimiter)` → `Controllers (Assistant, CustomerAi, Assessment, AdminAi)` → `Core Use Cases (Clean Architecture)` → `Hybrid AI Engine (Groq Cloud LLM + Quyet Small Worker)` → `Storage (PostgreSQL V21 + Redis)`

**Ý nghĩa thiết kế:**
1. **Phân định rõ trách nhiệm 2 mô hình AI (Dual-Brain Architecture):**
   - **Groq Cloud LLM (Llama 3.3/3.1):** Đảm nhiệm sinh văn bản hội thoại, tương tác ngôn ngữ tự nhiên và trích dẫn bằng chứng từ review có ID nguồn thực tế.
   - **Quyet Small Offline Worker (Fast Inference trên Local CPU/Container):** Chịu trách nhiệm thực thi các tác vụ quyết định có cấu trúc theo chuẩn phân loại cố định (rubrics): Phân loại ý định (`INTENT`), Chấm điểm tương quan gợi ý (`RELEVANCE`), Phân loại danh mục dịch vụ (`SERVICE_CATEGORY`), Phát hiện nội dung vi phạm (`MODERATION`), Đánh giá rủi ro (`RISK`), Tách khía cạnh review (`REVIEW`).
2. **Quy tắc Bất biến Tài chính & Tồn kho:** AI (kể cả Groq và Quyet) **không bao giờ có quyền ghi đè DB** hay tự ý trừ tiền, tạo booking, hủy đơn hàng hoặc phê duyệt thanh toán. Mọi quyết định cốt lõi do Spring Boot Business Logic kiểm soát.
3. **PostgreSQL Migration V21:** Bổ sung 2 bảng chuyên dụng:
   - `ai_itineraries`: Lưu kế hoạch lịch trình với cơ chế khóa lạc quan `@Version`, JSON snapshot, lập chỉ mục theo `owner_id`.
   - `ai_assessment_cases`: Lưu trữ toàn bộ bằng chứng kiểm duyệt văn bản, hồ sơ rủi ro giao dịch, mã nguyên nhân (`reasonCodes`), mô hình AI đã xử lý và trạng thái thẩm định của Admin.
4. **Redis Cache:** Lưu trữ trạng thái Key Rotator, Token Bucket theo cấp độ tin cậy (`TrustTier`), phiếu đặt chỗ nháp (`ConfirmationCard`) TTL 15 phút, và khóa phân tán (`Distributed Lock`) xử lý đặt vé chống Race Condition.

---

## 7. Customer_AI_Discovery_Flow.png — Khám phá Dịch vụ Thông minh (Search, Recommend, Nearby)

**Thành phần:** `Client` → `CustomerAiController` → `TravelQueryParser` → `DiscoverServicesUseCase` → `AiCatalogReadApi` → `Quyet Small Worker` → `TravelWeatherPort`

**Quy trình xử lý:**
1. **Phân giải yêu cầu:** Client gửi `TravelRequest` vào `POST /api/ai/search`, `/recommendations` hoặc `/nearby`. Bộ lọc `TravelContext.validateCurrentDates()` xác thực: partySize (1–50), ngân sách tổng (0–1 tỷ VND), khoảng thời gian không quá 14 ngày và không thuộc quá khứ.
2. **Xử lý ngôn ngữ tự nhiên (NLP Query Parsing):** `TravelQueryParser` bóc tách từ khóa hoạt động, ngân sách, số người, và danh sách các hoạt động người dùng muốn loại trừ (ví dụ: "không thích lặn", "trừ cano").
3. **Truy vấn Danh mục Thực tế (Hard Filtering):** Hệ thống lấy mẫu tối đa 50 dịch vụ `PUBLISHED` từ DB.
   - Loại bỏ các dịch vụ thuộc danh mục nằm trong danh sách loại trừ.
   - Kiểm tra ngân sách cho cả nhóm: tính toán chuẩn xác theo `PER_PERSON` (`unitPrice * partySize`) hoặc `PER_PACKAGE` (`unitPrice * ceil(partySize / maxPax)`).
   - Lọc các slot thời gian khớp với khung giờ hoạt động trong ngày (`dayStart` đến `dayEnd`).
4. **Kiểm tra An toàn Thời tiết:** Nếu `weatherSafeOnly = true`, kiểm tra tối đa 20 slot nhạy cảm với thời tiết (có cache theo `slot.id`). Slot không đạt điều kiện an toàn sẽ bị loại khỏi danh sách có thể đặt.
5. **Định vị & Khoảng cách (Haversine Distance):** Với chế độ `NEARBY` hoặc khi có bán kính `radiusKm`, tính khoảng cách địa lý đường chim bay. Nếu vượt bán kính $\rightarrow$ bỏ qua.
6. **Xếp hạng Thông minh (Quyet Relevance Scoring):**
   - Với `SEARCH`: Quyet dự đoán `INTENT` của người dùng để trả về kèm kết quả.
   - Với `RECOMMENDATION`: Lấy tối đa 10 ứng viên hàng đầu, gọi Quyet tính điểm tương đồng `RELEVANCE` giữa sở thích người dùng và mô tả dịch vụ $\rightarrow$ cộng dồn vào Baseline Score.
7. **Sắp xếp Kết quả:**
   - Chế độ `NEARBY`: Sắp xếp theo khoảng cách tăng dần $\rightarrow$ giá tổng tăng dần.
   - Chế độ `SEARCH / RECOMMENDATION`: Sắp xếp theo tổng điểm Score giảm dần $\rightarrow$ Service ID tăng dần.

---

## 8. Itinerary_Planning_And_Replan_Flow.png — Lập Lịch Trình & Tái Lập Lịch (Re-plan)

**Thành phần:** `Client` → `CustomerAiController` → `PlanItineraryUseCase` → `DiscoverServicesUseCase` → `TravelWeatherPort` → `ItineraryStorePort (PostgreSQL V21)`

**Hai chu trình cốt lõi:**
1. **Khởi tạo Kế hoạch (`POST /api/ai/itineraries`):**
   - Gọi `DiscoverServicesUseCase` chế độ `RECOMMENDATION` để lấy danh sách ứng viên có chỗ trống và mức giá thực tế.
   - Lấy tối đa 10 candidate, mở rộng tối đa 5 slot/option, sắp xếp theo thời gian bắt đầu (`slot.start`) và giá.
   - **Thuật toán Tham lam Có giới hạn (Bounded Chronological Greedy):**
     - Đảm bảo mỗi dịch vụ chỉ xuất hiện tối đa 1 lần trong cả chuyến đi.
     - Kiểm soát tổng ngân sách cả nhóm: `total + nextOption.partyTotal <= effectiveBudget`.
     - **Ước lượng chuyển tiếp (Estimated Transfers):** Nếu có hoạt động trước đó, tính khoảng cách Haversine giữa 2 tọa độ GPS. Thời gian di chuyển yêu cầu:
       $$\text{transitMinutes} = 15 \text{ (phút đệm)} + \left\lceil \frac{\text{distanceKm}}{25} \times 60 \right\rceil \text{ phút}$$
       Nếu giờ bắt đầu của ca sau sớm hơn $(\text{kết thúc ca trước} + \text{transitMinutes}) \rightarrow$ Bỏ qua slot này để tránh bể lịch trình.
     - Đánh giá thời tiết qua `TravelWeatherPort`: nếu thời tiết không thuận lợi $\rightarrow$ bỏ qua slot và ghi nhận mã cảnh báo.
   - Lưu kế hoạch vào bảng `ai_itineraries` với `version = 0`, gán chủ sở hữu `owner_id`.
2. **Tái lập Lịch trình khi có biến động (`POST /api/ai/itineraries/{id}/replan`):**
   - Khách yêu cầu đổi lịch (do hủy slot, thời tiết đổi, hoặc đổi sở thích), gửi `expectedVersion`, danh sách `excludedServiceIds`, `excludedSlotIds`, và `trigger`.
   - **Kiểm tra quyền sở hữu:** Lịch trình phải thuộc về người dùng đang đăng nhập (chặn 404/403).
   - **Kiểm soát đồng thời (Optimistic Locking Guard):** Đối chiếu `expectedVersion == old.version()`. Nếu phiên bản trong DB đã thay đổi $\rightarrow$ ném `AiStateConflictException` trả về **HTTP 409 Conflict**.
   - Tính toán kế hoạch mới loại bỏ các slot/dịch vụ bị hủy, lưu đè và tăng `@Version` lên $+1$.
   - Trả về chi tiết chênh lệch: `removedSlotIds`, `addedSlotIds`, `changedItems` với cờ `bookingChanged = false` (không tự ý sửa booking đã chốt).

---

## 9. Content_Assessment_And_Transaction_Risk_Flow.png — Kiểm Duyệt Văn Bản & Rủi Ro Giao Dịch

**Thành phần:** `User/Vendor/Admin` → `AiAssessmentController` / `AdminAiController` → `AssessTextUseCase` / `AnalyzeTransactionRiskUseCase` → `TransactionRiskReadPort` → `Quyet Small Worker` → `AssessmentCaseStorePort (PostgreSQL V21)`

**Ba giai đoạn thẩm định:**
1. **Kiểm duyệt nội dung văn bản (`POST /api/ai/content-assessments`):**
   - Quét Regex phát hiện thông tin liên lạc ngoài luồng: số điện thoại (+84/0...), link web http/https, địa chỉ email.
   - Gọi song song Quyet Worker thực hiện 2 task:
     - `SERVICE_CATEGORY`: Gợi ý danh mục trải nghiệm (SUP, KAYAK, DIVING...).
     - `MODERATION`: Đánh giá xác suất rủi ro (`external_payment`, `spam`, `abuse`).
   - Nếu phát hiện số điện thoại ngoài hoặc xác suất vi phạm $\ge 0.5 \rightarrow$ Đưa vào trạng thái `NEEDS_REVIEW`; ngược lại `NO_RULE_SIGNAL`.
   - Lưu hồ sơ vào bảng `ai_assessment_cases` với đầy đủ bằng chứng bằng văn bản (`evidenceType = TEXT_ONLY`).
2. **Phân tích rủi ro giao dịch của Admin (`POST /api/admin/ai/risk-cases`):**
   - Quản trị viên nhập `orderId` và khiếu nại của khách.
   - Backend truy vấn dữ liệu vận tốc giao dịch thực tế qua `TransactionRiskReadPort`:
     - Số lần thanh toán thất bại trong 24 giờ (`failedPayments24h >= 3`).
     - Tần suất tạo đơn trong 1 giờ gần nhất (`orders1h >= 5`).
     - Số yêu cầu hoàn tiền trong 7 ngày qua (`refundRequests7d >= 3`).
   - Gọi Quyet Worker task `RISK` kết hợp các tín hiệu hành vi và khiếu nại.
   - Nếu vi phạm bất kỳ ngưỡng quy tắc nào hoặc Quyet đánh giá xác suất $\ge 0.5 \rightarrow$ Tạo case trạng thái `NEEDS_REVIEW`.
3. **Hàng đợi xét duyệt & Giải quyết case của Admin:**
   - Admin xem danh sách hồ sơ cần thẩm định qua `GET /api/admin/ai/assessment-cases?status=NEEDS_REVIEW&limit=50`.
   - Admin xử lý hồ sơ qua `POST /api/admin/ai/assessment-cases/{id}/resolve` với `resolution` (`RESOLVED`, `DISMISSED`) và ghi chú giải trình (1–1000 ký tự).
   - Kiểm soát bằng khóa lạc quan: Chặn đứng tình huống 2 Admin duyệt trùng một case.

---

## 10. AI_Travel_Assistant_MultiTool_Flow.png — Trợ Lý Du Lịch Đa Công Cụ (15 Tools & Native Option Booking)

**Thành phần:** `Client` → `AssistantController` → `ChatUseCase` → `GroqLlmClient` → `AIToolRegistry (15 Tools)` → `CustomerFeatureTool` → `ConfirmBookingUseCase` → `CreateBookingHoldUseCase`

**Luồng thực thi:**
1. **Hội thoại & Vòng lặp gọi Tool (Tối đa 5 lượt):**
   - Groq LLM nhận danh sách **15 công cụ** đã đăng ký trong `AIToolRegistry`:
     - 6 công cụ truyền thống: `search_services`, `get_service_detail`, `get_policy`, `get_weather_forecast`, `get_safety_alert`, `request_booking_confirmation`.
     - 9 công cụ khách hàng mới: `ai_smart_search`, `ai_recommend_services`, `ai_nearby_services`, `ai_compare_services`, `ai_plan_itinerary`, `ai_replan_itinerary`, `ai_review_summary`, `ai_weather_slot`, `ai_customer_support`.
   - Khi LLM yêu cầu gọi Tool, `CustomerFeatureTool` thực thi logic nghiệp vụ thật từ Backend, đảm bảo kiểm tra quyền truy cập (yêu cầu đăng nhập với các tool riêng tư như lịch trình hay hỗ trợ đơn hàng).
2. **Khởi tạo Phiếu Đặt Chỗ Native Option (Draft Card trên Redis):**
   - Khách chốt lịch $\rightarrow$ LLM gọi `request_booking_confirmation(service_id, option_id, slot_id, date, participants)`.
   - `RequestBookingConfirmationTool` xác thực Option và Slot thực tế còn tồn tại và còn chỗ.
   - Sinh thẻ `ConfirmationCard` dạng Native Option chứa `optionId`, `slotId`, `unitPrice`, `participantsCount`, lưu vào Redis với TTL 15 phút.
   - Trả giao diện Thẻ Đặt Chỗ kèm nút bấm **[Xác Nhận Đặt Chỗ]** cho khách hàng.
3. **Xác nhận Đặt chỗ & Khóa Giữ Chỗ Phân Tán (Re-validation Guard):**
   - Khách bấm xác nhận $\rightarrow$ Frontend gọi `POST /api/assistant/conversations/{id}/confirm` kèm `cardId`.
   - **Khóa phân tán:** Xác lập `tryAcquireProcessingLock` trên Redis (TTL 30 giây) chống xử lý lặp.
   - **Xác thực quyền sở hữu kép:** Hội thoại phải thuộc về User hiện tại, và Card phải thuộc đúng Hội thoại đó (`card.conversationId == expectedConversationId`). Chặn đứng tấn công giả mạo Card (403 Forbidden).
   - **Tái thẩm định thời gian thực (Native Re-validation):** Đọc lại giá và slot từ CSDL. Nếu có biến động hoặc hết chỗ $\rightarrow$ quản lý số lần retry (tối đa 2 lần) và tạo thẻ đề xuất thay thế (`alternative_needed`).
   - Nếu dữ liệu hoàn toàn hợp lệ $\rightarrow$ kích hoạt `CreateBookingHoldUseCase`, thực hiện khóa tồn kho nguyên tử qua Redis Lua script và lưu bản ghi Booking trạng thái `HOLD` (15 phút) trong PostgreSQL.
   - Đánh dấu Card thành `CONFIRMED`, trả mã `bookingId` và thời gian hết hạn giữ chỗ `holdExpiresAt` để chuyển hướng sang thanh toán chính thức.

---

## 🔗 Bản Đồ Liên Kết Giữa Các Sơ Đồ

```
               [Sơ đồ 6: AI_Hybrid_Architecture] (Bức tranh tổng thể toàn hệ thống)
                                      |
         +----------------------------+----------------------------+
         |                                                         |
[TƯ VẤN & TRỢ LÝ ẢO]                                     [TÍNH NĂNG KHÁCH HÀNG & ADMIN]
         |                                                         |
 +-------+-------------------+                    +----------------+----------------+
 |                           |                    |                |                |
[Sơ đồ 1 & 2]          [Sơ đồ 10]            [Sơ đồ 7]        [Sơ đồ 8]        [Sơ đồ 9]
Luồng Chat &          Chatbot Đa Năng      Khám Phá Dịch Vụ  Lập Lịch Trình   Kiểm Duyệt &
Moderation 2 tầng      15 Tools & Đặt Vé    (Search/Nearby)   & Re-plan        Rủi Ro Giao Dịch
         |                   |
         +---------+---------+
                   |
         [Sơ đồ 3 & 4: Re-validation]
         (Xác thực tồn kho & khóa giữ chỗ)
                   |
         [Sơ đồ 5: RollKeys & Fallback]
         (Độ tin cậy hạ tầng Groq Cloud)
```
