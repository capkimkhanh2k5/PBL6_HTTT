# Đánh giá mức độ đầy đủ của API DANASEA

Ngày rà soát: 06/10/2026. Checkout: `main`, commit `0a236ec7a44d6db56e9f4c8d56e1ba9962145cee`.

## Kết luận

Backend đã có API cho phần lớn luồng giao dịch của sàn trải nghiệm biển đa nhà cung cấp: tài khoản, xác minh vendor, danh mục/dịch vụ, giữ chỗ, đơn tổng/đơn thành phần, thanh toán, hủy/hoàn, thời tiết, AI, check-in, tranh chấp và đối soát. Tuy nhiên, chưa thể kết luận đã đầy đủ theo đề tài hoặc đủ để vận hành xuyên suốt. Có cả chức năng chưa triển khai và các API đã có nhưng chưa nối đúng nghiệp vụ/persistence.

Ba việc cần ưu tiên là sửa luồng payment/refund và quyền truy cập; bổ sung quản lý lịch/tồn chỗ; bổ sung thống kê/báo cáo phục vụ yêu cầu học phần. Sau đó hoàn thiện đánh giá, chi trả vendor, cam kết an toàn và các chức năng hỗ trợ.

Không quy đổi số endpoint thành phần trăm hoàn thành: một API tồn tại không chứng minh luồng nghiệp vụ đã hoạt động đúng.

## Cơ sở và giới hạn

- Đọc `docs/DANASEA.docx`, `docs/DANASEA_Plan.docx`, `docs/YeuCau_PBL6_HTTT.docx.pdf`, tài liệu workflow và checklist API.
- Kiểm kê tất cả controller HTTP, đọc hợp đồng request/response, `SecurityConfig`, và theo các luồng trọng yếu tới usecase/adapter/repository.
- Phát hiện 35 controller, 102 handler HTTP tương ứng 106 tổ hợp method/path vì catalog và weather có alias. Số này gồm 4 đường dẫn kiểm tra phân quyền chỉ bật ở profile dev/test và dashboard hiện là placeholder. Không tính Actuator/Swagger do thư viện cung cấp.
- Test kiểm kê Spring trong suite cũng phát hiện 106 endpoint ở profile test, khớp kiểm kê mã nguồn.
- Nhận xét bảo mật/payment bên dưới dựa vào đường gọi trong code; chưa thực hiện giao dịch sandbox thực hoặc khai thác trên môi trường triển khai.
- Rà soát chức năng backend; không nghiệm thu Web/Flutter, SEO, trải nghiệm giao diện, tải thực hoặc chất lượng tư vấn AI.

## Phần đã có

- IAM: đăng ký, đăng nhập, Google OAuth2, refresh/logout, OTP xác minh email, hồ sơ, đổi mật khẩu, quản trị khóa/mở tài khoản và audit log.
- Vendor/catalog: hồ sơ có thông tin ngân hàng, giấy tờ, duyệt/từ chối vendor, CRUD dịch vụ, submit/approve/reject, pause/resume, ảnh và chứng chỉ an toàn, danh mục, tìm kiếm theo từ khóa/giá/tọa độ, wishlist và gần đây đã xem.
- Booking/order: giữ nhiều slot, xác nhận, hủy hold, lịch sử/detail, đơn tổng và đơn thành phần. API vendor reject đã có dù checklist còn để chưa xong.
- Giao dịch: API payment intent, webhook, cancellation preview, refund request. Mức độ tồn tại API cần phân biệt với những lỗi kết nối được nêu dưới đây.
- Vận hành: QR/check-in, tạo và xử lý tranh chấp, generate/list/detail/finalize settlement.
- Thời tiết/AI: thời tiết khí tượng và biển, kiểm tra an toàn trước booking, rule theo danh mục, giám sát slot, cảnh báo và xử lý admin; AI chat/tool calling, confirmation card, lưu hội thoại, rate limit và nội dung Việt/Anh.
- Thông báo: lưu và đọc thông báo, có hạ tầng gửi email qua hàng đợi. Chat AI khác với nhắn tin customer-vendor.

## Những vấn đề cần sửa ở API đã có

### P0 Thanh toán chưa nối đủ payment intent với webhook — ĐÃ XỬ LÝ

Cập nhật 06/10/2026: `CreatePaymentIntentUseCase` lưu và commit payment trước khi gọi gateway. Một transaction tiếp theo khóa payment rồi order để tuần tự hóa các request cùng key; khi gateway timeout, payment PENDING vẫn tồn tại và retry dùng lại cùng UUID. PayPal nhận `PayPal-Request-Id` bằng UUID đó. Entry point cũ trong `OrderPaymentService` cũng ủy quyền cho cùng use case.

Payment SUCCESS/FAILED/REFUNDED không được trả lại thành intent PENDING. Intent hết hạn được chuyển FAILED và commit trước khi trả HTTP 409. Customer/admin đã có API danh sách/chi tiết payment trong tracking. `PaymentIntentPersistenceIntegrationTest` kiểm tra PostgreSQL và Flyway thật, gateway/booking confirmation được mock; test webhook ở đây dùng HMAC/DTO nội bộ, không chứng minh callback của nhà cung cấp.

Lõi nối payment ID trong hệ thống đã được sửa. Tích hợp provider hoàn chỉnh vẫn còn các việc ở mục cổng thanh toán bên dưới. Xem [kết quả kiểm tra sandbox](payment-sandbox-verification-2026-10-06.md).

### P0 Hoàn tiền chưa thống nhất persistence và thực thi -> ĐÃ XỬ LÝ

`OrderController.requestRefund()` gọi `RequestRefundUseCase`. Usecase truyền `subOrderId.toString()` vào tham số `providerTransactionId`, bỏ qua kết quả gateway, tạo UUID refund trong response, nhưng không lưu refund, không tra idempotency đã xử lý và không cập nhật/nhả chỗ trong luồng đó. Webhook refund lại cần tìm refund đã lưu. Hệ quả: ID phản hồi không có lifecycle bền vững và gọi lặp chưa được bảo vệ bằng việc chỉ yêu cầu header.

Các luồng hủy booking, vendor reject, weather và dispute có tạo refund PENDING trong DB, nhưng không thấy worker/job/consumer gửi các bản ghi PENDING này tới cổng gốc. PENDING là trạng thái đúng trước khi có xác nhận của cổng; còn thiếu bước gửi, đối chiếu kết quả và thử lại có kiểm soát.

Cần dùng payment thành công gốc để lấy provider và transaction/capture ID; persist yêu cầu trước khi gọi ngoài; worker có idempotency, retry và trạng thái lỗi; chỉ đánh dấu PROCESSED khi có xác nhận; đồng bộ master/sub-order, booking và inventory. Cung cấp lịch sử/trạng thái refund cho customer/admin.

Bằng chứng: `modules/order/application/usecases/RequestRefundUseCase.java:140`, `modules/order/application/OrderPaymentService.java:416`, `modules/order/infrastructure/BookingCancellationFinancialAdapter.java`, `modules/dispute/application/usecases/ResolveDisputeUseCase.java`, `modules/weather/presentation/controllers/AdminWeatherAlertController.java`.

### P0 Kiểm tra quyền sở hữu còn thiếu ở một số nhánh — ĐÃ XỬ LÝ

Cập nhật 08/10/2026:
- `GET /api/assistant/conversations/{id}` và `GET .../{id}/history`: `AssistantController` đã chuyển sang gọi `ChatHistoryService.getConversationForUser()` và `getRecentMessagesForUser()`, kiểm tra quyền sở hữu của user đã đăng nhập. Trả về 403 Forbidden (`ACCESS_DENIED`) nếu hội thoại thuộc user khác, 404 Not Found nếu không tồn tại.
- `POST /api/assistant/conversations/{id}/confirm`: `AssistantController` đã kiểm tra quyền sở hữu hội thoại trước và truyền `id` vào overload `confirmBookingUseCase.execute(cardId, userId, sessionId, id)`. Ràng buộc chặt chẽ Card, Conversation và User trước khi tạo hold; trả về 403 nếu hội thoại thuộc user khác hoặc card thuộc conversation khác.
- `POST /api/orders`: `CreateOrderUseCase` đã bổ sung kiểm tra quyền sở hữu `order.getCustomerId().equals(command.customerId())` ở cả hai nhánh idempotency (`findByBookingId` và `findByCustomerIdAndIdempotencyKey`), ném `UnauthorizedOrderAccessException` (403 `UNAUTHORIZED_ORDER_ACCESS`) nếu phát hiện request lặp của user khác.
- Đã bổ sung 2 bộ test regression ở tầng HTTP với 2 tài khoản A/B: `AssistantOwnershipHttpRegressionTest` (10 test cases) và `OrderOwnershipHttpRegressionTest` (5 test cases), cùng các test case bổ sung trong `CreateOrderUseCaseTest`.

Bằng chứng: `modules/ai/presentation/controllers/AssistantController.java:103`, `:123`, `:131`; `modules/ai/domain/services/ChatHistoryService.java`; `modules/order/application/usecases/CreateOrderUseCase.java:64`; `modules/ai/presentation/controllers/AssistantOwnershipHttpRegressionTest.java`; `modules/order/presentation/controllers/OrderOwnershipHttpRegressionTest.java`.

### P0/P1 Cổng thanh toán có route nhưng tích hợp thực chưa hoàn chỉnh

- Đã chặn MOMO/SEPAY ở use case và PayPal adapter. Chỉ VNPAY/PAYPAL được tạo intent. MoMo chưa có adapter; SePay đã được bỏ khỏi thiết kế nhưng giữ enum/schema cho dữ liệu cũ.
- Đã bỏ token/URL mô phỏng khi PayPal OAuth/order lỗi; API trả 502 và giữ payment để retry. PayPal refund vẫn trả success mô phỏng khi request thật lỗi, cần sửa ở phạm vi refund. VNPay refund hiện tạo ID ngẫu nhiên và trả success, chưa gửi lệnh hoàn thật.
- Các HTTP webhook hiện dùng HMAC SHA-256 của DANASEA và payload DTO nội bộ. Nhánh xác minh provider trong gateway không phải nhánh được raw HTTP webhook gọi. Vì vậy test HMAC nội bộ không chứng minh callback thực của VNPay/MoMo/PayPal hoạt động.
- Đã bỏ nhánh chấp nhận chữ ký PayPal thiếu metadata và sửa `webhook_event` thành object khi gọi API xác minh. Raw HTTP webhook vẫn dùng signer HMAC riêng, chưa nối tới nhánh xác minh provider; cần nối theo hợp đồng PayPal.
- Không thấy bước capture order PayPal trong backend. Luồng redirect hiện cần bổ sung capture và lưu provider IDs tương ứng.

PayPal công bố riêng bước [capture sau approval](https://developer.paypal.com/api/orders/v2/orders-capture) và [xác minh webhook với bộ transmission headers và webhook event](https://developer.paypal.com/api/webhooks/v1/verify-webhook-signature-post). Adapter/callback cần khớp hợp đồng của cổng. Có thể mô phỏng cho đồ án, nhưng cần tách chế độ mô phỏng rõ ràng và có bằng chứng sandbox cho cổng công bố đã tích hợp.

Bằng chứng: `modules/order/infrastructure/adapters/PayPalPaymentAdapter.java:82`, `:86`, `:175`, `:223`, `:227`; `VNPayPaymentAdapter.java:152`; `modules/order/application/PaymentWebhookSigner.java`; `modules/order/presentation/controllers/PaymentController.java:99`.

## API và tính năng cần bổ sung theo ưu tiên

Các đường dẫn dưới đây là hợp đồng đề xuất, chưa tồn tại trừ khi ghi rõ mở rộng API có sẵn. Không cần tách endpoint riêng nếu có thể hoàn thiện hợp đồng hiện tại.

### P1 Quản lý lịch và tồn chỗ

Có model/repository slot và engine giữ chỗ, nhưng chưa có API vendor tạo/sửa/đóng/mở slot. `capacityPerSlot` trên service không thay thế lịch bán. Public detail hiện trả `availableSlots: List<String>` theo ngày/giờ, thiếu slotId mà `POST /bookings/hold` bắt buộc nhận; thiếu sức chứa, số chỗ trống và trạng thái cho khách lựa chọn.

- `GET /api/services/{id}/slots?from=&to=&quantity=` trả đối tượng slot gồm slotId, ngày, giờ bắt đầu/kết thúc, chỗ còn, trạng thái và giá nếu áp dụng theo slot.
- `GET /api/vendor/services/{id}/slots`.
- `POST /api/vendor/services/{id}/slots`, có thể hỗ trợ tạo hàng loạt theo lịch lặp.
- `PATCH /api/vendor/services/{id}/slots/{slotId}` để đổi sức chứa/giờ khi chưa ảnh hưởng đơn đã đặt.
- `PATCH .../{slotId}/close` và `PATCH .../{slotId}/open`, hoặc một API đổi trạng thái.

Không giảm capacity thấp hơn booked/held; không xóa slot có giao dịch; chặn thao tác vendor khác. Availability chỉ là dữ liệu tham khảo; hold vẫn phải kiểm tra nguyên tử. Cần test cạnh tranh slot cuối bằng Redis/DB thật.

### P1 Thống kê và báo cáo

> Cập nhật 09/10/2026: đã triển khai và sửa nghiệp vụ báo cáo trên `implement_admin_reports_dashboard`; xem [hợp đồng hiện tại](reports-dashboard-contract.md). Nội dung dưới đây là phát hiện tại thời điểm audit 06/10.

Yêu cầu học phần bắt buộc có báo cáo theo ngày, tuần, quý, năm và khoảng từ ngày đến ngày. `/api/admin/dashboard` chỉ trả `ADMIN_ACCESS_GRANTED`. Listing settlement có bộ lọc ngày là chức năng đối soát, chưa thay thế báo cáo kinh doanh/phân tích.

- Thay nội dung `GET /api/admin/dashboard` bằng số liệu thật.
- `GET /api/admin/reports/revenue?from=&to=&groupBy=day|week|quarter|year`.
- `GET /api/admin/reports/bookings?from=&to=&groupBy=`: số đơn, hoàn thành, hủy, lý do hủy.
- `GET /api/admin/reports/vendors?from=&to=`: doanh thu, đơn, hoàn/hủy, tỷ lệ sử dụng chỗ và chất lượng theo vendor.
- `GET /api/vendor/dashboard`, `GET /api/vendor/reports/revenue` với cùng quy ước ngày và isolation theo vendor.
- `GET /api/admin/reports/export?type=&from=&to=&format=csv`; phiên bản vendor tương ứng. CSV đủ cho MVP nếu chưa cần XLSX.

Phân biệt giá trị bán, tiền thu, hoàn tiền, hoa hồng và số thực nhận; thống nhất trạng thái được tính, timezone và các ngày biên. Phần phân tích nên giúp chọn thời gian/dịch vụ/vendor cần cải thiện, không chỉ cộng tổng.

### P1 Đánh giá và chất lượng dịch vụ

`Review`, JPA entity và repository đã có; chưa có usecase/controller ghi và đọc đánh giá. Trường averageRating/reviewCount trên catalog chưa chứng minh vòng đời đánh giá hoạt động.

- `POST /api/sub-orders/{id}/reviews`: chỉ chủ đơn đã hoàn thành; một đánh giá cho một trải nghiệm, hỗ trợ ảnh nếu giữ phạm vi đã đăng ký.
- `GET /api/services/{id}/reviews` có phân trang.
- `GET /api/vendor/reviews`, `POST /api/vendor/reviews/{id}/reply`.
- `GET /api/admin/reviews`, `PATCH /api/admin/reviews/{id}/visibility`; báo cáo nội dung nếu cần.

Cập nhật điểm service/vendor từ đánh giá hợp lệ; xử lý điểm khi ẩn đánh giá và khi người dùng sửa/xóa theo chính sách. Badge uy tín nên suy ra từ dữ liệu này.

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

### P1 Quên mật khẩu

Có đổi mật khẩu khi đã biết mật khẩu cũ và OTP xác minh email. PasswordResetToken hiện chỉ có model/entity/repository; chưa có API phục hồi tài khoản.

- `POST /api/auth/forgot-password` và `POST /api/auth/reset-password`.
- Token/OTP riêng mục đích reset, hết hạn, dùng một lần, rate limit; response không tiết lộ email tồn tại; thu hồi phiên cũ khi reset thành công.

### P1 Quản trị giao dịch và theo dõi hoàn tiền

Admin xem detail order/booking nếu đã biết UUID, nhưng chưa có API list giao dịch toàn sàn, lọc đơn và lịch sử refund/payment phục vụ xử lý vấn đề.

- `GET /api/admin/orders`, `GET /api/admin/payments`, `GET /api/admin/refunds`, lọc trạng thái/provider/vendor/customer/thời gian.
- `GET /api/orders/{id}/payments`, `GET /api/orders/{id}/refunds`, kiểm tra owner.
- Tra trạng thái cổng khi webhook mất/chậm, retry an toàn và lưu lịch sử; đây là service/job nội bộ, không nhất thiết phải có endpoint công khai.

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

Checklist `danasea-api-tracking.md` hiện ghi nhiều API weather/AI/check-in/dispute/settlement chưa xong dù code đã có; ngược lại thiếu module reviews/promotions/messaging/reporting/payout. Workflow refund mô tả persist/cancel/release slot nhưng đường HTTP hiện tại chưa làm các bước đó. Cần cập nhật cả hợp đồng API, workflow và tiêu chí nghiệm thu từ code cuối cùng.

Giỏ hàng không bắt buộc cần CRUD backend riêng: yêu cầu học phần cho phép có hoặc không tùy ứng dụng. Client cart cộng với hold nhiều item có thể đáp ứng MVP; chỉ cần API cart nếu muốn lưu bền vững/đồng bộ nhiều thiết bị. Ba cổng thanh toán cũng không cần hoàn thiện đồng thời nếu một cổng nội địa và một cổng quốc tế đáp ứng phạm vi đã chốt.

## Đề xuất thứ tự triển khai

1. Sửa quyền truy cập, thống nhất payment intent/webhook và refund lifecycle; loại bỏ success mô phỏng khỏi adapter thật. Có test HTTP tới DB và callback sandbox cho cổng chọn.
2. Hoàn thiện slot CRUD/availability và dữ liệu detail; test cạnh tranh slot cuối, hold expiry và rollback nhiều item.
3. Thống kê/báo cáo admin/vendor, lọc khoảng thời gian, CSV; đây là khoảng trống trực tiếp của yêu cầu học phần.
4. Review, waiver, forgot password, payout/mark-paid, commission và admin quản trị giao dịch.
5. Đổi lịch, voucher, nhắn tin, tranh chấp đọc/phản hồi, khám phá và thông báo; xếp lại theo phạm vi nghiệm thu và thời gian còn lại.

## Kiểm chứng trong lần rà soát này

Chạy `./mvnw -q test` từ backend. Kết quả tổng hợp Maven: **1497 tests, 1 failure, 27 errors, 136 skipped**, exit code 1. Các test skipped không phải bằng chứng pass.

- Failure: `BackendApplicationTests.allApplicationEndpointsAreDiscoverable` kỳ vọng 105 endpoint nhưng Spring phát hiện 106. Cần cập nhật kiểm kê có chủ đích; đây không tự nó là lỗi nghiệp vụ.
- 27 errors thuộc các test tích hợp liên quan Testcontainers không tìm được Docker environment và các class phụ thuộc khởi tạo thất bại. Cần chạy lại khi Docker dùng được; chưa thể nghiệm thu các invariant bằng kết quả này.
- Context test thông thường đã chạy được, nhưng không thay thế full integration context với PostgreSQL/Redis thật.
- Những vấn đề payment/refund/owner nêu trên là phát hiện từ mã nguồn; suite hiện tại chưa chứng minh đã bao phủ các đường lỗi đó.

Không sửa mã nguồn sản phẩm hoặc test trong lần nhận xét này. Phụ lục dưới đây liệt kê toàn bộ API tìm thấy, gồm alias và endpoint chỉ dùng dev/test.

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

### Order và payment (13 method/path)

- **POST `/api/orders`** — Tạo master/sub-orders từ booking; nhánh trả order cũ cần kiểm tra owner. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:77).
- **GET `/api/orders`** — Danh sách đơn customer; chưa có các bộ lọc kinh doanh/time/status. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:89).
- **GET `/api/orders/{id}`** — Chi tiết order của owner hoặc admin; chưa trả lịch sử payment/refund. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:108).
- **POST `/api/orders/{id}/refund-request`** — Yêu cầu hoàn; hiện cần sửa persist/idempotency/providerTransactionId. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:116).
- **GET `/api/orders/{id}/cancellation-preview`** — Tính mức hoàn trước hủy; cần đồng nhất với lệnh hủy/refund. [Mã nguồn](/Users/capkimkhanh/Documents/DUT4_1/PBL6/backend/src/main/java/com/danasea/backend/modules/order/presentation/controllers/OrderController.java:145).
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
