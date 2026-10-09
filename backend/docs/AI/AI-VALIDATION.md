# Bằng chứng kiểm tra AI backend (09/10/2026)

Checkout: `/Users/capkimkhanh/Documents/DUT4_1/PBL6`, branch `feat/AI`. Các kết quả dưới đây thuộc code hiện tại trong checkout này, không dùng lại số test hoặc migration từ worktree cũ.

| Kiểm tra | Kết quả |
| --- | --- |
| `./mvnw -q clean test` | Đang chạy lại sau khi thêm V22–V24 và contract customer AI; số cuối cùng được ghi sau khi lệnh hoàn tất. |
| `./mvnw -q -DskipTests package` | Build JAR thành công. |
| PostgreSQL 16/Testcontainers | Flyway chạy đến V24, gồm context/idempotency assistant, lifecycle/revision itinerary và preferences/support. |
| API inventory/context | 140 endpoint trong application context; các test MVC/security cũ tiếp tục pass. |
| Python worker | 7/7 boundary tests: auth, rubric cố định, reject client questions, input/task sai, NaN, body quá lớn, inference đồng thời trả 429. |
| Python style/syntax | Ruff check, Ruff format --check và parse Python source đều pass. |
| Shell/Git/Compose | `sh -n run.sh`, `git diff --check`, `docker compose ... config --quiet` pass. |
| Quyet local thật | 7 task qua HTTP Python và 7 task qua Java adapter với `-Dai.local.smoke=true`; output đủ model/revision/rubric contract. Không chứng minh nhãn đúng. |
| JAR + JWT + PostgreSQL/Redis + Quyet thật | 5/5 nhóm smoke HTTP thành công; dữ liệu Docker tạm đã xóa, backend thử đã dừng. |

Chi tiết suite mới và tổng số: [backend-test-results.json](../ai-decision/backend-test-results.json). Lệnh full suite ghi log tại `/tmp/danasea-ai-final-suite.log`; package tại `/tmp/danasea-ai-package.log`. Các file log tạm không phải artifact bền vững; JSON kết quả và code kiểm thử nằm trong repository.

## Hành vi được kiểm tra

- Ngân sách tổng nhóm và chỗ trống là điều kiện bắt buộc, kể cả khi model chấm relevance cao. Giá PER_PACKAGE dùng số gói; số người không bị coi là số gói.
- Catalog AI giữ nguyên view_count, chỉ trả option có chỗ thực; private inventory dùng từng đơn vị sức chứa hiện có.
- Parser Việt có mẫu ngân sách/ký hiệu tiền, số người, ngày mai, phủ định hoạt động và nhiều hoạt động; tọa độ NaN/null interests bị reject. Nearby kiểm tra bán kính thực.
- Lịch không vượt tổng tiền, không chồng giờ, có buffer chuyển tiếp; forecast thiếu chặn đề xuất thời tiết, estimated marine luôn provisional.
- Lịch riêng chỉ owner đọc/replan; kiểm tra owner trước model/catalog; version stale trả 409. JSON lịch quá khứ vẫn đọc được.
- Support chỉ đọc đơn của actor, không gọi model cho foreign order, không đổi trạng thái tài chính. Tool bỏ qua userId do model tự gửi; cần actor server cho private features.
- Summary chỉ dùng review public chưa flag; rating/count và trích đoạn dựa trên record thật. ID review do Groq bịa bị loại, không có review thì không có claim.
- Backend risk signals không bị model NO xóa; kiểm thử API admin với payment FAILED lưu trong PostgreSQL. Không đổi trạng thái payment/refund/order.
- Case moderation lưu bền; admin mới resolve, không resolve hai lần, không tự authorize publication. Rate limit dùng actor và remote address.
- Card đặt dịch vụ giữ optionId/slotId/quantity/participants riêng; xác nhận mới tạo hold; đổi giá khi tạo hold có compensation và yêu cầu xác nhận lại. Card legacy thiếu option cần refresh.
- Chat retry theo actor + Idempotency-Key không chạy lại tool/hold; replan retry tạo proposal idempotent; feedback chỉ nhận service thuộc recommendation đã trả.
- Catalog fixture kiểm tra dịch vụ hợp lệ cũ hơn 65 nguồn không phù hợp, nearby ngoài nhóm dịch vụ mới, giới hạn 15 và `PARTIAL/nextOffset` khi chưa quét đủ.
- Review cache fingerprint đổi khi review bị flag/sửa/xóa; support request và admin queue dùng version/idempotency, chỉ tạo human-review notice.
- JSON chat có conversationId không hợp lệ trả 400 trước khi gọi chat hoặc rate limiter; error contract Việt/Anh tiếp tục qua source guard.

## Smoke runtime thật

[backend-smoke-results.json](../ai-decision/backend-smoke-results.json) được tạo bằng `smoke_backend.py` với JAR mới, Spring Security/JWT thật, PostgreSQL/Redis riêng và worker local đã nạp model. Nó xác nhận:

1. Token thật gọi API phân loại, nhận model/revision đã pin.
2. Recommendation có intent/relevance Quyet thật và quote 300000 VND cho 3 người, 2 gói × 150000.
3. Lưu lịch trình, foreign owner bị 404, không reserve inventory.
4. Kiểm duyệt Quyet thật → case → admin resolve; customer không đọc được queue.
5. View count giữ nguyên 7 sau các AI reads.

Không gọi Groq để đo chất lượng hội thoại, không gọi provider thanh toán, không thay container/data hiện có. Worker vẫn chạy local ở 127.0.0.1:8091; backend container 8080 từ trước cần rebuild để nhận code mới. Docker worker image chưa build/chạy trong phép kiểm tra này; chỉ Compose config và runtime host đã được kiểm chứng.

## Giới hạn chất lượng model

[local-smoke-results.json](../ai-decision/local-smoke-results.json) ghi latency và output của 7 task minh họa. Case cảnh báo “Không chuyển tiền ngoài hệ thống...” bị gắn nhầm external_payment/spam cả trong worker smoke và pipeline backend. Giữ lỗi này, không coi confidence/Noul là độ chính xác đã đo.

Passing tests chứng minh những contract/hành vi đã kiểm tra, chưa chứng minh recommendation tốt hơn baseline, kiểm duyệt chính xác, fraud classifier hiệu quả hoặc khả năng xử lý ảnh. Muốn tự động hóa cần corpus tiếng Việt có nhãn, phủ định/đối kháng, tách tập đánh giá và đo false positive/false negative theo task. Hiện các quyết định model là gợi ý, nguồn dữ liệu và nghiệp vụ backend vẫn kiểm soát quyền, inventory, giá, thời tiết và tiền.
