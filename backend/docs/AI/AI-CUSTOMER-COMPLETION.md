# Hoàn thiện AI khách hàng — backend DANASEA

Phạm vi 09/10/2026: nâng cấp mười nhóm khách hàng. Không nối, sửa hoặc chuyển UI React/Flutter sang backend thật. Sáu nhóm Vendor/Admin AI đang hoãn giữ nguyên. Quyet Small quyết định có cấu trúc trên text/JSON; Groq hội thoại và hỗ trợ chọn nguồn. Không tích hợp Florence/Jev/vision.

## Chatbot, tool và API

Chatbot là điểm tương tác hội thoại. Groq chọn tool đã đăng ký; tool và REST API gọi cùng application use case. Không phải mỗi endpoint là một tool: endpoints lưu tùy chọn, gửi feedback, chấp nhận lịch và xác nhận nghiệp vụ dành cho actor có quyền cùng bước xác nhận rõ ràng. Backend luôn kiểm giá, chỗ, quyền sở hữu và chính sách. Nội dung hội thoại sinh tự do có `generatedTextVerified=false`; các card chứa dữ kiện backend riêng biệt.

## Mười nhóm được nâng cấp

| Nhóm | API chính | Hành vi backend |
| --- | --- | --- |
| Travel Assistant | `/api/assistant/chat`, conversation confirm/history | Card/sources/actions/requiredInputs; context có cấu trúc theo hội thoại, khóa xử lý và gửi lại theo Idempotency-Key; nguồn policy canonical, timeout/fallback. |
| Smart Search | POST `/api/ai/search` | Resolve ngày ISO/ngày-tháng/hôm nay/ngày mai/ngày kia/thứ/cuối tuần, giờ, số người, ngân sách mỗi người/cả nhóm và hoạt động phủ định; thiếu số người trong câu gia đình hỏi rõ; thuộc tính chưa có nguồn được ghi unsupported. |
| Recommendation | POST `/api/ai/recommendations` | Preferences được đồng ý lưu, wishlist/recent views đọc qua module API, feedback gắn recommendationId và đúng danh sách đã trả. Baseline trước; Quyet chấm tối đa năm ứng viên, không nới điều kiện giá/chỗ. |
| Itinerary Planner | POST `/api/ai/itineraries/preview`, save preview | Tối đa ba phương án, hoạt động theo từng ngày, tổng ngân sách và chuyển tiếp; trạng thái DRAFT/ACCEPTED/STALE/ARCHIVED, version và lịch sử. Lịch chưa giữ chỗ. |
| Weather-Aware | POST `/api/ai/weather` | Provider/thời điểm lấy dữ liệu/thời điểm kiểm/hạn dùng/coverage/metrics/thresholds; missing/stale/ước lượng ghi rõ; tối đa ba slot thay thế được kiểm trong cửa sổ yêu cầu. |
| Re-planning | POST `/api/ai/itineraries/{id}/replan`, accept/reject proposal | Tạo proposal và diff, không ghi đè lịch đang lưu. Job đọc trạng thái slot/alert thật, chống event trùng và gửi một thông báo trong ứng dụng; giữ phần không bị ảnh hưởng. Khách accept với expectedVersion. |
| Service Comparison | POST `/api/ai/services/compare` | Ma trận hai đến bốn dịch vụ trên cùng nhóm/ngày/VND, giá nhóm và option còn chỗ, khác biệt có nguồn; bestFit theo giá rồi khoảng cách, không suy ra chất lượng cao nhất. |
| Review Summary | GET `/api/ai/review-summaries/{serviceId}` | Tổng thể review công khai hợp lệ riêng với mẫu cân bằng tối đa 50; phân bố rating và trích dẫn nguyên văn theo aspect. Fingerprint nguồn kiểm mỗi lần, cache không giữ review đã sửa/flag/xóa. |
| Nearby Discovery | POST `/api/ai/nearby` | Database xếp khoảng cách trên catalog trước khi giới hạn, bán kính thực, kiểm option/availability thật; không dùng pool dịch vụ mới nhất. |
| Customer Support | POST `/api/ai/support`, `/api/ai/support/requests` | Đơn của chính khách, preview và actions dựa eligibility thật; yêu cầu hỗ trợ/đổi lịch có xác nhận, idempotency, thông báo và theo dõi trạng thái; nhân viên xử lý thủ công qua queue. Không tự thực hiện đổi lịch/hủy/hoàn tiền. |

## Giới hạn 15 dịch vụ và coverage

`limit` mặc định và tối đa **15**. PostgreSQL lọc PUBLISHED, hoạt động/category, option ACTIVE, báo giá nhóm, ngày/giờ và geo; bilingual FTS kết hợp đối chiếu tên không dấu. Reader duyệt theo thứ tự phù hợp và kiểm availability gồm Redis holds trước khi chọn tối đa 15.

Reader có ngân sách quét tám giây, tối đa 10.000 ứng viên; query riêng có timeout năm giây. Đây là giới hạn phối hợp, không phải cam kết latency tuyệt đối cho toàn request. `offset`/`nextOffset` chỉ vị trí trong catalog đã xếp hạng; dùng nextOffset trả về, không tự cộng số kết quả sau lọc weather. Catalog/chỗ thay đổi có thể làm thay đổi các trang.

Weather kiểm tối đa 20 slot nhạy thời tiết trong một discovery. Trang bị lọc hoặc coverage chưa đủ trả PARTIAL cùng nextOffset/limitations, không giả thành đã chứng minh catalog không có dịch vụ. Không khẳng định xếp hạng tối ưu toàn hệ thống.

`totalBudget` luôn là tổng cả nhóm, ưu tiên hơn số tiền trong query; PER_PERSON trong query được nhân số người. `budgetBasis`, excludedInterests, useSavedPreferences và offset là các field mới. Ước tính di chuyển đường chim bay 25 km/h + đệm 15 phút, không có routing thật hoặc chi phí di chuyển/bữa ăn đã xác minh.

## API bổ sung và trình tự sử dụng

- GET/PUT `/api/ai/preferences`; PUT cần expectedVersion và enabled rõ ràng. Recommendation dùng `useSavedPreferences=true` cùng consent đang bật.
- POST `/api/ai/recommendations/{recommendationId}/feedback`: serviceId và SHOWN/CLICK/POSITIVE/NEGATIVE; Idempotency-Key bắt buộc, chỉ result của actor trong 24 giờ và dịch vụ thực sự đã trả.
- POST `/api/ai/itineraries/preview` → POST `/api/ai/itineraries/previews/{previewId}/save` với alternativeId + Idempotency-Key → POST `/{id}/accept` với expectedVersion. Preview hết hạn sau 15 phút.
- GET `/api/ai/itineraries/page?page=0&size=20`; POST `/{id}/archive`; GET `/{id}/revisions`.
- POST `/{id}/replan` trả lịch cũ và proposal PENDING. GET `/{id}/proposals`; POST `/{id}/proposals/{proposalId}/accept` hoặc `/reject` với expectedVersion. Client trigger không trở thành event weather được xác thực.
- POST `/api/ai/support/requests/preview` → POST `/api/ai/support/requests` với confirmed=true và Idempotency-Key. Body: orderId, kind=HANDOFF/CHANGE_REQUEST, message, desiredDate hoặc desiredSlotId khi đổi lịch. GET danh sách/`/{id}`, POST `/{id}/cancel` với expectedVersion.
- Queue xử lý thủ công: ADMIN GET `/api/admin/support/requests` và `/{id}`, POST `/{id}/handle` với expectedVersion/status/responseNote. WAITING_REVIEW → IN_REVIEW → RESOLVED/DECLINED; giải quyết ticket không tự thay booking hoặc payment. Đây là hỗ trợ thủ công, không triển khai Admin Complaint Analysis.

Các action hủy/hoàn tiền dùng API order đang có với eligibility/preview, xác nhận và Idempotency-Key của core. Hoàn tiền giữ PENDING đến khi provider/webhook hoàn tất; dấu hiệu rủi ro không được model tự kết luận gian lận.

## Xác nhận booking và phục hồi

Pending card hiển thị option/slot/giá nhóm/số gói/số người và participantsPerPackage cho gói riêng. Ví dụ ba người, tối đa hai người/gói: [2,1], quantity=2. Khách xác nhận card mới gọi native hold với hai nhóm đã hiển thị; không bật allowSplit hoặc vượt sức chứa. Thẻ multi-package cũ thiếu phân nhóm phải tạo thẻ mới để xác nhận.

V25 lưu outcome trong PostgreSQL, khóa theo card và ràng buộc actor/conversation/fingerprint. Native hold và outcome cùng transaction; gửi lại sau khi mất Redis vẫn trả bookingId/holdExpiresAt cũ, không gia hạn hold. DB lock wait tối đa năm giây, transaction tối đa 20 giây. Thẻ cache CONFIRMED thiếu outcome bền vững trả conflict để phục hồi, không tạo hold mới.

Rollback thông thường giải phóng Redis bằng cơ chế core hiện có; process chết trước DB commit có thể để Redis giữ chỗ đến TTL 15 phút ban đầu. Đây không phải cam kết exactly-once cho payment/provider. Các guard giá, option, tồn chỗ và bồi hoàn của core được giữ nguyên.

## Vận hành và bằng chứng

- PostgreSQL migrations V22 context/idempotency chat, V23 lifecycle/index/revisions/proposals, V24 preferences/feedback/support; V25 outcome xác nhận đặt chỗ.
- Quyet revision `233167bba5df61b5375a522bf8a042d8d2189379`; task/rubric fixed server-side. Per-request tối đa sáu decision calls, chỉ bắt đầu khi còn đủ thời gian transport; hết budget dùng fallback. Groq kiểm thời gian trước từng retry.
- Tối đa tám request AI (bao gồm xác nhận booking) đang xử lý trong mỗi JVM, vượt trả 429/Retry-After. Actor rate limit hiện có tiếp tục áp dụng. Metric `danasea.ai.request.duration` không ghi text hội thoại.
- `ai.itinerary.monitoring-enabled`, `ai.itinerary.monitoring-delay-ms=60000` và `app.scheduler.enabled` điều khiển monitor; mỗi lượt tối đa 100 slot được index, chỉ phần tương lai.
- `forecastAt` là lúc backend nhận forecast từ provider, không phải thời điểm model khí tượng được nhà cung cấp phát hành. Redis không được làm mới tuổi dữ liệu bằng thời điểm cache hit.

Kiểm thử cuối: 1.769 tests được inventory, 1.634 chạy thành công, 135 skipped, không failure/error; Python boundary 7/7; JAR/JWT/PG/Redis/Quyet thật 13/13 nhóm. Chi tiết tại [AI-VALIDATION.md](AI-VALIDATION.md). Test kỹ thuật và fixture tổng hợp không chứng minh độ chính xác ngôn ngữ/model, hiệu quả cá nhân hóa, phát hiện gian lận hoặc production load. Cần cohort/nhãn thực và baseline để đánh giá các chỉ số sản phẩm đó.
