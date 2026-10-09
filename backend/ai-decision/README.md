# DANASEA Quyet Small decision worker

Worker dùng `chinhnc/Quyet-1.0-Small` cho **text/JSON**, độc lập với Spring Boot. Groq tiếp tục phục vụ hội thoại; Florence không nằm trong runtime. Không có OCR, đọc ảnh hoặc sinh hội thoại bằng Quyet.

## Chạy local trên máy này

Model và Python đã tải ở cache riêng ngoài Git. Khởi động từ thư mục dự án:

```sh
/Users/capkimkhanh/.cache/danasea/quyet-small/.venv/bin/python backend/ai-decision/local.py
```

Worker lắng nghe `127.0.0.1:8091`, nạp model một lần, CPU 4 threads, một process. `local.py` tạo khóa riêng 0600 ở `~/.cache/danasea/quyet-small/service.key`; không ghi khóa vào log. Để bật Spring Boot chạy trên host, dùng cùng khóa qua biến môi trường:

```sh
export AI_QUYET_ENABLED=true
export AI_QUYET_BASE_URL=http://127.0.0.1:8091
export AI_QUYET_API_KEY="$(cat "$HOME/.cache/danasea/quyet-small/service.key")"
cd backend
./mvnw spring-boot:run
```

Dùng cấu hình database/Redis/RabbitMQ sẵn có của backend. Không chạy thêm backend vào cổng 8080 khi container hiện tại vẫn dùng cổng này. Docker backend cần rebuild để nhận code mới.

`GET /health` chỉ trả metadata. `POST /v1/decisions` yêu cầu `Authorization: Bearer ...` và JSON:

```json
{"task":"service_category","state":{"content":"Tour kayak có hướng dẫn viên"}}
```

Các task cố định: `intent`, `service_category`, `review`, `moderation`, `relevance`, `complaint`, `risk`. Client không được gửi `questions`, rubric hoặc tiêu chí tùy ý. Rubric do server quản lý trong `rubrics.py`.

## Model, contract và giới hạn

- Revision: `233167bba5df61b5375a522bf8a042d8d2189379`; rubric `danasea-2026-10-08-v1`.
- Khi startup: kiểm tra hash manifest đã pin rồi hash từng file. Nếu không khớp, không nạp model. Weights và venv không thuộc repository.
- Offline inference; không gửi text lên Hugging Face. HTTP worker dùng credential riêng với Groq.
- Body tối đa 32 KiB, `strict=True`, model giới hạn token riêng. Reject input bị cắt, output thiếu/sai nhãn hoặc xác suất không hợp lệ.
- Một inference đồng thời; trả 429 khi bận. Backend timeout kết nối 1 giây/đọc 3 giây và trả trạng thái unavailable; không retry vô hạn. Sau lỗi HTTP/network, adapter nghỉ 5 giây trước khi gọi lại để tránh mỗi candidate chờ thêm timeout.
- Kết quả luôn là suggestion, không phải tỷ lệ đúng đã đo. Noul là xác suất yes do model tính, không có confidence riêng.
- Quyet Small có thể sai với phủ định, prompt injection và dữ liệu ngoài miền. Không dùng để tự duyệt nội dung, kết luận gian lận, điều chỉnh tiền hoặc vượt quy tắc an toàn thời tiết.

## Docker tùy chọn

Cấu hình `AI_QUYET_API_KEY` và `QUYET_MODEL_DIR` (thư mục có `MANIFEST.sha256`, không phải thư mục cha của revision), rồi:

```sh
docker compose -f docker-compose.yml -f docker-compose.ai.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.ai.yml up --build -d quyet backend
```

Worker không mở cổng ra host; backend truy cập qua mạng Docker. Mount model read-only, chạy user thường. Image cài dependencies trong build; offline chỉ áp dụng lúc inference. Không phân phối weights nếu chưa giữ đầy đủ LICENSE/NOTICE từ bản tải. Compose được kiểm tra cấu hình riêng với thử nghiệm runtime local; build image là một phép kiểm chứng khác.

## Kiểm tra

```sh
cd backend/ai-decision
/Users/capkimkhanh/.cache/danasea/quyet-small/.venv/bin/python -m unittest -v test_service
/Users/capkimkhanh/.cache/danasea/quyet-small/.venv/bin/ruff check .
/Users/capkimkhanh/.cache/danasea/quyet-small/.venv/bin/ruff format --check .
```

`local-smoke-results.json` lưu kết quả 7 task trên worker thật. Đây là smoke có đầu vào minh họa, không phải tập đánh giá chất lượng. Ví dụ câu cảnh báo “Không chuyển tiền ngoài...” đã bị model gắn nhầm external_payment; giữ lỗi này làm bằng chứng cần duyệt thủ công. Kiểm thử Java với `-Dai.local.smoke=true -Dtest=QuyetDecisionAdapterTest` gọi worker thật qua adapter; mặc định suite không phụ thuộc worker local đang chạy.


`smoke_backend.py` chạy JAR đã build với PostgreSQL/Redis Docker tạm, JWT thật và worker đang chạy trên 8091; kiểm tra HTTP rồi xóa các container tạm. Chạy bằng Python trong venv từ root dự án sau `./mvnw -DskipTests package` ở backend. Kết quả: `backend-smoke-results.json`; không gọi Groq hoặc API tài chính.
