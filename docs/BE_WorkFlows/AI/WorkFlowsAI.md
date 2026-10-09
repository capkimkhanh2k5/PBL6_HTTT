# 🌊 DANASEA — TÀI LIỆU KỸ THUẬT & LUỒNG NGHIỆP VỤ HỆ THỐNG AI (WORKFLOWS AI)

---

## 1. KIẾN TRÚC TỔNG THỂ HỆ THỐNG AI LAI (HYBRID AI ARCHITECTURE)

Hệ thống AI của DANASEA áp dụng mô hình **Dual-Brain Hybrid Architecture** kết hợp giữa Điện toán Đám mây (Cloud LLM) và Máy chủ Suy luận Cục bộ (Local Fast-Inference Worker):

```
Client (Web/App)
       │
       ▼
Spring Boot Core Backend (Clean Architecture)
       │
       ├──► Groq Cloud LLM (Llama 3.3/3.1) ────────► Hội thoại, sinh ngôn ngữ tự nhiên, trích xuất dẫn chứng review
       │
       ├──► Quyet Small Worker (Fast Inference) ───► Phân loại ý định, chấm điểm gợi ý, kiểm duyệt text, rủi ro giao dịch
       │
       ├──► PostgreSQL (Flyway V21) ──────────────► Bảng ai_itineraries (@Version), ai_assessment_cases, catalog dịch vụ
       │
       └──► Redis Cache ──────────────────────────► Rate limiter, Draft Confirmation Card (15p), Distributed Locks (30s)
```

### Nguyên tắc Bất biến Cốt lõi (Invariants):
1. **Không có quyền tự động can thiệp tài chính/tồn kho:** Cả Groq LLM và Quyet Worker đều không bao giờ được phép trực tiếp sửa đổi tiền, hủy booking, hoàn tiền hay tạo hóa đơn trong DB.
2. **Backend Code luôn nắm quyền quyết định cuối cùng:** Khi mô hình AI trả về kết quả mâu thuẫn với quy tắc hệ thống, Backend Rule Engine luôn ghi đè kết quả của AI.
3. **Truy vấn AI không gây ô nhiễm dữ liệu vận hành:** Các yêu cầu đọc dữ liệu qua AI không được làm tăng `view_count` của dịch vụ và không ghi lịch sử xem gần đây.

---

## 2. BỘ CÔNG CỤ TOÀN DIỆN CỦA TRỢ LÝ ẢO (AI TOOL REGISTRY — 15 TOOLS)

Trợ lý ảo (`POST /api/assistant/chat`) được trang bị **15 công cụ kỹ thuật** (6 công cụ cơ bản + 9 công cụ khách hàng mở rộng):

### A. 6 Công cụ cơ bản (Legacy Core Tools)
1. **`search_services`** *(READ - PostgreSQL)*: Tìm kiếm dịch vụ theo từ khóa, tầm giá và danh mục bằng Full-Text Search.
2. **`get_service_detail`** *(READ - PostgreSQL)*: Lấy thông tin chi tiết dịch vụ, biểu giá các tùy chọn đặt và các khung giờ khả dụng.
3. **`get_policy`** *(READ - PostgreSQL/Redis)*: Đọc chính xác văn bản chính sách hệ thống (Hủy vé, hoàn tiền, hủy do thời tiết, quy chuẩn an toàn).
4. **`get_weather_forecast`** *(READ - Weather API)*: Dự báo thời tiết và tình trạng sóng biển tại các vùng biển Đà Nẵng (Sơn Trà, Mỹ Khê, Mân Thái).
5. **`get_safety_alert`** *(READ - Maritime Safety)*: Cảnh báo an toàn hàng hải, ngưỡng rủi ro sóng to gió lớn cho từng loại hình hoạt động biển.
6. **`request_booking_confirmation`** *(WRITE - Redis)*: Tạo phiếu xác nhận đặt chỗ nháp (`ConfirmationCard`) dạng Native Option với `option_id`, `slot_id`, `unitPrice`, `participants` lưu vào Redis với TTL 15 phút.

### B. 9 Công cụ mở rộng khách hàng (CustomerFeatureTool)
7. **`ai_smart_search`** *(READ)*: Tìm kiếm dịch vụ bằng ngôn ngữ tự nhiên, tự động trích xuất ngân sách nhóm, số người, ngày đi và ràng buộc giờ.
8. **`ai_recommend_services`** *(READ)*: Đề xuất dịch vụ dựa trên sở thích, điểm đánh giá và điểm tương quan `RELEVANCE` từ Quyet Worker.
9. **`ai_nearby_services`** *(READ)*: Tìm kiếm trải nghiệm biển xung quanh tọa độ GPS người dùng bằng công thức bán kính Haversine.
10. **`ai_compare_services`** *(READ)*: So sánh từ 2 đến 4 dịch vụ về tổng chi phí cho cả đoàn, quyền lợi gói, thời lượng và chính sách hoàn tiền chung.
11. **`ai_plan_itinerary`** *(WRITE - PostgreSQL V21)*: Tự động lập lịch trình du lịch biển tối ưu ngân sách, kiểm tra thời gian di chuyển giữa các điểm và tính an toàn thời tiết.
12. **`ai_replan_itinerary`** *(WRITE - PostgreSQL V21)*: Tái lập lịch trình khi có slot bị hủy hoặc thời tiết thay đổi, kiểm soát đồng thời bằng Optimistic Locking (`@Version`).
13. **`ai_review_summary`** *(READ)*: Đọc tóm tắt đánh giá trích xuất từ tối đa 50 bình luận thực tế; Quyet phân tích khía cạnh và Groq trích dẫn câu có ID nguồn kiểm chứng.
14. **`ai_weather_slot`** *(READ)*: Kiểm tra an toàn thời tiết cho một ca dịch vụ cụ thể; nếu thiếu dữ liệu sẽ trả về `UNKNOWN` chứ không tự ý cho phép đặt chỗ.
15. **`ai_customer_support`** *(READ)*: Tra cứu trạng thái đơn hàng của chính khách hàng đăng nhập và xem trước chính sách hủy (`CancellationPreviewResult`).

---

## 3. DANH SÁCH TOÀN BỘ API BACKEND MODULE AI

### A. Nhóm Chat Assistant (`/api/assistant/**`)
- **`POST /api/assistant/chat`** *(READ + WRITE)*: Tiếp nhận câu hỏi, kiểm tra Rate Limit (Token Bucket + TrustTier), quét LlamaGuard, nạp lịch sử từ DB, gọi Groq LLM trong vòng lặp Tool Calling (tối đa 5 lượt) và trả lời.
- **`GET /api/assistant/conversations/{id}`** *(READ)*: Lấy thông tin hội thoại. Kiểm tra quyền sở hữu của User (403 Forbidden nếu truy cập chéo).
- **`GET /api/assistant/conversations/{id}/history`** *(READ)*: Lấy lịch sử tin nhắn gần nhất kèm kiểm tra quyền sở hữu.
- **`POST /api/assistant/conversations/{id}/confirm`** *(READ + WRITE)*: Xác nhận đặt chỗ từ Thẻ nháp. Thiết lập khóa phân tán Redis (30s), kiểm tra ràng buộc 3 bên (Card — Conversation — User), thẩm định lại giá & slot thời gian thực, kích hoạt `CreateBookingHoldUseCase` giữ chỗ 15 phút.

### B. Nhóm Tính Năng Khách Hàng AI (`/api/ai/**` — Yêu cầu Đăng nhập)
- **`POST /api/ai/search`**: Khám phá thông minh bằng ngôn ngữ tự nhiên. Trả về candidates và intent của khách.
- **`POST /api/ai/recommendations`**: Gợi ý dịch vụ theo ngữ cảnh và sở thích kết hợp Quyet Relevance Scoring.
- **`POST /api/ai/nearby`**: Tìm dịch vụ quanh tọa độ `latitude`/`longitude` trong bán kính `radiusKm`.
- **`POST /api/ai/services/compare`**: So sánh 2–4 dịch vụ theo cấu trúc giá nhóm và tiện ích.
- **`POST /api/ai/weather`**: Đánh giá điều kiện thời tiết cho một cặp Service-Option-Slot cụ thể.
- **`GET /api/ai/review-summaries/{serviceId}`**: Tóm tắt đánh giá khách quan dựa trên review thật chưa bị gắn cờ.
- **`POST /api/ai/support`**: Hỗ trợ khách hàng tra cứu đơn của chính mình và xem trước điều kiện hoàn hủy.
- **`POST /api/ai/itineraries`**: Tạo và lưu bản ghi lịch trình đề xuất vào bảng `ai_itineraries`.
- **`GET /api/ai/itineraries`**: Lấy danh sách lịch trình của chính người dùng (tối đa 50 lịch).
- **`GET /api/ai/itineraries/{id}`**: Xem chi tiết lịch trình theo ID (kiểm tra quyền sở hữu).
- **`POST /api/ai/itineraries/{id}/replan`**: Tái lập lịch trình với `@Version` chống xung đột ghi đè.

### C. Nhóm Kiểm Duyệt & Đánh Giá Rủi Ro (`/api/ai/**` & `/api/admin/ai/**`)
- **`POST /api/ai/content-assessments`**: Quét văn bản (1–2500 ký tự) bằng Regex (SĐT/link) + Quyet Moderation (thanh toán ngoài, spam, lạm dụng) $\rightarrow$ lưu `ai_assessment_cases`.
- **`POST /api/ai/classifications/service`**: Dự đoán phân loại danh mục dịch vụ (SUP, KAYAK, DIVING...).
- **`POST /api/admin/ai/risk-cases`** *(ADMIN ONLY)*: Phân tích rủi ro giao dịch dựa trên vận tốc tạo đơn, thanh toán lỗi và hoàn tiền $\rightarrow$ tạo case thẩm định.
- **`GET /api/admin/ai/assessment-cases`** *(ADMIN ONLY)*: Lấy danh sách hồ sơ rủi ro/kiểm duyệt theo trạng thái (`NEEDS_REVIEW`).
- **`POST /api/admin/ai/assessment-cases/{id}/resolve`** *(ADMIN ONLY)*: Admin giải quyết hồ sơ (`RESOLVED`/`DISMISSED`) kèm ghi chú giải trình.

---

## 4. QUY TẮC NGHIỆP VỤ & THUẬT TOÁN ĐẶC TẢ

### 1. Thuật toán Lập Lịch Trình (Bounded Chronological Greedy with Estimated Transfers)
- **Đầu vào:** `TravelContext` (ngày đi, khung giờ `dayStart` - `dayEnd`, số người `partySize`, ngân sách tổng `effectiveBudget`, số lượng hoạt động tối đa `maxActivities`).
- **Lấy mẫu:** Lấy tối đa 10 ứng viên từ bộ lọc Recommendation, phẳng hóa tối đa 5 slot/option, sắp xếp tăng dần theo: `slot.date + slot.start` $\rightarrow$ `partyTotal` $\rightarrow$ `service.id`.
- **Duyệt tham lam (Greedy Loop):**
  1. Kiểm tra giới hạn: Dừng lại nếu đã đủ `maxActivities`.
  2. Không trùng lặp: Mỗi dịch vụ chỉ xuất hiện tối đa 1 lần trong cả hành trình.
  3. Ngân sách: Tổng chi phí không vượt quá `effectiveBudget`.
  4. **Ước lượng chuyển tiếp (Estimated Transfers):**
     $$\text{transitMinutes} = 15 \text{ (phút đệm)} + \left\lceil \frac{\text{distanceKm}}{25} \times 60 \right\rceil \text{ phút}$$
     Nếu $\text{start}_{\text{sau}} < \text{end}_{\text{trước}} + \text{transitMinutes} \rightarrow$ Bỏ qua slot do không kịp di chuyển giữa 2 bãi biển.
  5. Thời tiết: Phải đạt chuẩn `acceptable` từ `TravelWeatherPort`.
- **Kết quả:** Lưu DB bảng `ai_itineraries` với `version = 0`. Trạng thái: `PROPOSED` (nếu dự báo chuẩn) hoặc `PROVISIONAL` (nếu dự báo ước lượng).

### 2. Thuật toán Tái Lập Lịch Trình (Re-planning with Optimistic Locking)
- **Bảo vệ quyền sở hữu:** `itinerary.ownerId == currentUserId` (chặn 404/403).
- **Kiểm soát phiên bản:** Đối chiếu `request.expectedVersion == savedItinerary.version`. Nếu lệch $\rightarrow$ HTTP 409 Conflict.
- **Tính toán lại:** Loại bỏ các slot/dịch vụ trong `excludedSlotIds` và `excludedServiceIds`. Chạy lại thuật toán Greedy.
- **Cập nhật:** Lưu đè bản ghi và tăng `version` lên $+1$. Trả về danh sách chênh lệch: `removedSlotIds`, `addedSlotIds`, `changedItems` (`bookingChanged = false`).

### 3. Quy tắc Tính Giá Khách Đoàn (Party Pricing Formula)
- **Hình thức theo người (`PER_PERSON`):**
  $$\text{quantity} = \text{partySize}, \quad \text{partyTotal} = \text{unitPrice} \times \text{quantity}$$
- **Hình thức theo gói (`PER_PACKAGE`):**
  $$\text{quantity} = \left\lceil \frac{\text{partySize}}{\text{maxPaxPerPackage}} \right\rceil, \quad \text{partyTotal} = \text{unitPrice} \times \text{quantity}$$
  *(Ví dụ: Nhóm 11 người chọn cano riêng tối đa 10 người/gói $\rightarrow$ cần ít nhất 2 gói).*

### 4. Bộ Quy Tắc Phát Hiện Rủi Ro Giao Dịch (Velocity Rules 3/5/3)
Backend tính toán các chỉ số hành vi thực tế trong CSDL:
- **Số lần thanh toán thất bại:** $\ge 3$ lần trong vòng 24 giờ (`THREE_FAILED_PAYMENTS_IN_24H`).
- **Tần suất tạo đơn hàng:** $\ge 5$ đơn trong vòng 1 giờ (`FIVE_ORDERS_IN_1H`).
- **Tần suất yêu cầu hoàn tiền:** $\ge 3$ yêu cầu trong vòng 7 ngày (`THREE_REFUND_REQUESTS_IN_7D`).
- Kết hợp với xác suất rủi ro từ Quyet Worker task `RISK` (ngưỡng $\ge 0.5$). Nếu thỏa mãn bất kỳ điều kiện nào $\rightarrow$ Đưa vào hàng đợi `NEEDS_REVIEW`.

---

## 5. VÍ DỤ HAI LUỒNG THỰC TẾ ĐIỂN HÌNH

### Kịch bản 1: Khám Phá Thông Minh ➔ Lập Lịch Trình ➔ Re-plan Khi Có Biến Động
1. **Tìm kiếm:** Khách gọi `POST /api/ai/search` gửi `"Tìm chèo Sup ở Bán đảo Sơn Trà ngày mai cho 4 người dưới 300k/người"`.
   - Backend parse query: keyword `"chèo sup"`, `partySize = 4`, `effectiveBudget = 1.200.000đ`.
   - Hệ thống lọc catalog dịch vụ `PUBLISHED`, đọc Option và Slot còn trống, tính khoảng cách, trả danh sách kết quả.
2. **Lập lịch:** Khách gọi `POST /api/ai/itineraries` với các tiêu chí trên.
   - Hệ thống chạy thuật toán Greedy, tính thời gian di chuyển giữa các điểm (đệm 15 phút, tốc độ 25km/h), kiểm tra sóng gió thời tiết.
   - Lưu vào PostgreSQL bảng `ai_itineraries`, trả về bản kế hoạch chi tiết với `version: 0`.
3. **Re-plan khi slot bị hủy:** Giả sử một ca lặn san hô buổi chiều bị nhà cung cấp hủy do biển động.
   - Khách gọi `POST /api/ai/itineraries/{id}/replan` gửi kèm `expectedVersion: 0`, `excludedSlotIds: ["slot-da-huy"]`, `trigger: "SLOT_CANCELLED"`.
   - Backend xác thực chủ sở hữu và version $\rightarrow$ đề xuất ca thay thế phù hợp $\rightarrow$ tăng `version: 1` và trả về danh sách slot đã đổi.

### Kịch bản 2: Trợ Lý Đa Năng ➔ Chốt Vé Native Option ➔ Khóa Giữ Chỗ Re-validation
1. **Tư vấn:** Khách chat: *"Mai tôi muốn đi cano riêng ngắm Cù Lao Chàm nhóm 8 người, có gói nào phù hợp không?"*
   - Groq LLM kích hoạt công cụ `ai_smart_search` và `get_service_detail`. Backend trả về gói cano riêng (tối đa 10 người/gói, giá 2.500.000đ/gói).
2. **Chốt phiếu đặt vé nháp:** Khách đồng ý đặt $\rightarrow$ LLM gọi `request_booking_confirmation(service_id, option_id, slot_id, date, participants: 8)`.
   - Backend kiểm tra slot còn trống, sinh thẻ `ConfirmationCard` dạng Native Option lưu vào Redis với TTL 15 phút.
   - Khách nhìn thấy Thẻ Xác Nhận trên giao diện chat kèm nút **[Xác Nhận Đặt Chỗ]**.
3. **Thẩm định & Khóa giữ chỗ:** Khách bấm xác nhận $\rightarrow$ gọi `POST /api/assistant/conversations/{id}/confirm`.
   - Backend khóa phân tán Redis chống bấm đúp, kiểm tra quyền sở hữu hội thoại và thẻ nháp.
   - Đối chiếu lại giá và chỗ trống thực tế trong PostgreSQL.
   - Mọi thông tin hợp lệ $\rightarrow$ Kích hoạt `CreateBookingHoldUseCase`, chạy script Redis Lua khóa nguyên tử và tạo bản ghi Booking trạng thái `HOLD` (15 phút) trong CSDL.
   - Chuyển khách hàng sang màn hình thanh toán đơn hàng an toàn.