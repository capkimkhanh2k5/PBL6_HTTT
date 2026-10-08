# DANASEA — Kiểm tra thanh toán dev/sandbox ngày 06/10/2026

Kiểm tra tại checkout `PBL6` trên nhánh `main`, sử dụng cấu hình sandbox có trong `.env`/`backend/.env`. Không ghi client secret, token hay merchant hash secret vào báo cáo. Các probe tạo order chưa thanh toán; không đăng nhập buyer, thu tiền hay xác nhận hoàn tiền.

> **Đối chiếu lại code ngày 08/10/2026:** nội dung bên dưới là kết quả lịch sử ngày 06/10, trước các sửa capture/refund/webhook. Hiện PayPal đã có `POST /api/payments/paypal/capture`: backend nhận kết quả capture trực tiếp từ PayPal, kiểm tra ID/số tiền/tiền tệ rồi cập nhật payment SUCCESS, order PAID và confirm booking. Callback PayPal hiện xác minh transmission headers theo hợp đồng cổng; webhook HMAC mô phỏng chỉ bật ở profile test và yêu cầu ADMIN. `APP_PAYPAL_WEBHOOK_ID` trong `.env` vẫn trống: có thể kiểm thử luồng thanh toán qua capture API, nhưng chưa nghiệm thu callback sandbox thật. Cần đăng ký URL HTTPS công khai và Webhook ID để kiểm thử kênh callback. Xem trạng thái cập nhật tại [API Tracking](danasea-api-tracking.md).

## Các lỗi đã sửa

- Commit payment ID/key trước khi gọi gateway. Timeout giữ payment PENDING, retry cùng key dùng cùng UUID. Request đồng thời cùng key được tuần tự hóa bằng khóa DB; PayPal nhận `PayPal-Request-Id` bằng UUID payment.
- Chỉ replay payment PENDING. SUCCESS/FAILED/REFUNDED bị từ chối; intent hết hạn được commit FAILED trước khi trả HTTP 409.
- Entry point create-intent cũ trong `OrderPaymentService` ủy quyền cho cùng use case để không giữ hai cách xử lý khác nhau.
- MOMO/SEPAY bị từ chối khi tạo intent; không chuyển ngầm sang PayPal. Giữ enum/schema để tương thích dữ liệu cũ.
- Bỏ token/approval URL mô phỏng khi PayPal OAuth/order lỗi; lỗi provider trả HTTP 502. Nhánh verify PayPal thiếu metadata trả false, `webhook_event` gửi tới API verify là object.
- Docker Compose dev truyền các biến PayPal/VNPay vào backend; mặc định PayPal sandbox base URL. Đã build và khởi động backend dev, health trả `UP`.
- Sửa fixture test QR: luôn đổi một ký tự chữ ký. Việc thay đuôi bằng `ab` trước đây có thể không đổi token nếu chữ ký gốc cũng kết thúc bằng `ab`.

## Kết quả cổng thanh toán

| Cổng | Kết quả probe ngày 06/10/2026 | Trạng thái backend ghi nhận ngày 06/10/2026 (lịch sử) |
|---|---|---|
| PayPal | OAuth HTTP 200; tạo order HTTP 201 có approval link; replay cùng PayPal-Request-Id HTTP 200 và cùng provider order ID. | Chưa. Backend thiếu capture sau buyer approval; APP_PAYPAL_WEBHOOK_ID đang trống; HTTP callback còn dùng HMAC/DTO nội bộ. |
| VNPay | URL ký bằng merchant hiện có mở trang chọn ngân hàng sandbox HTTP 200. | Chưa. Callback hiện là POST JSON/HMAC nội bộ; IPN của VNPay gửi GET query có vnp_SecureHash và cần phản hồi RspCode. Return URL localhost chỉ là bước trình duyệt quay về. |
| MoMo | Không có adapter và cấu hình merchant MoMo thực trong các file dev đang kiểm tra; tạo intent bị từ chối. | Không sử dụng được. |
| SePay | Đã bỏ khỏi quyết định nghiệp vụ; không có adapter, tạo intent bị từ chối. | Không sử dụng được. |

**Tạo được order hoặc mở được checkout chưa chứng minh đã thanh toán thành công.** Không cổng nào được xác nhận chạy trọn luồng sandbox → cập nhật order PAID qua callback provider thật trong lần kiểm tra này.

VNPay cần giữ cookie giữa các redirect. Probe ban đầu không giữ cookie nên bị đưa tới Error.html; kiểm tra lại có cookie mở đúng PaymentMethod.html, cả với UUID có dấu gạch ngang như adapter hiện tại. Không thay đổi định dạng reference chỉ dựa trên trang lỗi đó.

Refund vẫn thuộc phần chưa hoàn thiện: VNPay refund đang mô phỏng; PayPal refund còn fallback success khi provider lỗi; refund request/pending worker cần nối persistence và provider transaction ID gốc. Các sửa ở trên tập trung luồng payment intent, không đóng các P0 refund/owner khác trong báo cáo API.

## Bằng chứng và chạy lại

Validation cuối: `./mvnw test` — **1.518 test, 0 failure, 0 error, 135 skipped**; 1.383 test thực thi thành công. Các ca skipped là bốn nhóm Services E2E scaffolding cũ, không phải test payment mới. `git diff --check`, kiểm tra cú pháp script, `docker compose config --quiet` và build image backend đều thành công. Backend dev đã chạy bản mới, health HTTP 200 / UP.

- `PaymentIntentPersistenceIntegrationTest`: 9 ca dùng PostgreSQL/Flyway thật; mock gateway và booking confirmation. Bao phủ replay, hai request đồng thời, expiry giữ FAILED, ba trạng thái terminal, timeout giữ UUID, HTTP 502 giữ payment và HTTP 409 chặn payment FAILED. Webhook thành công được kiểm tra bằng DTO/HMAC nội bộ.
- `PayPalPaymentAdapterTest`: HTTP mock, không gọi Internet trong unit test; kiểm tra provider idempotency, các nhánh lỗi và payload xác minh webhook.
- `PostgreSqlMigrationIntegrationTest`: áp dụng/validate Flyway V1–V14 trên PostgreSQL 16.
- `BackendApplicationTests`: 111 endpoint trong profile test. OpenAPI của container dev/default có 107 endpoint; 4 endpoint chẩn đoán chỉ bật ở dev/test. Tracking hiện liệt kê đủ cả 111 endpoint, gồm cancellation-preview được bổ sung khi đối chiếu runtime.

Từ thư mục gốc repository:

```sh
python3 backend/scripts/check_payment_sandbox.py
cd backend
./mvnw test
```

Script ưu tiên environment của shell, sau đó `backend/.env`, rồi `.env`. Chỉ cho gửi request tới hostname sandbox chính thức của PayPal/VNPay, không gọi host production; không in credentials hoặc access token. Probe tạo một PayPal order chưa thanh toán và kiểm tra replay; VNPay chỉ mở checkout. Script không kiểm tra capture, giao dịch ngân hàng hay callback hoàn tất.

Tài liệu provider: [PayPal create order](https://developer.paypal.com/sdk/orders/v2/orders-create/), [PayPal capture order](https://developer.paypal.com/api/orders/v2/orders-capture), [PayPal verify webhook](https://developer.paypal.com/api/webhooks/v1/verify-webhook-signature-post), [VNPay payment/IPN](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html).
