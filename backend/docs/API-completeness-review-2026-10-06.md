# ĐÁNH GIÁ TOÀN BỘ API 08/10/2026

## Phần đã có

- IAM: đăng ký, đăng nhập, Google OAuth2, refresh/logout, OTP xác minh email, hồ sơ, đổi mật khẩu, quản trị khóa/mở tài khoản và audit log.
- Vendor/catalog: hồ sơ có thông tin ngân hàng, giấy tờ, duyệt/từ chối vendor, CRUD dịch vụ, submit/approve/reject, pause/resume, ảnh và chứng chỉ an toàn, danh mục, tìm kiếm theo từ khóa/giá/tọa độ, wishlist và gần đây đã xem.
- Booking/order: giữ nhiều slot, xác nhận, hủy hold, lịch sử/detail, đơn tổng và đơn thành phần. API vendor reject đã có dù checklist còn để chưa xong.
- Giao dịch: API payment intent, webhook, cancellation preview, refund request. Mức độ tồn tại API cần phân biệt với những lỗi kết nối được nêu dưới đây.
- Vận hành: QR/check-in, tạo và xử lý tranh chấp, generate/list/detail/finalize settlement.
- Thời tiết/AI: thời tiết khí tượng và biển, kiểm tra an toàn trước booking, rule theo danh mục, giám sát slot, cảnh báo và xử lý admin; AI chat/tool calling, confirmation card, lưu hội thoại, rate limit và nội dung Việt/Anh.
- Thông báo: lưu và đọc thông báo, có hạ tầng gửi email qua hàng đợi. Chat AI khác với nhắn tin customer-vendor.

## API và tính năng cần bổ sung theo ưu tiên

Các đường dẫn dưới đây là hợp đồng đề xuất, chưa tồn tại trừ khi ghi rõ mở rộng API có sẵn. Không cần tách endpoint riêng nếu có thể hoàn thiện hợp đồng hiện tại.

### P1 Quản lý lịch và tồn chỗ -> Đã Xử Lý

Có model/repository slot và engine giữ chỗ, nhưng chưa có API vendor tạo/sửa/đóng/mở slot. `capacityPerSlot` trên service không thay thế lịch bán. Public detail hiện trả `availableSlots: List<String>` theo ngày/giờ, thiếu slotId mà `POST /bookings/hold` bắt buộc nhận; thiếu sức chứa, số chỗ trống và trạng thái cho khách lựa chọn.

- `GET /api/services/{id}/slots?from=&to=&quantity=` trả đối tượng slot gồm slotId, ngày, giờ bắt đầu/kết thúc, chỗ còn, trạng thái và giá nếu áp dụng theo slot.
- `GET /api/vendor/services/{id}/slots`.
- `POST /api/vendor/services/{id}/slots`, có thể hỗ trợ tạo hàng loạt theo lịch lặp.
- `PATCH /api/vendor/services/{id}/slots/{slotId}` để đổi sức chứa/giờ khi chưa ảnh hưởng đơn đã đặt.
- `PATCH .../{slotId}/close` và `PATCH .../{slotId}/open`, hoặc một API đổi trạng thái.

Không giảm capacity thấp hơn booked/held; không xóa slot có giao dịch; chặn thao tác vendor khác. Availability chỉ là dữ liệu tham khảo; hold vẫn phải kiểm tra nguyên tử. Cần test cạnh tranh slot cuối bằng Redis/DB thật.

### P1 Thống kê và báo cáo -> Đã Xử Lý 

> Cập nhật 09/10/2026: đã triển khai và sửa nghiệp vụ báo cáo trên `implement_admin_reports_dashboard`; xem [hợp đồng hiện tại](reports-dashboard-contract.md). Nội dung dưới đây là phát hiện tại thời điểm audit 06/10.

Yêu cầu học phần bắt buộc có báo cáo theo ngày, tuần, quý, năm và khoảng từ ngày đến ngày. `/api/admin/dashboard` chỉ trả `ADMIN_ACCESS_GRANTED`. Listing settlement có bộ lọc ngày là chức năng đối soát, chưa thay thế báo cáo kinh doanh/phân tích.

- Thay nội dung `GET /api/admin/dashboard` bằng số liệu thật.
- `GET /api/admin/reports/revenue?from=&to=&groupBy=day|week|quarter|year`.
- `GET /api/admin/reports/bookings?from=&to=&groupBy=`: số đơn, hoàn thành, hủy, lý do hủy.
- `GET /api/admin/reports/vendors?from=&to=`: doanh thu, đơn, hoàn/hủy, tỷ lệ sử dụng chỗ và chất lượng theo vendor.
- `GET /api/vendor/dashboard`, `GET /api/vendor/reports/revenue` với cùng quy ước ngày và isolation theo vendor.
- `GET /api/admin/reports/export?type=&from=&to=&format=csv`; phiên bản vendor tương ứng. CSV đủ cho MVP nếu chưa cần XLSX.

Phân biệt giá trị bán, tiền thu, hoàn tiền, hoa hồng và số thực nhận; thống nhất trạng thái được tính, timezone và các ngày biên. Phần phân tích nên giúp chọn thời gian/dịch vụ/vendor cần cải thiện, không chỉ cộng tổng.

### P1 Đánh giá và chất lượng dịch vụ — Đã Xử Lý

Cập nhật 08/10/2026 trên nhánh `implement_review_rating_system`:
- `POST /api/sub-orders/{id}/reviews`: chỉ chủ đơn COMPLETED; unique constraint theo sub-order; ảnh tối đa 5 HTTP(S) URL, mỗi URL tối đa 2048 ký tự.
- `PUT /api/sub-orders/{id}/reviews` và `PUT /api/reviews/{id}`: chỉ tác giả, sửa trong 7 ngày. Nhánh sub-order lấy scalar ID trước khi khóa để không lưu lại snapshot cũ.
- `GET /api/services/{id}/reviews`: phân trang, chỉ review visible; không trả order/customer ID, flag reason hoặc moderation note.
- `GET /api/vendor/reviews`, `POST /api/vendor/reviews/{id}/reply`: giới hạn theo vendor hiện tại.
- `GET /api/admin/reviews`, `PATCH /api/admin/reviews/{id}/visibility`: chỉ ADMIN; lọc vendor/service/flag/visibility; ghi chú kiểm duyệt tách khỏi lý do báo cáo.
- `POST /api/reviews/{id}/flag`: yêu cầu đăng nhập và review visible; trả xác nhận id/isFlagged, không trả nội dung review.

Các thao tác sửa/reply/flag/ẩn/hiện khóa review; tổng điểm được bảo vệ bằng khóa vendor, service. Tạo review giữ khóa vendor trước kiểm tra trùng và insert. Rating/count service/vendor và badge suy ra từ review visible được cập nhật trong cùng transaction. Hiện chưa có API xóa; admin ẩn/hiện và khách sửa theo chính sách trên.

Migration `V21__review_enhancements.sql` bổ sung visibility/moderation, unique constraint, rating constraint và TEXT cho JSON ảnh. V18/V19 dành cho slot, V20 dành cho password reset; nhánh review không giữ migration V18 trùng số. Không tự xóa dữ liệu trùng khi áp dụng unique constraint.

Bằng chứng kiểm thử: `ReviewUseCaseTest`, `ReviewControllerTest`, `ReviewRatingIntegrationTest` và `ReviewConcurrencyIntegrationTest` với PostgreSQL 16 thật (11 ca kiểm tra cạnh tranh, privacy, ảnh, ownership và badge). Validation dùng bundle Anh/Việt; inventory của nhánh là 124 endpoint.

### P1 Chi trả vendor và cấu hình hoa hồng

Settlement đã có generate/list/detail/finalize. Finalize chỉ chốt kỳ, chưa đánh dấu PAID; PayoutRequest mới có model/entity/repository. Chưa có luồng rút tiền và xác nhận chi trả. `CommissionPolicyAdapter` hiện trả mặc định 10%, chưa đọc cấu hình.

- `POST /api/vendor/payout-requests`, `GET /api/vendor/payout-requests`.
- `GET /api/admin/payout-requests`; approve/reject và xác nhận paid, hoặc luồng admin trực tiếp chi trả settlement nếu muốn thu gọn MVP.
- `PATCH /api/admin/settlements/{id}/mark-paid` kèm tham chiếu giao dịch/chứng từ, nếu chọn cách chi trả thủ công mô phỏng.
- API cấu hình commission theo category/vendor/gói; lưu tỷ lệ tại lúc tạo sub-order để thay đổi sau không sửa giao dịch cũ.

Đảm bảo một settlement không chi trả hai lần, không rút quá số được nhận, lưu audit log. Thông tin ngân hàng riêng tư chỉ dùng cho vendor/admin có quyền.

### P1 Cam kết an toàn trước giao dịch

Vendor có thể nhập waiver content, sub-order có waiverAccepted/waiverAcceptedAt; public detail chưa trả nội dung waiver và order request chỉ nhận bookingId. Sub-order được tạo với waiverAccepted=false; chưa thấy luồng xác nhận hay guard yêu cầu cam kết.

Mở rộng public detail trả nội dung điều kiện tham gia/waiver theo ngôn ngữ và version. Mở rộng request tạo order/checkout nhận xác nhận từng service rủi ro; lưu user, thời điểm, version/nội dung snapshot. Có thể dùng endpoint `POST /api/sub-orders/{id}/waiver-acceptance` nếu phù hợp thứ tự checkout. Không cần thêm cả hai cách.

### P1 Quên mật khẩu — Đã Xử Lý

Cập nhật 08/10/2026:
- `POST /api/auth/forgot-password`: Nhận email, chống User Enumeration bằng phản hồi đồng nhất; tạo mã OTP 6 chữ số băm SHA-256 lưu trong `password_reset_tokens` (hết hạn sau 15 phút, vô hiệu hóa các mã cũ của user); phát sự kiện `PasswordResetRequestedEvent` qua RabbitMQ topic exchange `danasea.exchange.topic` tới queue `notification.password-reset-email.queue` để gửi email bất đồng bộ; áp dụng Rate Limit (5 requests/phút theo IP qua Bucket4j/Redis).
- `POST /api/auth/reset-password`: Nhận email, OTP 6 chữ số và mật khẩu mới; kiểm tra active token và giới hạn tối đa 5 lần thử sai bằng `failed_attempts` trong PostgreSQL; lần sai thứ 5 commit trạng thái vô hiệu hóa dù API trả lỗi. Cấp mã, reset, login và refresh dùng khóa bi quan chung trên tài khoản, bảo đảm OTP dùng một lần khi request đồng thời. Reset cập nhật BCrypt password hash, đánh dấu `usedAt`, tăng `users.session_version`, thu hồi mọi refresh token và ghi audit log `PASSWORD_RESET`. JWT có claim `session_version`; filter đọc tài khoản không qua cache và từ chối access token có phiên bản cũ.
- Migration `V20__password_reset_security.sql` bổ sung trạng thái bảo mật với giá trị mặc định cho dữ liệu cũ; token reset mới để Hibernate tự sinh UUID. V18/V19 được giữ cho nhánh quản lý slot đang triển khai riêng. JWT cũ chưa có claim version được coi là phiên bản 0 và bị vô hiệu hóa sau lần reset đầu tiên.
- Đã bổ sung bộ test kiểm chứng: `ForgotPasswordUseCaseTest`, `ResetPasswordUseCaseTest`, `PasswordResetEmailConsumerTest`, `AuthenticationControllerTest`; `PasswordResetSecurityIntegrationTest` dùng PostgreSQL 16/Redis thật để kiểm tra migration/schema, HTTP response đồng nhất kể cả publisher lỗi, giới hạn OTP, hết hạn, cấp mã/reset đồng thời, thu hồi access/refresh token, race login/refresh với reset, cache cũ và rollback; cập nhật kiểm kê `BackendApplicationTests` (117 endpoints).

Bằng chứng: `security/authentication/presentation/AuthenticationController.java`, `ForgotPasswordUseCase.java`, `ResetPasswordUseCase.java`, `PasswordResetEmailConsumer.java`, `configs/RabbitMQConfig.java`.

### P1 Quản trị giao dịch và theo dõi hoàn tiền ->

**Kiểm chứng 09/10/2026:** `./mvnw clean verify` → BUILD SUCCESS; 1.711 test cases, 1576 thực chạy, 135 skipped, 0 failures, 0 errors. Trong đó 29 ca `AdminTransactionIntegrationTest` trên PostgreSQL 16/Flyway/Redis 7 riêng và 57 ca tập trung reconciliation/adapter/job. Kiểm kê runtime: 126 method/path; migration mới V23 đã được kiểm tra trên DB mới và nâng từ V20. Gateway được mock; sandbox thật chưa được chạy trong lần này.

Cập nhật 09/10/2026 trên nhánh `admin_transaction_refund_management`:

- Admin: `GET /api/admin/orders`, `/payments`, `/refunds` và detail payment/refund. Có phân trang, bộ lọc trạng thái/provider/vendor/customer/order/thời gian; bộ lọc động chạy trên PostgreSQL với cận ngày rỗng, một cận hoặc đủ hai cận. Ngày sai thứ tự trả 400.
- Customer/admin: `GET /api/orders/{id}/payments` và `/refunds`; kiểm tra owner hoặc quyền ADMIN.
- Job tra cứu VNPay querydr/PayPal order-capture, không gửi lại capture/refund. Kiểm tra checksum, reference và loại giao dịch VNPay; kiểm tra transaction ID, provider amount/currency với intent. Kết quả query được áp dụng trong transaction có khóa payment/master order, đọc lại trạng thái sau external request.
- Timeout/UNKNOWN/local URL hết hạn không kết luận chưa thu tiền. Các trạng thái VNPay đảo/nghi ngờ hoặc liên quan refund giữ chờ xác minh. Lỗi xác nhận booking hoặc audit rollback kết quả và lưu marker cần review; không tự xác nhận hold hết hạn hay vượt tồn.
- Batch tối đa 50, claim và backoff 30–300 giây, tránh query trùng capture và tránh để payment chưa rõ chiếm batch đầu mãi. Query trước approval không chặn capture đầu tiên; marker capture timeout legacy vẫn chặn gửi lại lệnh.
- Audit kết quả và phục hồi capture được ghi cùng transaction, metadata JSON lưu TEXT. Chi tiết payment/refund trả lỗi và lịch/số lần xác minh để admin theo dõi.
- Đồng bộ `paymentStatus`: PAID sau thanh toán, REFUNDED khi hoàn toàn bộ, giữ PAID nếu hoàn một phần, NO_REFUND khi hủy không hoàn. Migration V23 bổ sung lịch đối soát và backfill paymentStatus từ dữ liệu payment cũ.

Bằng chứng mã nguồn: `OrderPaymentService`, `RefundProcessingService`, `AdminTransactionSpecifications`, các controller/repository order-payment-refund, `PaymentReconciliationJob`, `VNPayPaymentAdapter`, API audit đồng bộ và migration V23.

Kiểm thử: `AdminTransactionIntegrationTest` chạy HTTP/RBAC/owner và transaction/concurrency/migration trên PostgreSQL thật; `OrderPaymentReconciliationTest`, `PaymentReconciliationJobTest`, `VNPayPaymentAdapterTest` kiểm tra các nhánh gateway, signed response và lịch quét. Bộ test capture/refund/booking hiện có được chạy hồi quy.

Phạm vi này là backend được kiểm chứng với gateway mô phỏng. Giao dịch capture/refund/webhook trên sandbox thật vẫn cần nghiệm thu riêng. Với giao dịch đã thu tiền nhưng booking không thể xác nhận, hệ thống giữ dấu hiệu cần review để xử lý vận hành; không tự bỏ qua guard tồn hoặc gửi một lệnh thu/hoàn tiền khác.

### P1/P2 Đổi lịch do thời tiết và lý do vận hành

Chưa thấy API chuyển booking/sub-order sang slot mới. Action DISMISSED ở weather alert chỉ sửa evaluation; thông báo nói rescheduled nhưng không đổi slot. Vendor reject đã tồn tại nhưng chỉ chấp nhận sub-order CONFIRMED, khác mô tả checklist từ chối trước thanh toán. Cần chốt và cập nhật tài liệu theo nghiệp vụ thực.

- `GET /api/sub-orders/{id}/reschedule-options` hoặc dùng API slots đã đề xuất.
- `POST /api/sub-orders/{id}/reschedule` để giữ slot mới, xử lý chênh lệch giá, nhả slot cũ và audit/notify nguyên tử.
- Phương án thay thế khác vendor nên do khách chọn lại; không tự chuyển booking.
- Khi cache thời tiết hết hạn và API ngoài lỗi, public weather usecase chưa có fallback dữ liệu cũ được gắn nhãn. Bổ sung fetchedAt/source/stale/mức đủ dữ liệu; không biến thiếu dữ liệu thành chứng nhận an toàn.

Mức P1 nếu giữ cam kết đổi lịch trong đề tài; có thể tạm giảm phạm vi sang hủy/hoàn minh bạch nếu được chấp nhận.

### P2 Khuyến mãi

Có DiscountCode/DiscountRedemption nhưng chưa có API quản lý, kiểm tra/apply voucher; tạo order đặt discount bằng 0.

- `GET/POST/PATCH /api/admin/discount-codes`; phiên bản vendor nếu vendor có quyền cấp mã.
- `POST /api/checkout/discount-preview` và mở rộng tạo order nhận code.
- Kiểm tra thời gian, quota, tối thiểu, phạm vi vendor/service, giới hạn mỗi user; phân bổ discount vào sub-order để refund/settlement không sai. PromotionalPrice riêng chưa thay thế voucher.

### P2 Nhắn tin customer-vendor và theo dõi tranh chấp

Conversation/Message của communication mới có persistence, chưa có API nhắn tin. Hội thoại AI là module riêng.

- `POST/GET /api/conversations`, `GET/POST /api/conversations/{id}/messages`; read receipt nếu cần. REST polling đủ cho MVP, chưa bắt buộc WebSocket.
- Customer: `GET /api/disputes`, `GET /api/disputes/{id}` hoặc gộp danh sách vào order detail.
- Vendor: `GET /api/vendor/disputes`, gửi phản hồi/bằng chứng cho dispute liên quan.
- Admin detail dispute nếu nội dung/bằng chứng không đủ trong listing. Upload bằng chứng nên giới hạn loại/kích thước và quyền.

### P2 Hoàn thiện khám phá, hồ sơ công khai và thông báo

- Mở rộng `GET /api/services` nhận ngày/giờ, số người, vendor, minimum rating và sort theo giá/điểm/phổ biến; những tiêu chí này chưa được controller nhận.
- Mở rộng public service detail trả vendorId/tên công khai/badge, thời lượng, sức chứa, điều kiện tham gia, chính sách và structured slots. Tách public vendor DTO khỏi DTO hồ sơ chứa ngân hàng.
- `GET /api/vendors/{id}`, `GET /api/vendors/{id}/services` nếu có trang cửa hàng công khai. So sánh có thể làm ở client từ các detail; không bắt buộc API compare riêng.
- `PATCH /api/notifications/{id}/read`, mark-all-read và unread count. Trạng thái SENT không phải đã đọc.
- Nhắc chuyến sắp tới, thông báo payment/refund hoàn tất cần listener/job thực; hiện không thấy scheduled job nhắc chuyến. Push mobile chỉ ưu tiên khi demo có nhu cầu, không bắt buộc triển khai đồng thời SMS/Telegram/email.
- Biên nhận: `GET /api/orders/{id}/receipt` và phát hành sau thanh toán, kiểm tra owner. Invoice hiện mới có persistence; biên nhận PDF cho đồ án không tự chứng minh hệ thống hóa đơn điện tử pháp lý.

### P2/P3 Hoàn thiện vận hành vendor và tài liệu

Đề tài nêu đình chỉ/tái xác minh vendor; admin vendor hiện chỉ list/detail/approve/reject. Khóa user đã có nhưng chưa thay thế rõ ràng trạng thái kinh doanh và việc ẩn dịch vụ khi đình chỉ. Có thể bổ sung suspend/reactivate/resubmit và vòng đời giấy tờ nếu giữ phạm vi này.

Checklist `danasea-api-tracking.md` hiện ghi nhiều API weather/AI/check-in/dispute/settlement chưa xong dù code đã có; ngược lại thiếu module promotions/messaging/reporting/payout. Workflow refund mô tả persist/cancel/release slot nhưng đường HTTP hiện tại chưa làm các bước đó. Cần cập nhật cả hợp đồng API, workflow và tiêu chí nghiệm thu từ code cuối cùng.

Giỏ hàng không bắt buộc cần CRUD backend riêng: yêu cầu học phần cho phép có hoặc không tùy ứng dụng. Client cart cộng với hold nhiều item có thể đáp ứng MVP; chỉ cần API cart nếu muốn lưu bền vững/đồng bộ nhiều thiết bị. Ba cổng thanh toán cũng không cần hoàn thiện đồng thời nếu một cổng nội địa và một cổng quốc tế đáp ứng phạm vi đã chốt.

## Phụ lục kiểm kê toàn bộ API

### Tài khoản cá nhân (3 method/path)

- **GET `/api/users/me`** — Xem hồ sơ người đang đăng nhập. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/account/presentation/UserController.java:27).
- **PATCH `/api/users/me`** — Cập nhật tên, avatar và ngôn ngữ hồ sơ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/account/presentation/UserController.java:36).
- **POST `/api/users/me/change-password`** — Đổi mật khẩu khi biết mật khẩu cũ; không thay thế forgot/reset password. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/account/presentation/UserController.java:52).

### Quản trị user, vendor và audit (10 method/path)

- **GET `/api/admin/audit-logs`** — Phân trang nhật ký thao tác; còn có thể mở rộng lọc actor/action/time. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminAuditLogController.java:31).
- **GET `/api/admin/audit-logs/{id}`** — Xem chi tiết nhật ký; đã có dù checklist nói không cần detail riêng. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminAuditLogController.java:44).
- **GET `/api/admin/users`** — Tìm và lọc người dùng theo role, trạng thái khóa. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminUserController.java:47).
- **GET `/api/admin/users/{id}`** — Xem chi tiết user cho admin. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminUserController.java:69).
- **PATCH `/api/admin/users/{id}/lock`** — Khóa tài khoản. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminUserController.java:79).
- **PATCH `/api/admin/users/{id}/unlock`** — Mở khóa tài khoản. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminUserController.java:92).
- **GET `/api/admin/vendors`** — Danh sách vendor theo trạng thái xác minh. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminVendorController.java:41).
- **GET `/api/admin/vendors/{id}`** — Chi tiết hồ sơ vendor để kiểm duyệt. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminVendorController.java:59).
- **PATCH `/api/admin/vendors/{id}/approve`** — Duyệt hồ sơ vendor. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminVendorController.java:64).
- **PATCH `/api/admin/vendors/{id}/reject`** — Từ chối hồ sơ vendor và ghi lý do. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/admin/presentation/AdminVendorController.java:72).

### Trợ lý AI (4 method/path)

- **POST `/api/assistant/chat`** — Chat với AI có tool calling/history/rate limit; request Map cần schema validation rõ hơn. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/presentation/controllers/AssistantController.java:47).
- **POST `/api/assistant/conversations/{id}/confirm`** — Tạo hold từ confirmation card; cần ràng buộc owner/conversation/card. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/presentation/controllers/AssistantController.java:103).
- **GET `/api/assistant/conversations/{id}`** — Đọc hội thoại; cần sửa kiểm tra chủ sở hữu. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/presentation/controllers/AssistantController.java:123).
- **GET `/api/assistant/conversations/{id}/history`** — Đọc lịch sử; cần kiểm tra chủ sở hữu và giới hạn limit ở HTTP. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/ai/presentation/controllers/AssistantController.java:131).

### Booking (8 method/path)

- **GET `/api/bookings/{id}`** — Chi tiết booking theo quyền user/admin. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/booking/presentation/controllers/BookingController.java:49).
- **GET `/api/bookings`** — Danh sách booking của customer; lọc status/phân trang/sort. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/booking/presentation/controllers/BookingController.java:90).
- **PATCH `/api/bookings/{id}/cancel`** — Hủy booking và tính quyền hoàn; cần nối worker thực thi refund. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/booking/presentation/controllers/BookingController.java:134).
- **POST `/api/bookings/hold`** — Giữ nhiều slot cho customer/admin; public availability còn thiếu slotId cho client. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/booking/presentation/controllers/BookingHoldController.java:41).
- **POST `/api/bookings/{holdId}/confirm`** — Xác nhận booking theo điều kiện thanh toán; cần test end-to-end với webhook. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/booking/presentation/controllers/BookingHoldController.java:59).
- **DELETE `/api/bookings/hold/{holdId}`** — Hủy giữ chỗ và giải phóng hold. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/booking/presentation/controllers/BookingHoldController.java:71).
- **PATCH `/api/vendor/bookings/{id}/reject`** — Từ chối booking item CONFIRMED; id là bookingItemId, không phải master bookingId. Cần nối refund và thống nhất tài liệu. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/booking/presentation/controllers/VendorBookingController.java:41).
- **GET `/api/vendor/bookings`** — Danh sách booking item của vendor; cần bảo đảm dữ liệu đủ cho màn hình khách tham gia. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/booking/presentation/controllers/VendorBookingController.java:55).

### QR check-in (2 method/path)

- **POST `/api/bookings/{id}/qr-code`** — Sinh QR có thời hạn cho check-in; cần chuẩn hóa nghĩa id booking/sub-order trong contract. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/checkin/presentation/controllers/CustomerBookingQrController.java:24).
- **POST `/api/vendor/checkin/verify`** — Xác minh QR và cập nhật thực hiện; cần test chống replay với DB thật. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/checkin/presentation/controllers/VendorCheckinController.java:24).

### Thông báo (1 method/path)

- **GET `/api/notifications`** — Phân trang thông báo của user; chưa có read/unread API. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/communication/presentation/controllers/NotificationController.java:27).

### Tranh chấp (3 method/path)

- **GET `/api/admin/disputes`** — Danh sách dispute theo status/reason; chưa thay thế theo dõi dispute cho customer/vendor. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/dispute/presentation/controllers/AdminDisputeController.java:32).
- **PATCH `/api/admin/disputes/{id}/resolve`** — Admin xử lý dispute; refund PENDING cần luồng thực thi. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/dispute/presentation/controllers/AdminDisputeController.java:43).
- **POST `/api/orders/{id}/disputes`** — Customer tạo dispute gắn sub-order; cần API theo dõi/bổ sung phản hồi. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/dispute/presentation/controllers/CustomerDisputeController.java:24).

### Order và payment (20 method/path)

- **GET `/api/admin/orders`** — Danh sách đơn hàng toàn sàn cho Admin, phân trang và lọc đa tiêu chí (status, paymentStatus, customerId, vendorId, khoảng thời gian). [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/AdminOrderController.java:31).
- **GET `/api/admin/payments`** — Danh sách giao dịch thanh toán toàn sàn cho Admin, lọc status, provider, orderId, vendorId, customerId, thời gian. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/AdminPaymentController.java:38).
- **GET `/api/admin/payments/{id}`** — Chi tiết giao dịch thanh toán cho Admin. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/AdminPaymentController.java:68).
- **GET `/api/admin/refunds`** — Danh sách yêu cầu hoàn tiền toàn sàn cho Admin, lọc status, reason, subOrderId, orderId, provider, vendorId, customerId, thời gian. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/AdminRefundController.java:38).
- **GET `/api/admin/refunds/{id}`** — Chi tiết yêu cầu hoàn tiền cho Admin. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/AdminRefundController.java:71).
- **POST `/api/orders`** — Tạo master/sub-orders từ booking; nhánh trả order cũ kiểm tra owner. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:77).
- **GET `/api/orders`** — Danh sách đơn customer; chưa có các bộ lọc kinh doanh/time/status. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:89).
- **GET `/api/orders/{id}`** — Chi tiết order của owner hoặc admin. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:108).
- **GET `/api/orders/{id}/payments`** — Lịch sử giao dịch thanh toán theo đơn hàng, kiểm tra quyền owner hoặc admin. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:115).
- **GET `/api/orders/{id}/refunds`** — Lịch sử hoàn tiền theo đơn hàng, kiểm tra quyền owner hoặc admin. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:124).
- **POST `/api/orders/{id}/refund-request`** — Yêu cầu hoàn; hiện cần sửa persist/idempotency/providerTransactionId. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:133).
- **GET `/api/orders/{id}/cancellation-preview`** — Tính mức hoàn trước hủy; cần đồng nhất với lệnh hủy/refund. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:162).
- **POST `/api/payments/{orderId}/create-intent`** — Tạo intent; cần lưu payment và idempotency để webhook dùng được. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/PaymentController.java:60).
- **POST `/api/payments/webhook/vnpay`** — Nhận webhook vnpay; raw route dùng HMAC/payload nội bộ, cần kết nối đúng callback của cổng. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/PaymentController.java:85).
- **POST `/api/payments/webhook/momo`** — Nhận webhook momo; raw route dùng HMAC/payload nội bộ, cần kết nối đúng callback của cổng. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/PaymentController.java:92).
- **POST `/api/payments/webhook/paypal`** — Nhận webhook paypal; raw route dùng HMAC/payload nội bộ, cần kết nối đúng callback của cổng. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/PaymentController.java:99).
- **POST `/api/payments/webhook/vnpay/refund`** — Nhận kết quả refund vnpay; cần refund persisted và hợp đồng callback thật. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/PaymentController.java:108).
- **POST `/api/payments/webhook/momo/refund`** — Nhận kết quả refund momo; cần refund persisted và hợp đồng callback thật. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/PaymentController.java:115).
- **POST `/api/payments/webhook/paypal/refund`** — Nhận kết quả refund paypal; cần refund persisted và hợp đồng callback thật. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/PaymentController.java:122).
- **GET `/api/vendor/orders`** — Danh sách sub-order của vendor; chưa có detail/filter theo khách/ngày/status. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/VendorOrderController.java:43).

### Catalog, dịch vụ, ảnh và an toàn (31 method/path)

- **GET `/api/services`** — Tìm dịch vụ công khai theo keyword/category/giá/tọa độ; thiếu date/quantity/vendor/rating/sort. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/CatalogController.java:29).
- **GET `/api/v1/catalog`** — Alias của /api/services. Tìm dịch vụ công khai theo keyword/category/giá/tọa độ; thiếu date/quantity/vendor/rating/sort. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/CatalogController.java:29).
- **GET `/api/services/{id}`** — Chi tiết public, ảnh, ngày/giờ khả dụng; thiếu structured slots/vendor/waiver và dữ liệu tham gia. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/CatalogController.java:74).
- **GET `/api/v1/catalog/{id}`** — Alias của /api/services/{id}. Chi tiết public, ảnh, ngày/giờ khả dụng; thiếu structured slots/vendor/waiver và dữ liệu tham gia. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/CatalogController.java:74).
- **GET `/api/recently-viewed`** — Đọc dịch vụ gần đây theo user/session. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/RecentlyViewedController.java:22).
- **POST `/api/wishlists/{serviceId}`** — Lưu dịch vụ yêu thích. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/WishlistController.java:23).
- **DELETE `/api/wishlists/{serviceId}`** — Bỏ lưu dịch vụ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/WishlistController.java:32).
- **GET `/api/wishlists`** — Xem danh sách yêu thích. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/WishlistController.java:41).
- **GET `/api/admin/categories`** — Cây danh mục cho admin, gồm inactive. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminCategoryController.java:41).
- **POST `/api/admin/categories`** — Tạo danh mục. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminCategoryController.java:46).
- **PATCH `/api/admin/categories/{id}`** — Cập nhật danh mục và quan hệ cha/con. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminCategoryController.java:58).
- **PATCH `/api/admin/categories/{id}/deactivate`** — Ngừng danh mục; còn thiếu thao tác reactivate nếu muốn phục hồi. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminCategoryController.java:73).
- **GET `/api/admin/services/{serviceId}/safety-documents`** — Admin đọc giấy tờ an toàn của service. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminSafetyDocumentController.java:30).
- **PATCH `/api/admin/services/{serviceId}/safety-documents/{docId}/approve`** — Duyệt giấy tờ an toàn. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminSafetyDocumentController.java:42).
- **PATCH `/api/admin/services/{serviceId}/safety-documents/{docId}/reject`** — Từ chối giấy tờ an toàn. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminSafetyDocumentController.java:58).
- **GET `/api/admin/services`** — Danh sách service theo status; hiện trả List, nên phân trang/filter khi cần. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminServiceController.java:40).
- **PATCH `/api/admin/services/{id}/approve`** — Duyệt xuất bản service với safety guard. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminServiceController.java:46).
- **PATCH `/api/admin/services/{id}/reject`** — Từ chối service và lý do. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/AdminServiceController.java:55).
- **GET `/api/categories`** — Cây danh mục đang hoạt động cho public. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/CategoryController.java:20).
- **POST `/api/vendor/services/{serviceId}/safety-documents`** — Vendor tải giấy tờ an toàn; chưa có vendor list/status API riêng cho các giấy tờ này. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorSafetyDocumentController.java:30).
- **POST `/api/vendor/services`** — Vendor tạo service DRAFT; không tạo lịch/slot bán. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceController.java:54).
- **GET `/api/vendor/services`** — Danh sách service của vendor; hiện trả List. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceController.java:81).
- **GET `/api/vendor/services/{id}`** — Chi tiết service thuộc vendor. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceController.java:88).
- **PATCH `/api/vendor/services/{id}`** — Sửa nội dung, giá, sức chứa mặc định và thuộc tính an toàn. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceController.java:96).
- **POST `/api/vendor/services/{id}/submit`** — Gửi dịch vụ kiểm duyệt. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceController.java:112).
- **PATCH `/api/vendor/services/{id}/pause`** — Tạm ngừng dịch vụ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceController.java:120).
- **PATCH `/api/vendor/services/{id}/resume`** — Mở lại dịch vụ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceController.java:128).
- **DELETE `/api/vendor/services/{id}`** — Xóa dịch vụ theo điều kiện nghiệp vụ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceController.java:136).
- **POST `/api/vendor/services/{serviceId}/images`** — Tải ảnh dịch vụ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceImageController.java:37).
- **DELETE `/api/vendor/services/{serviceId}/images/{imageId}`** — Xóa ảnh dịch vụ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceImageController.java:50).
- **PATCH `/api/vendor/services/{serviceId}/images/reorder`** — Sắp xếp ảnh. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/service/presentation/controllers/VendorServiceImageController.java:64).

### Đối soát (6 method/path)

- **POST `/api/admin/settlements/generate`** — Sinh kỳ đối soát cho vendor trong khoảng ngày. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/settlement/presentation/controllers/AdminSettlementController.java:54).
- **GET `/api/admin/settlements`** — Danh sách kỳ theo vendor/status/from/to. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/settlement/presentation/controllers/AdminSettlementController.java:64).
- **GET `/api/admin/settlements/{id}`** — Chi tiết kỳ và các mục đối soát. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/settlement/presentation/controllers/AdminSettlementController.java:76).
- **PATCH `/api/admin/settlements/{id}/finalize`** — Chốt kỳ FINALIZED; chưa phải chi trả PAID. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/settlement/presentation/controllers/AdminSettlementController.java:82).
- **GET `/api/vendor/settlements`** — Danh sách kỳ của vendor, lọc status/from/to. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/settlement/presentation/controllers/VendorSettlementController.java:49).
- **GET `/api/vendor/settlements/{id}`** — Vendor xem chi tiết kỳ thuộc mình. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/settlement/presentation/controllers/VendorSettlementController.java:61).

### Hồ sơ vendor (5 method/path)

- **POST `/api/vendor/profile`** — Customer/vendor đăng ký hồ sơ kinh doanh. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/vendor/presentation/controllers/VendorProfileController.java:50).
- **GET `/api/vendor/profile`** — Đọc hồ sơ riêng; có thông tin ngân hàng nên không dùng như public vendor DTO. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/vendor/presentation/controllers/VendorProfileController.java:60).
- **PATCH `/api/vendor/profile`** — Vendor sửa hồ sơ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/vendor/presentation/controllers/VendorProfileController.java:68).
- **POST `/api/vendor/documents`** — Tải giấy tờ hồ sơ vendor. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/vendor/presentation/controllers/VendorProfileController.java:78).
- **GET `/api/vendor/documents`** — Đọc giấy tờ hồ sơ vendor. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/vendor/presentation/controllers/VendorProfileController.java:91).

### Thời tiết và rule an toàn (8 method/path)

- **GET `/api/admin/category-safety-rules`** — Đọc ngưỡng an toàn theo danh mục. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/weather/presentation/controllers/AdminCategorySafetyRuleController.java:34).
- **PATCH `/api/admin/category-safety-rules/{categoryId}`** — Admin cập nhật rule an toàn. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/weather/presentation/controllers/AdminCategorySafetyRuleController.java:43).
- **GET `/api/admin/weather-alerts`** — Danh sách cảnh báo cần xử lý/đang theo dõi. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/weather/presentation/controllers/AdminWeatherAlertController.java:107).
- **POST `/api/admin/weather-alerts/{evaluationId}/resolve`** — Xử lý cảnh báo: tạo yêu cầu refund hoặc dismiss; dismiss chưa đổi lịch. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/weather/presentation/controllers/AdminWeatherAlertController.java:176).
- **GET `/api/weather/current`** — Đọc weather/marine theo tọa độ; cache TTL 30 phút, cần metadata/fallback rõ khi lỗi. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/weather/presentation/controllers/WeatherController.java:30).
- **GET `/api/v1/weather/current`** — Alias của /api/weather/current. Đọc weather/marine theo tọa độ; cache TTL 30 phút, cần metadata/fallback rõ khi lỗi. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/weather/presentation/controllers/WeatherController.java:30).
- **GET `/api/weather/advance-safety-check`** — Kiểm tra an toàn theo slot hoặc category/toạ độ/ngày giờ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/weather/presentation/controllers/WeatherController.java:42).
- **GET `/api/v1/weather/advance-safety-check`** — Alias của /api/weather/advance-safety-check. Kiểm tra an toàn theo slot hoặc category/toạ độ/ngày giờ. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/weather/presentation/controllers/WeatherController.java:42).

### Auth và endpoint kiểm tra quyền (12 method/path)

- **POST `/api/auth/oauth2/google`** — Đăng nhập Google OAuth2. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java:53).
- **POST `/api/auth/login`** — Đăng nhập email/password và refresh cookie. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java:67).
- **POST `/api/auth/register`** — Đăng ký tài khoản. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java:83).
- **POST `/api/auth/refresh`** — Làm mới access token theo refresh cookie. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java:102).
- **POST `/api/auth/logout`** — Thu hồi refresh và xóa cookie. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java:120).
- **POST `/api/auth/otp/send`** — Gửi OTP xác minh email của user đã có session. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java:131).
- **POST `/api/auth/otp/verify`** — Xác minh OTP email; không phải reset password. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java:140).
- **GET `/api/admin/dashboard`** — Placeholder xác nhận quyền admin; chưa có thống kê. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authorization/presentation/AdminController.java:15).
- **GET `/api/authorization/admin`** — Endpoint kiểm tra quyền, chỉ profile dev/test; không là tính năng sản phẩm. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationController.java:14).
- **GET `/api/authorization/vendor`** — Endpoint kiểm tra quyền, chỉ profile dev/test; không là tính năng sản phẩm. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationController.java:20).
- **GET `/api/authorization/user`** — Endpoint kiểm tra quyền, chỉ profile dev/test; không là tính năng sản phẩm. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationController.java:26).
- **GET `/api/authorization/product-read`** — Endpoint kiểm tra quyền, chỉ profile dev/test; không là tính năng sản phẩm. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationController.java:32).
