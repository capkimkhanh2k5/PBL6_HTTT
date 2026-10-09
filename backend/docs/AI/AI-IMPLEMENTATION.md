# AI DANASEA: backend với Quyet Small

Phạm vi đã chốt ngày 08/10/2026: 10 tính năng khách hàng trong `AI-API.md`, phân loại văn bản, kiểm duyệt văn bản và dấu hiệu giao dịch bất thường. Groq dùng cho hội thoại/chọn trích đoạn review; Quyet Small dùng cho quyết định có cấu trúc. Không tích hợp Florence, Jev hoặc xử lý ảnh. API và tool backend đã triển khai; giao diện React/Flutter cần gọi các contract này.

## Tính năng và nguồn dữ liệu

| Tính năng | API | Hành vi hiện tại |
| --- | --- | --- |
| Travel Assistant | `POST /api/assistant/chat` | Groq gọi 15 tool (6 cũ + 9 mới), đọc dữ liệu backend; actor và conversation lấy từ server. |
| Smart Search | `POST /api/ai/search` | Trích hoạt động, ngân sách, số người, hôm nay/ngày mai; lọc dịch vụ PUBLISHED và option ACTIVE còn slot. Quyet phân loại intent để hiển thị. |
| Recommendation | `POST /api/ai/recommendations` | Baseline theo rating, sở thích nhập vào, vị trí; Quyet relevance góp điểm cho tối đa 10 ứng viên hợp lệ. Không học hành vi người dùng. |
| Itinerary Planner | `POST /api/ai/itineraries` | Tạo/lưu đề xuất theo slot thật, tổng giá, thời gian và quy tắc thời tiết. Không giữ chỗ. |
| Weather-Aware | `POST /api/ai/weather` | Đối chiếu service/option/slot hiện tại rồi gọi quy tắc an toàn đang có. Thiếu dữ liệu trả UNKNOWN; dự báo ước lượng ghi provisional. |
| Re-planning | `POST /api/ai/itineraries/{id}/replan` | Chủ lịch chủ động yêu cầu tính lại; hỗ trợ loại dịch vụ/slot hủy và đọc lại giá, inventory, thời tiết. Không tự đổi booking. |
| Service Comparison | `POST /api/ai/services/compare` | 2–4 service ID công khai; giá option và tổng cho nhóm, quyền lợi, duration, rating, vị trí, slot hiện tại; chính sách nền tảng chung từ RefundPolicyEngine. |
| Review Summary | `GET /api/ai/review-summaries/{serviceId}` | Mẫu tối đa 50 review thật chưa bị flag, trung bình/count, trích dẫn tích cực/tiêu cực theo rating; Quyet gợi ý aspect tối đa 20 comment. Groq chỉ chọn ID nguồn hợp lệ; fallback trích đoạn. |
| Nearby Discovery | `POST /api/ai/nearby` | Cần lat/lon; loại nguồn thiếu tọa độ, kiểm tra bán kính Haversine, sắp xếp khoảng cách với slot còn chỗ. |
| Customer Support | `POST /api/ai/support` | Đơn của chính khách, trạng thái thanh toán và preview hủy theo chính sách hiện tại; Quyet phân loại topic/urgency. Đổi lịch/hủy/hoàn tiền vẫn qua API nghiệp vụ với xác nhận. |

Đọc AI không tăng view_count và không tạo lịch sử xem gần đây. Giá legacy trên service không được dùng thay giá option cho tìm kiếm/lập lịch/confirmation card mới. `PER_PERSON`: quantity = partySize; `PER_PACKAGE`: quantity = ceil(partySize/maxPaxPerPackage); `partyTotal = unitPrice * quantity`. Inventory dùng API availability hiện có, bao gồm Redis hold, đơn vị sức chứa và option riêng/chung, không cho chia nhóm ngầm.

## Contract chung

Các route `/api/ai/**` yêu cầu đăng nhập. Actor lấy từ SecurityContext, không nhận userId/isAdmin từ request. Accept-Language chọn dữ liệu dịch vụ Việt/Anh. Vi phạm input trả 400; resource không thấy/không thuộc chủ lịch trả 404; đơn của người khác 403; version đổi 409; rate limit 429. Nguồn không tồn tại không được model tự tạo.

Body cho search/recommendations/nearby/create itinerary, hoặc trong trường `context` của comparison/weather:

```json
{
  "query": "Kayak ngày mai cho 3 người dưới 200k mỗi người",
  "partySize": 3,
  "totalBudget": 600000,
  "from": "2026-10-09",
  "to": "2026-10-09",
  "dayStart": "08:00",
  "dayEnd": "18:00",
  "latitude": 16.1,
  "longitude": 108.2,
  "radiusKm": 10,
  "interests": ["kayak"],
  "limit": 10,
  "maxActivities": 3,
  "weatherSafeOnly": false
}
```

Ngày ví dụ phải thay bằng ngày hiện tại/tương lai. `totalBudget` luôn là ngân sách **cả nhóm**, ưu tiên hơn ngân sách trong query; nếu chỉ query có “mỗi người”, parser nhân với số người. Mặc định 1 người, today → today+7 ngày, 08–18h, limit 10/maxActivities 3. Party 1–50, budget 0–1 tỷ VND, cửa sổ ngày ≤14 ngày, limit 1–20/maxActivities 1–5, radius (0,200] km. Không hỗ trợ lịch qua nửa đêm. Ngày nhập rõ ràng được ưu tiên; parser ngôn ngữ tự nhiên chỉ bao phủ các mẫu đã nêu, không suy ra ngày tùy ý hoặc attribute phức tạp.

Kết quả discovery có `status`, `context`, `retrievalKeyword`, `effectiveTotalBudget`, `candidates`, `requiredInputs`, `intent`, `rankingMethod`, `exhaustive=false` và `limitations`. `NO_MATCHES` là không thấy trong mẫu catalog đang xét; `NEEDS_INPUT` chỉ ra dữ liệu cần bổ sung. Mỗi candidate có snapshot, giá tổng nhóm, khoảng cách (nếu biết), reasonCodes và kết quả relevance/unavailable. Truy vấn catalog bị giới hạn 50 dịch vụ; weatherSafeOnly kiểm tra tối đa 20 slot nhạy thời tiết (cache theo slot); nguồn vượt giới hạn bị bỏ và limitations ghi rõ. Không bảo đảm tìm mọi dịch vụ trong hệ thống hoặc nearest toàn cục. Đây chưa phải vector search hay một thuật toán recommendation đã được đánh giá.

Comparison: `{"serviceIds":["uuid-1","uuid-2"],"context":{...}}`. Không có chính sách riêng từng vendor trong nguồn hiện tại; `commonPolicy` ghi rõ phạm vi chung. Tham khảo preview đơn để biết eligibility/số tiền thực tế.

Weather: `{"serviceId":"uuid","optionId":"uuid","slotId":"uuid","context":{...}}`; `bookingAuthorized=false`. Quyết định safety thuộc rule engine/weather provider, không thuộc Quyet.

Support: `{"orderId":"uuid","message":"Tôi muốn đổi lịch","includeCancellationPreview":true}`. Thiếu orderId trả NEEDS_INPUT; preview không thực hiện hủy/hoàn tiền. Response loại bỏ commission/vendor payout. Không có chức năng tự gửi ticket hay đổi lịch tự động.

## Lịch trình và replan

`GET /api/ai/itineraries` trả tối đa 50 lịch của actor; `GET /api/ai/itineraries/{id}` chỉ đọc của actor. Migration V21 tạo `ai_itineraries`, JSON snapshot, @Version và index theo owner. Lịch cũ vẫn đọc được dù ngày đã qua; cần cập nhật ngày trước khi replan.

Planner xét tối đa 10 candidate, tối đa 5 slot/option, dùng greedy theo giờ bắt đầu rồi giá. Mỗi dịch vụ tối đa một lần, tối đa maxActivities toàn lịch. Tổng ngân sách và chuyển tiếp kiểm tra bằng code: 15 phút đệm + khoảng cách đường chim bay với giả định 25 km/h. Thiếu tọa độ cho chuyển tiếp sẽ bỏ ứng viên và ghi constraint. Đây là ước lượng, chưa có routing đường bộ hoặc tối ưu toàn cục. Giá chỉ là snapshot, `inventoryReserved=false`.

```json
{
  "expectedVersion": 0,
  "context": {"partySize": 3, "from": "2026-10-09", "totalBudget": 600000},
  "excludedServiceIds": [],
  "excludedSlotIds": ["uuid-slot-cancelled"],
  "trigger": "CUSTOMER_REQUEST"
}
```

Replan kiểm tra owner trước khi đọc dữ liệu/tính toán; optimistic locking chống ghi đè. Trả `removedSlotIds`, `addedSlotIds`, `changedItems` (giá/option/thông tin đổi trên cùng slot), `bookingChanged=false`. Weather/service cancellation không tự chạy background replan hoặc phát thông báo; client/assistant gọi khi có yêu cầu và thông tin nguồn cập nhật.

Booking assistant: `request_booking_confirmation` cần service_id, option_id, slot_id, date, participants. Card lưu unitPrice, quantity và participants riêng; user xác nhận mới tạo native booking hold. Khi giá/slot thay đổi, yêu cầu xác nhận lại; nếu giá đổi ngay lúc tạo hold thì hủy hold và trả refresh_required. Card legacy thiếu option không được giữ inventory bằng production path mới. API thanh toán hiện có quyết định trạng thái cuối cùng.

## Phân loại, kiểm duyệt và transaction risk

- `POST /api/ai/classifications/service`: `{"text":"Tour kayak có hướng dẫn viên"}`. Trả nhãn SUP/KAYAK/DIVING/BOAT/BEACH/OTHER hoặc unavailable. Không tự thay category của listing.
- `POST /api/ai/content-assessments`: `{"text":"..."}`. Tối đa 2500 ký tự; regex liên hệ + Quyet external_payment/spam/abuse. Lưu case, `publicationAuthorized=false`, `evidenceType=TEXT_ONLY`. Text do người gọi cung cấp; không giả định là listing đã xác minh. Không quét ảnh, URL ảnh hoặc toàn catalog tự động.
- Admin `POST /api/admin/ai/risk-cases`: `{"orderId":"uuid","complaint":"..."}`. Đọc order/customer và đếm payment FAILED trong 24h, số order 1h, refund record 7d bằng backend. Ngưỡng demo 3/5/3; ruleVersion `velocity-demo-v1`, chưa hiệu chỉnh với fraud label. Refund record có thể là hoàn hợp lệ, failed payment có thể do provider; đây chỉ là tín hiệu cần xem xét.
- Admin `GET /api/admin/ai/assessment-cases?status=NEEDS_REVIEW&limit=50`; status cho phép NEEDS_REVIEW/NO_RULE_SIGNAL/REVIEWED/DISMISSED.
- Admin `POST /api/admin/ai/assessment-cases/{id}/resolve`: `{"resolution":"DISMISSED","note":"Đã đối chiếu nguồn"}`; note 1–1000 ký tự, version chống đồng thời, không được resolve hai lần.

Rule backend luôn thắng model trả NO. Model lỗi chuyển NEEDS_REVIEW; NO_RULE_SIGNAL không nghĩa là nội dung an toàn/giao dịch không gian lận. Cases lưu actor, evidence và model/revision/rubric version trong V21 `ai_assessment_cases`; chỉ admin đọc queue. Resolution ghi người duyệt và note, không sửa source/payment. Cần chính sách lưu/xóa evidence trước khi dùng với dữ liệu cá nhân trên môi trường thực.

## Khởi động và giới hạn bằng chứng

Xem [worker README](../ai-decision/README.md), `.env.example` và `docker-compose.ai.yml`. `AI_QUYET_ENABLED=false` mặc định; bật với URL và key riêng. Groq vẫn cần GROQ_API_KEYS nếu dùng chat. Các endpoint dữ liệu vẫn trả baseline và unavailable khi worker không chạy. Model nạp offline từ cache đã xác minh, không tải lại theo request.

Kết quả local 7 task nằm ở [local-smoke-results.json](../ai-decision/local-smoke-results.json). Trong lần thử minh họa này, model gắn nhầm câu cảnh báo chống chuyển tiền ngoài thành external_payment. Vì vậy không dùng confidence/Noul để khẳng định chất lượng DANASEA; cần tập dữ liệu Việt có nhãn, phủ định/đối kháng, chia train/validation/test và đo false positive/false negative trước khi tự động hóa.

Validation được ghi riêng ở [AI-VALIDATION.md](AI-VALIDATION.md): compilation, unit/HTTP integration, Flyway/PostgreSQL, Python và local real-model smoke. Backend container đã chạy từ trước cần rebuild để nhận code/migration mới; không thay container hoặc dữ liệu hiện có trong quá trình kiểm thử này.
