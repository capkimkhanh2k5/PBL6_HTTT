# Bằng chứng kiểm tra AI backend — 09/10/2026

Vị trí mã nguồn hiện tại: `/Users/capkimkhanh/Documents/DUT4_1/PBL6`, branch `feat/AI`, base commit `20d33c4` cùng thay đổi chưa commit.

Các kiểm thử dưới đây đã chạy trong worktree `codex/customer-ai-completion` trước khi gộp về `feat/AI` ngày 09/10/2026. SHA-256 của toàn bộ Java/resources/tests tại checkout hiện tại khớp bản đã kiểm thử; không chạy lại full suite chỉ vì chuyển vị trí mã nguồn. Báo cáo JSON giữ nguyên nơi thực thi ban đầu và bổ sung thông tin chuyển về `feat/AI`.

| Kiểm tra | Kết quả hiện tại |
| --- | --- |
| `./mvnw -q -Dai.local.smoke=true clean test` | **1.769** tests được inventory; **1.634 chạy thành công**, **135 skipped**, **0 failures/errors**, exit 0. |
| `./mvnw -q -DskipTests package` | Build JAR thành công trên cùng source Java/resources. |
| PostgreSQL 16 + Redis 7/Testcontainers | Flyway **V25** apply/validate; runtime schema/context và các workflow AI được kiểm tra. |
| Application API inventory | **160 endpoint** trong context; 32 endpoint khách hàng AI, 2 text assessment, 3 admin AI đã có và 3 manual support queue. Mười nhóm tính năng không có quan hệ một-một với số endpoint/tool. |
| Python worker | **7/7** unittest boundary: auth, task/rubric cố định, input sai/NaN/body quá lớn, inference đồng thời trả 429. |
| Python/static/config | Ruff check + format check, Python syntax, shell worker syntax, Git diff check và Compose base + AI overlay config đều pass. Compose worker image chưa được build/chạy trong lần này. |
| Quyet local thật | Test Java adapter chạy **cả 7 task**, kiểm model/revision/rubric và cấu trúc output. |
| JAR + JWT + PG/Redis + Quyet thật | **13/13 nhóm** HTTP smoke thành công; dùng container/data riêng, dừng backend thử và xóa container tạm sau chạy. |
| FE/mobile | Không có thay đổi trong lượt nâng cấp này; UI thật chưa nối. |

Bằng chứng bền vững: [backend-test-results.json](../../ai-decision/reports/integration/backend-test-results.json), [backend-smoke-results.json](../../ai-decision/reports/integration/backend-smoke-results.json). JSON có counts theo suite và SHA-256 source Java/resources/tests. Log tạm: `/tmp/danasea-customer-ai-full-final.log`, `/tmp/danasea-customer-ai-package-final.log`, `/tmp/danasea-customer-ai-smoke-final.log`.

## Kiểm tra sau khi gom thư mục worker

Ngày 09/10/2026, worker và bộ chẩn đoán Quyet đã được gom vào `backend/ai-decision`; thử nghiệm Florence được loại khỏi repository. [Cấu trúc và lệnh chạy mới](../../ai-decision/README.md) tách `app`, `config`, `scripts`, `tests`, `reports` và `docs`.

Sau sắp xếp: 7/7 unittest HTTP boundary pass; worker local thực thi đủ 7 task Quyet thật với model revision/rubric giữ nguyên; shell launcher và CLI chẩn đoán chạy được từ thư mục khác, fixture mặc định tải được và một request chẩn đoán qua schema validation. Ruff check/format, Mypy trên 9 file Python (bỏ qua dependency imports), Bandit medium/high, Python/JSON/shell syntax, Compose config, nguồn COPY Dockerfile và các link tài liệu đều được kiểm tra. Image Docker chưa được build/chạy lại trong lượt sắp xếp này. Kết quả lượt này: [layout-validation.json](../../ai-decision/reports/integration/layout-validation.json).

Các fixtures, rubric, dependency locks và báo cáo đã lưu được chuyển nguyên dữ liệu. SHA-256 Java/resources/tests vẫn khớp bản đã chạy full suite ở trên; không chạy lại full suite Java hoặc 13 nhóm JAR smoke chỉ vì chuyển thư mục worker. Thay đổi người dùng đã stage trước khi sắp xếp được giữ nguyên trong index; phần sắp xếp cần được stage thêm trước khi commit.

## Các hành vi đã được kiểm tra

- Catalog có hơn 65 nguồn không phù hợp vẫn tìm được dịch vụ hợp lệ cũ; Nearby xếp khoảng cách trên toàn tập lọc trước limit; giới hạn 15, pagination và coverage weather bị lọc không biến thành kết luận hết dịch vụ.
- Hard filters giá nhóm/live slots không bị model relevance nới; consent bật/tắt, wishlist/recent views chỉ đọc, feedback gắn đúng actor/recommendation/service và gửi lại idempotent.
- Activity exclusion dựa tên/category, không loại kayak chỉ vì mô tả nhắc “không bao gồm SUP”. Criteria Việt/Anh/ngày/giờ/phủ định/budgetBasis, family query thiếu số người hỏi rõ; invalid date/coordinates trả 400; model unavailable/429 giữ fallback.
- Chat card/source/actions, context tiếp nối, source policy thật và gửi lại message đầu tiên; quyền conversation và locale giữ nguyên. Groq retry không bắt đầu transport khi không còn đủ budget.
- Planner theo ngày/budget/overlap/transfer, alternatives, preview/save/accept/archive/revisions; proposal không ghi đè lịch. Canonical slot events đánh STALE, giữ phần không ảnh hưởng và phần đã bắt đầu, dedup proposal/thông báo. Stale version/foreign owner bị chặn.
- Forecast thiếu/quá hạn không safe; estimated marine là provisional; metadata freshness không bị reset bằng cache hit. API weather không authorize booking.
- Comparison thống nhất partyTotal của package/per-person, unavailable option và source policy chung; rating không được dùng để khẳng định chất lượng cao nhất.
- Review population aggregates riêng với sample, exact quotes và nguồn thật; fingerprint đổi khi review sửa/flag/xóa, cache không giữ nguồn đã ẩn; không có review không sinh claim.
- Support kiểm owner trước model, preview/eligibility thật, confirmed manual request và idempotency, manual ADMIN queue/version, responseNote và thông báo. Ticket RESOLVED không tự sửa booking/payment/refund.
- **Chín integration cases V25** dùng PostgreSQL, native Create/CancelBookingHold và Redis thật: hai yêu cầu đồng thời chỉ một hold; mất cache/restart replay outcome; binding owner/conversation/fingerprint; thẻ CONFIRMED thiếu outcome không tạo lại hold; lỗi cache/serialize rollback DB và giải phóng Redis; ba người trong gói riêng được hiển thị [2,1], tạo hai nhóm không vượt sức chứa/không bật allowSplit; nhóm 50 người được đặt qua 25 gói đã hiển thị, gom cùng số người/gói để không vượt giới hạn số item của native API.
- Moderation/risk đã có tiếp tục chỉ tạo gợi ý/case để người duyệt; rule backend không bị model NO xóa và model không đổi trạng thái publication/tài chính.

## Các nhóm smoke trên bản JAR

1. JWT và phân loại bằng Quyet đã pin.
2. Recommendation có Quyet intent/relevance thật và quote nhóm 300000 VND.
3. Lịch lưu riêng và foreign owner bị 404.
4. Moderation thật, queue admin và quyền resolve.
5. Feedback đúng recommendation/owner và retry trả record cũ.
6. Preference consent lưu PostgreSQL.
7. Nearby/live slots/limit 15.
8. Comparison package 300000 với per-person 540000 cho cùng ba người.
9. Weather rule contract; fixture không cần forecast, bookingAuthorized=false.
10. Không có review không sinh summary.
11. Handoff → manual IN_REVIEW → RESOLVED → khách đọc phản hồi, không đổi tiền.
12. Planner preview/save retry/accept/replan proposal/reject; client không giả được trigger weather.
13. AI reads giữ view_count=7.

Không gọi provider thanh toán, không chạy Groq thật để đánh giá hội thoại, không chạy load test production/cohort thực. Runtime container/UI đang có không được redeploy. Worker Quyet còn chạy host local `127.0.0.1:8091`; backend JAR của smoke đã dừng.

## Giới hạn của bằng chứng

Quyet tiếp tục gắn nhầm câu cảnh báo “Không chuyển tiền ngoài hệ thống DANASEA” thành external_payment/spam trong smoke. Giữ nguyên gợi ý sai đó làm bằng chứng về giới hạn, không biến confidence/Noul thành accuracy.

Passing tests chứng minh các contract và trường hợp đã kiểm tra; chưa chứng minh ranking tốt hơn baseline, semantic search tổng quát, moderation/fraud classifier chính xác hoặc khả năng vision. Cần corpus/cohort thật có nhãn, tách tập đánh giá và đo false positive/false negative/relevance trước khi khẳng định hiệu quả sản phẩm.

Native hold và outcome V25 commit trong một transaction PostgreSQL; Redis vẫn là hệ thống riêng. Rollback thông thường đã kiểm giải phóng holds, nhưng process chết đột ngột trước commit có thể để Redis giữ inventory tới TTL ban đầu. Replay không gia hạn hold; muốn biết booking còn hiệu lực phải đọc trạng thái hiện tại qua API booking/order. Không khẳng định exactly-once cho payment/provider hoặc phục hồi mọi dạng crash.
