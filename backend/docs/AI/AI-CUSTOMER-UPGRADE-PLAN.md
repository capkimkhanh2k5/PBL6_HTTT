# Kế hoạch nâng cấp 10 tính năng AI khách hàng DANASEA

Ngày rà soát: 09/10/2026. Trạng thái: **đề xuất để chốt phạm vi; chưa sửa application code**. Nguồn: controller, use case, read adapters và màn hình chat hiện tại của checkout PBL6. Sáu nhóm Vendor/Admin chưa triển khai tiếp theo yêu cầu. Quyet phục vụ quyết định text/JSON; Groq phục vụ hội thoại/trích dữ kiện/diễn đạt; không bổ sung Florence, Jev hoặc vision.

## Định nghĩa hoàn chỉnh trong phạm vi này

Khách thực hiện được cả luồng từ câu hỏi → hỏi rõ → kết quả có nguồn → chọn/lưu/so sánh → preview/xác nhận → API nghiệp vụ và theo dõi trạng thái. API nhất quán, nội dung không vượt dữ liệu, model lỗi có fallback, UI web/mobile gọi backend thật. Đây không phải yêu cầu giải quyết mọi câu ngôn ngữ tự nhiên hay một thuật toán tối ưu toàn cục.

10 nhóm đều cần nâng cấp, nhưng không cần thay toàn bộ endpoint. Phần lớn giữ route, mở rộng contract và use case. Các route mới dưới đây chỉ là đề xuất.

## Các phát hiện dùng chung cần xử lý đầu tiên

1. AssistantController trả chỉ status/conversationId/message; tool JSON nằm trong history, chưa thành typed cards/sources/actions cho UI. Web AiAssistant dùng setTimeout và mobile AiAssistantScreen dùng Future.delayed/mock, kèm lời khuyên thời tiết cố định. Cần thay bằng kết quả backend thật, không hiển thị các số sóng/tầm nhìn mock như forecast.
2. AiCatalogReader lấy tối đa 50 dịch vụ trước khi lọc giá/options/slots; repository ORDER BY createdAt DESC. Hệ quả: search có thể bỏ nguồn hợp lệ, Nearby không phải nearest toàn catalog, planner/ranking kế thừa pool sai lệch.
3. Có search_vector song ngữ và GIN ở V11, nhưng luồng mới dùng catalog keyword LIKE. Adapter search cũ dùng FTS nhưng giá service legacy và limit 5. Cần thống nhất retrieval và quote option, không chỉ chuyển tool cũ vào use case mới một cách cơ học.
4. Input có giá trị mặc định nhưng chưa đủ clarification/context. Output cần resolvedCriteria, source/freshness, coverage, reasons, warnings và actions; internal model probabilities không nên trở thành thông tin khách phải hiểu.
5. Các fallback hiện có bảo vệ transport, nhưng chưa chứng minh model quality. Quyet từng sai với câu phủ định; confidence/Noul không phải tỷ lệ đúng đã đo. Policy guard keyword trong chat cũng chưa kiểm chứng đầy đủ claim và có thể chặn preview hợp lệ vì chỉ ghi nhận get_policy.

Nguồn trực tiếp: [AssistantController](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/presentation/controllers/AssistantController.java:90), [catalog retrieval](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/application/api/AiCatalogReader.java:30), [newest ordering](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/adapters/ServiceRepositoryAdapter.java:115), [web mock](/Users/capkimkhanh/Documents/DUT4_1/PBL6/frontend/src/pages/customer/Profile/AiAssistant.tsx:24), [mobile mock](/Users/capkimkhanh/Documents/DUT4_1/PBL6/mobile/lib/features/chat/presentation/screens/ai_assistant_screen.dart:54).

## Nâng cấp theo từng tính năng

### 1. Travel Assistant — P0

API: `POST /api/assistant/chat`; `POST /api/assistant/conversations/{id}/confirm`; `GET /api/assistant/conversations/{id}/history`.

Hiện tại: Chat chỉ trả status, conversationId, message; kết quả tool và card xác nhận chưa trở thành payload UI có kiểu. Context chủ yếu là 20 message gần nhất; policy guard dựa vào từ khóa/get_policy.

Cần cải thiện:

- Trả messageId, content, typed cards, sources, clarificationQuestions và actions; backend dựng các dữ kiện giá/slot từ tool, không yêu cầu FE đọc JSON trong câu trả lời.
- Lưu context có cấu trúc theo conversation: party, ngày đã resolve, budgetBasis, preferences, service/option/slot được chọn và itineraryId; hỏi lại khi thiếu hoặc xung đột.
- Đồng bộ tool cũ và ai_* vào cùng catalog/quote/policy contract; thay guard chỉ nhận get_policy bằng xác minh nguồn policy/cancellation preview được phép.
- Thêm idempotency cho gửi lại message/xác nhận và lưu outcome bookingId; giữ processing lock, kiểm giá/slot lại và trạng thái cần xác nhận. Streaming có thể bổ sung sau contract.

Tiêu chí nghiệm thu:

- Hội thoại '3 người ngày mai' → 'không SUP, chọn kayak' giữ đúng context, trả card nguồn thật và bước hỏi rõ khi cần.
- Retry sau timeout không tạo card/hold trùng; policy lấy từ preview không bị chặn nhầm; mỗi con số trên card khớp nguồn backend.

Nguồn code: [AssistantController.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/presentation/controllers/AssistantController.java), [ChatUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/ChatUseCase.java).

### 2. Smart Search — P0

API: `POST /api/ai/search`.

Hiện tại: Parser theo mẫu nhỏ; chỉ hiểu hôm nay/ngày mai. Hoạt động ngoài map có thể trả keyword null rồi duyệt catalog rộng; ngày, giờ, địa điểm, phủ định và nhiều hoạt động chưa được hiểu đầy đủ.

Cần cải thiện:

- Có pipeline trích criteria có schema: ngày cụ thể/cuối tuần, giờ, nhiều category, địa điểm, budget cả nhóm/mỗi người và exclusions; Groq hỗ trợ trích dữ kiện, Quyet hỗ trợ intent, code resolve/validate.
- Trả resolvedCriteria, applied/defaulted/unsupported constraints và clarificationQuestions; không âm thầm áp số người/ngày khi câu yêu cầu mơ hồ.
- Tận dụng search_vector song ngữ hiện có, bổ sung từ đồng nghĩa/không dấu/địa điểm; thống nhất với catalog active-option để không quay lại giá service legacy.
- Lọc theo option và slot trước khi cắt pool; pagination/cursor, coverage và total đủ nghĩa; bổ sung metadata có nguồn cho trẻ em, kỹ năng bơi hoặc thiết bị trước khi hứa lọc các thuộc tính đó.

Tiêu chí nghiệm thu:

- Bộ query Việt/Anh kiểm từng field, ngày cuối tuần, phủ định và budgetBasis; câu 'đi cuối tuần cho gia đình dưới 1 triệu' phải hỏi rõ phần thiếu.
- Dịch vụ hợp lệ ngoài 50 bản mới nhất vẫn được tìm thấy; ràng buộc giá, party và slot không bị model nới lỏng.

Nguồn code: [TravelQueryParser.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/domain/services/TravelQueryParser.java), [DiscoverServicesUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/DiscoverServicesUseCase.java), [V11__backend_i18n.sql](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/resources/db/migration/V11__backend_i18n.sql).

### 3. Recommendation — P1

API: `POST /api/ai/recommendations`.

Hiện tại: Công thức rating + trùng sở thích + khoảng cách; Quyet chỉ chấm 10 candidate đầu trong pool hợp lệ, chưa nối hồ sơ/hành vi hoặc đo chất lượng ranking.

Cần cải thiện:

- Kết nối preference được khách nhập và cho phép lưu; reuse wishlist/recently viewed qua read API của module, bổ sung feedback/impression từ tương tác thật.
- Xếp hạng baseline trước, chấm relevance trên top candidate có chủ đích, dùng rankingVersion và feature contribution; giữ filters giá/tồn chỗ là điều kiện bắt buộc.
- Chuẩn hóa ảnh hưởng số lượng review, đa dạng category/vendor, hỗ trợ cold-start bằng context và giải thích 'vì sao đề xuất' từ dữ kiện.
- Đánh giá top-K trên dữ liệu có nhãn/feedback, so baseline với Quyet; ngưỡng dùng score và fallback được chốt trước tập test.

Tiêu chí nghiệm thu:

- Hai khách có preference khác nhau nhận khác biệt có thể giải thích; người mới vẫn có gợi ý phù hợp.
- Không gọi 'cá nhân hóa tốt' chỉ từ test kỹ thuật; cần metric ranking có denominator, baseline và tập đánh giá không bị dùng để chỉnh policy.

Nguồn code: [DiscoverServicesUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/DiscoverServicesUseCase.java), [WishlistController.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/WishlistController.java), [RecentlyViewedController.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/RecentlyViewedController.java).

### 4. Itinerary Planner — P1

API: `POST /api/ai/itineraries`; `GET /api/ai/itineraries`; `GET /api/ai/itineraries/{id}`.

Hiện tại: Greedy theo giờ rồi giá, chỉ 10 candidate/5 slot mỗi option, maxActivities áp cho cả lịch. Chuyển tiếp đường chim bay 25 km/h; thiếu tọa độ bỏ ứng viên tiếp theo.

Cần cải thiện:

- Lập theo từng ngày, giờ rảnh, thời gian nghỉ/ăn, starting point và số hoạt động/ngày; tách serviceCost với transferEstimate và các khoản có nguồn.
- Tìm nhiều phương án có giới hạn thay vì chọn sớm nhất ngay; tối ưu budget, preferences, di chuyển và coverage; trả 2–3 lựa chọn cùng lý do trade-off.
- Xác định transfer mode/estimate source và uncertainty; thêm tọa độ đáng tin cậy. Có thể giữ ước lượng ở phạm vi demo nhưng phải hiển thị và cho khách điều chỉnh.
- Có draft/accepted/stale lifecycle, preview trước save, idempotency khi tạo, sửa/chọn/archiving và phân trang; kiểm lại quote khi chuyển sang booking. Draft xa ngày forecast ghi WAITING_FOR_FORECAST, không khẳng định safe.

Tiêu chí nghiệm thu:

- Fixture có một slot sớm rẻ nhưng làm mất hoạt động tốt phía sau: planner phải có phương án thay thế và giải thích.
- Đúng constraints theo từng ngày, không chồng giờ, ngân sách không vượt, phần chưa đạt ghi rõ; accepted itinerary vẫn không đồng nghĩa đã reserve booking.

Nguồn code: [PlanItineraryUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/PlanItineraryUseCase.java), [ItineraryPlan.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/domain/models/ItineraryPlan.java), [ItineraryStoreAdapter.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/infrastructure/persistence/ItineraryStoreAdapter.java).

### 5. Weather-Aware — P0

API: `POST /api/ai/weather`.

Hiện tại: Đã kiểm service/option/slot và phân biệt UNKNOWN/provisional, nhưng payload chưa đủ thời điểm cập nhật/hết hiệu lực, đường dẫn phương án thay thế và sự liên kết theo dõi lịch.

Cần cải thiện:

- Trả provider/source, forecastAt, checkedAt, validUntil, timezone, coverage, metrics/thresholds và lý do an toàn theo đúng rule engine.
- Phân biệt dữ liệu thiếu, vượt horizon, dữ liệu ước lượng, stale và unsafe; giới hạn forecast cần dẫn đến trạng thái partial/needs-revalidation, không giả thành 'không có dịch vụ'.
- Gợi ý các slot/ngày khác qua inventory + weather đã kiểm; chọn alternative không tự authorize booking.
- Liên kết lịch đã chấp nhận với các slot cần theo dõi; reuse job/rule/notification của weather, tránh dựng một engine an toàn mới bằng model.

Tiêu chí nghiệm thu:

- Thiếu forecast/forecast stale không xuất lời hứa biển an toàn; estimated marine luôn được hiển thị provisional.
- Khi slot không phù hợp, trả alternative có nguồn và thời gian dữ liệu; quyết định cuối của booking theo core rules.

Nguồn code: [WeatherAwareUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/WeatherAwareUseCase.java), [TravelWeatherAdapter.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/infrastructure/adapters/TravelWeatherAdapter.java).

### 6. Re-planning — P1

API: `POST /api/ai/itineraries/{id}/replan`.

Hiện tại: Chỉ chạy theo lời gọi chủ lịch; trigger là text người gọi, chưa gắn event xác thực; tính xong ghi đè plan ngay, chưa có preview/accept và chưa phân biệt hoạt động đã trả tiền.

Cần cải thiện:

- Gắn event thời tiết/slot hủy với itinerary items có index; giữ eventId, lý do nguồn và chống xử lý event trùng.
- Tạo proposed revision và diff, khách xem rồi accept với expectedVersion; background chỉ tạo đề xuất/thông báo, không tự overwrite lịch đang chấp nhận.
- Khóa các booking đã trả tiền, giữ phần không ảnh hưởng; phân biệt sửa kế hoạch dự kiến và yêu cầu đổi booking theo nghiệp vụ.
- Diff bao gồm ngày/option/giá/tổng tiền/di chuyển, replacement reason và constraints bị mất; version history, no-alternative và conflict có hành vi rõ.

Tiêu chí nghiệm thu:

- Một slot hủy chỉ làm thay phần liên quan; booking đã trả tiền và payment không tự bị sửa.
- Hai yêu cầu đồng thời không ghi đè; event lặp không tạo nhiều thông báo/đề xuất giống nhau; khách có thể từ chối hoặc accept đề xuất.

Nguồn code: [PlanItineraryUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/PlanItineraryUseCase.java), [AiItineraryJpaEntity.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/infrastructure/persistence/entities/AiItineraryJpaEntity.java).

### 7. Service Comparison — P1

API: `POST /api/ai/services/compare`.

Hiện tại: Trả list snapshot và tên trường; chưa có ma trận khác biệt, đánh giá fit theo nhu cầu, hoặc recommended option có giải thích. Nguồn chính sách đang là common platform policy.

Cần cải thiện:

- Chuẩn hóa 2–4 dịch vụ trên cùng party/date/currency và basis; chọn option tương đương, tách không có option/slot thay vì so giá không cùng điều kiện.
- Trả comparison matrix, key differences, pros/cons có nguồn và bestFit theo tiêu chí khách; không tuyên bố 'chất lượng cao nhất' chỉ từ rating.
- Nguồn quality gồm rating + số review + review evidence + xác minh vendor khi có dữ liệu; điều kiện tham gia/thiết bị cần metadata xác nhận.
- Chính sách chung hiển thị riêng; chỉ so chính sách riêng vendor nếu được lưu và có hiệu lực. Snapshot có freshness và bước kiểm lại lúc đặt.

Tiêu chí nghiệm thu:

- Gói private hai người và giá per-person được so bằng partyTotal cùng điều kiện, không đánh tráo unitPrice.
- Không tạo điểm mạnh/chính sách thiếu nguồn; option unavailable và dữ liệu thiếu được hiển thị rõ.

Nguồn code: [CompareServicesUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/CompareServicesUseCase.java), [AiPolicyReader.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/application/api/AiPolicyReader.java).

### 8. Review Summary — P1

API: `GET /api/ai/review-summaries/{serviceId}`.

Hiện tại: Đếm rating/mẫu 50 review mới nhất, trích đoạn và gợi ý một aspect cho 20 comment; Groq chỉ chọn ID, chưa tạo bản tổng hợp ưu/nhược điểm được kiểm chứng.

Cần cải thiện:

- Tách thống kê toàn bộ review hợp lệ khỏi sample NLP, chọn mẫu phủ positive/neutral/negative và khoảng thời gian; bỏ thiên lệch chỉ review mới nhất.
- Quyet gợi ý nhiều aspect/sentiment có rubric cố định; aggregate theo aspect có count và evidenceReviewIds. Rating tốt/xấu không đồng nghĩa comment có/không có nhược điểm.
- Groq diễn đạt từ aggregate được xác minh; mỗi mệnh đề phải hỗ trợ bởi review thật. ID tồn tại không tự chứng minh câu tóm tắt đúng; fallback extractive khi không đủ căn cứ.
- Cache/background theo service reviewVersion + locale + model/rubric version; invalidation khi review bị flag/sửa/xóa. Ít review ghi insufficient evidence.

Tiêu chí nghiệm thu:

- Không có review không sinh ưu/nhược điểm; mẫu ít hoặc lệch phải ghi coverage và không mô tả như ý kiến toàn bộ khách.
- Kiểm tra riêng claim support, sentiment/negation và aspect; review bị flag/xóa biến mất khỏi summary/cache.

Nguồn code: [ReviewSummaryUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/ReviewSummaryUseCase.java), [AiReviewReadAdapter.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/operation/infrastructure/persistence/AiReviewReadAdapter.java), [GroqReviewHighlightAdapter.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/infrastructure/groq/GroqReviewHighlightAdapter.java).

### 9. Nearby Discovery — P0

API: `POST /api/ai/nearby`.

Hiện tại: Đã có bán kính Haversine nhưng chỉ sort sau khi lấy 50 dịch vụ mới nhất; một dịch vụ gần hơn, cũ hơn có thể bị bỏ trước khi tính khoảng cách.

Cần cải thiện:

- Lọc geo chính xác và ORDER BY khoảng cách trước LIMIT/pagination; xét options/slots hợp lệ trong retrieval, không lấy mẫu newest rồi gọi là nearest.
- Bổ sung hoạt động có thể bắt đầu trong khoảng thời gian khách còn rảnh, thời gian đến điểm và starting point; phân biệt đường chim bay với thời gian đi thật.
- Cho chọn tọa độ/địa điểm thủ công khi khách không cấp location; timestamp/độ chính xác vị trí nếu dùng GPS.
- Trả distanceKm, travel estimate source, eligible slot và các bộ lọc được áp dụng; mở rộng radius phải xin/ghi rõ thay đổi criteria.

Tiêu chí nghiệm thu:

- Catalog >50 dịch vụ: bản cũ gần nhất vẫn đứng trước bản mới ở xa; không có entry nằm ngoài radius.
- Slot cần đến sớm hơn thời gian khách có thể tới không được ghi là phù hợp ngay bây giờ.

Nguồn code: [DiscoverServicesUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/DiscoverServicesUseCase.java), [ServiceRepositoryAdapter.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/adapters/ServiceRepositoryAdapter.java).

### 10. Customer Support — P0

API: `POST /api/ai/support`; `GET /api/orders/{id}/cancellation-preview`; `POST /api/orders/{id}/refund-request`.

Hiện tại: Chỉ đọc owned order, phân loại và preview; nextActions là list mã cố định, chưa phản ánh eligibility/state hoặc đi tới yêu cầu hỗ trợ/đổi lịch thực.

Cần cải thiện:

- Trả câu trả lời có căn cứ trạng thái/order/policy và actions có method, target, schema, preconditions và requiresConfirmation; actions được core use case quyết định.
- Nối cancellation preview/refund-request sẵn có (Idempotency-Key), hiển thị amount/status thực; hủy yêu cầu không có nghĩa provider đã hoàn tiền.
- Có tiếp nhận yêu cầu hỗ trợ của khách, requestId, trạng thái và handoff thủ công; không triển khai tính năng AI phân tích khiếu nại Admin đang hoãn.
- Đổi lịch cần core workflow mới: preview slot/price delta/eligibility, khách xác nhận, request có idempotency và quyền; chốt rule thời hạn/approval trước, không dùng model để tự quyết tiền.

Tiêu chí nghiệm thu:

- Đơn của người khác bị chặn trước inference; đơn không đủ điều kiện không có nút thao tác giả.
- Khách hoàn tất flow preview → xác nhận → core request và theo dõi trạng thái; retries không nhân yêu cầu hay hoàn tiền.

Nguồn code: [CustomerSupportUseCase.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/application/usecase/CustomerSupportUseCase.java), [OrderController.java](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java).


## Contract và hạ tầng dùng chung

- Cùng schema criteria cho API và tool: dates/timezone, partySize, budgetBasis, categories/exclusions, location, preferences. Xác minh taxonomy/IDs từ dữ liệu; không mặc định có điều kiện trẻ em/an toàn nếu source không có thuộc tính đó.
- Response public phân biệt AVAILABLE, NEEDS_INPUT, PARTIAL, NO_MATCHES và DEGRADED. RequestId, sources, sourceAt/validUntil, coverage và actionable next step; HTTP conflict/version giữ contract riêng. Quote expiry không có nghĩa inventory đã được giữ.
- Bulk catalog/option/slot reads và xử lý live holds để tránh nhiều query theo từng service/option. Cache dữ liệu public có version; inventory phải revalidate. Context/order private không dùng cache chung sai owner.
- Cache Quyet theo input/model/rubric/locale và scope; giới hạn inference/queue/deadline, xử lý 429 có backoff phù hợp. Review aggregation có thể chạy nền theo version thay vì 20 call đồng bộ mỗi GET. Job API chỉ cần nếu có tác vụ thực sự dài.
- Model adapter không đổi quyền, giá, inventory, safety hoặc financial state. Typed source cards và core validation vẫn bắt buộc ngay cả khi model có score cao.
- SLO latency/concurrency phải chọn trước load test theo quy mô demo/khách thực. Thu metrics p50/p95, errors, fallback/model busy, cache và request deadline; không coi latency một lần inference là SLA.

## API bổ sung đề xuất, không tính thành tính năng AI mới

- Preference của khách: GET/PUT /api/me/travel-preferences, hoặc mở rộng profile API đã có.
- Feedback/impression: POST /api/ai/feedback, gắn request/result IDs và actor thật.
- Replan preview + accept: mở rộng mode của endpoint hiện có hoặc tách POST /api/ai/itineraries/{id}/replan-preview và POST /api/ai/itineraries/{id}/accept-replan. Accept chỉ đổi lịch đề xuất, không đổi booking.
- Support handoff: POST /api/ai/support/requests, hoặc reuse flow tiếp nhận thủ công hiện có. Đây là phục vụ khách, không phát triển Admin Complaint Analysis đang hoãn.
- Core reschedule: preview/request endpoints trong booking/order sau khi chốt policy. Hiện chưa tìm thấy workflow đổi lịch tương ứng trong module booking/order; không giả định đã có.
- GET /api/ai/jobs/{jobId} chỉ nếu cần background tasks có vòng đời dài. Không thêm endpoint không có client/use case rõ ràng.

## Thứ tự triển khai

1. Nền tảng: typed chat cards/actions, persisted context, criteria schema/clarification, canonical retrieval/quotes, status/freshness và idempotency; nối một luồng UI thật trước. Bảo vệ quyền/giá/slot/weather/refund đang có.
2. Discovery: hoàn thiện Smart Search + Nearby + Weather + Comparison. Các thuật toán sau chỉ tốt khi lấy đúng pool dữ liệu; chưa thêm embedding/model khác khi dữ liệu và baseline chưa được đo.
3. Nội dung và lập kế hoạch: Recommendation theo preferences/feedback + Planner nhiều phương án/ngày + Review Summary có claims/source/coverage. Đánh giá model riêng với fixture kỹ thuật.
4. Vòng đời và thao tác: Replan event → preview → accept; Customer Support preview → confirm → native request/handoff; nối các màn hình còn lại và chạy E2E xuyên suốt.

## Quyết định kiến trúc đề xuất

- Giữ Quyet/Groq và các endpoint đang có, thống nhất contract sau adapter. Lợi ích: không đổi hướng model; chi phí: cần schema và dữ liệu nguồn tốt hơn.
- Reuse PostgreSQL search/metadata và read API theo module trước. Lợi ích: tận dụng search_vector đã có, ít dependency; giới hạn: phải đánh giá query/đồng nghĩa và không hứa tìm kiếm semantic tổng quát.
- Tự động phát hiện sự kiện, chỉ tạo proposed replan để khách accept. Lợi ích: phản ứng chủ động mà không sửa booking/tiền; chi phí: thêm indexed itinerary items, event deduplication, revision history và UI diff.
- Không có routing thật thì chỉ đưa transfer estimate có nguồn và uncertainty; không ép thêm nhà cung cấp bản đồ trả phí vào scope. Có thể bổ sung routing adapter sau khi yêu cầu độ chính xác được chốt.

## Cổng nghiệm thu chung

- Functional E2E cho đủ 10 tính năng, Việt/Anh, user A/B, model unavailable/429, forecast thiếu/stale, option thay giá/hết chỗ, request lặp và version conflict.
- Test catalog >50 nguồn, quote package/per-person, phủ định/multi-activity/ngày mơ hồ, multi-day schedule, missing coordinates và sự kiện hủy ảnh hưởng một item.
- Semantic evaluation: labelled queries cho criteria; relevance labels cho top-K (so baseline); aspect/sentiment labels và claim support cho summary. Chốt thresholds trước test, không tune trên tập dùng để báo cáo kết quả.
- Sáu tính năng đang hoãn vẫn ngoài scope. Các dữ liệu service/preferences/workflow cần bổ sung phục vụ 10 tính năng khách hàng, không mở rộng thành Vendor/Admin AI.

Đây là rà soát code và kế hoạch, chưa chạy lại backend test/load test, chưa sửa API hoặc giao diện. Không dùng kết quả test cũ để gọi các nâng cấp đề xuất là đã hoàn thành.

