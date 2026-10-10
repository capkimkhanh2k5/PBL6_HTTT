# DANASEA — Danh sách trang Web và màn hình Mobile cần có

**Ngày phân tích:** 09/10/2026.

**Phạm vi:** toàn bộ API controller trong checkout hiện tại, các luồng nghiệp vụ backend, các route/màn hình React và Flutter, cùng thiết kế đã có trong repository.

Web có ba vai trò **Khách hàng (CUSTOMER), Nhà cung cấp (VENDOR), Quản trị viên (ADMIN)**. Mobile có hai vai trò **Khách hàng và Vendor**; repository hiện tách thành `mobile/` và `vendor_mobile/`. Admin vận hành qua web.

## 1. Cách đọc và kết quả phân tích

Danh sách dưới đây mô tả **trang/màn hình nghiệp vụ cần có**, không phải xác nhận giao diện đã tích hợp thật. Một trang có thể có nhiều tab, drawer hoặc bottom sheet; không cần tạo một route cho từng nút gọi API. Các đường dẫn web là **đề xuất**, cần bổ sung hoặc đối chiếu với router hiện tại khi triển khai.

### 1.1. Mức hỗ trợ của backend

| Ký hiệu | Ý nghĩa |
| --- | --- |
| **Có API** | Đã có endpoint phục vụ thao tác chính; vẫn cần tích hợp và nghiệm thu giao diện. |
| **Một phần** | Có một số endpoint nhưng thiếu dữ liệu, thao tác hoặc bước nghiệp vụ để hoàn tất trang. |
| **Thiếu API** | Chưa có endpoint phục vụ chức năng chính trong controller hiện tại. |
| **Client/tĩnh** | Có thể xử lý bằng giao diện, điều hướng hoặc nội dung tĩnh; không nhất thiết cần API riêng. |

**Ưu tiên:** P0 khép kín đăng nhập → bán dịch vụ → đặt và thanh toán → thực hiện dịch vụ; P1 khép kín quản trị, an toàn, hoàn tiền, đối soát và các tính năng AI đã có; P2 hoàn thiện các nhóm sản phẩm đã có trong thiết kế/model nhưng chưa có API như chat, đánh giá, voucher và chi trả. P2 vẫn cần làm nếu nghiệm thu toàn bộ các nhóm chức năng này, nhưng không được coi là đã hoạt động chỉ vì có màn hình mock.

### 1.2. Những điều cần biết trước khi thiết kế trang

- Kiểm kê tĩnh được **44 controller, 140 tổ hợp HTTP method/path**, bao gồm alias, API chẩn đoán và webhook chỉ dùng trong test. Danh mục đầy đủ và trang sử dụng nằm ở mục 9; không lấy số lượng cũ trong API checklist làm số lượng hiện tại.
- Đã có API cho tài khoản, hồ sơ vendor, danh mục, dịch vụ, ảnh, chứng từ an toàn dịch vụ, option, ca/tồn chỗ, booking, order, payment/refund, check-in, dispute của admin, weather, settlement, notification và các tính năng AI hiện tại.
- Chưa có API hoàn chỉnh cho chat khách–vendor, đánh giá/ phản hồi đánh giá, voucher, yêu cầu chi trả, báo cáo thống kê, đổi lịch thực tế, theo dõi/phản hồi dispute của khách và vendor, đánh dấu thông báo đã đọc, hoặc cấu hình hệ thống qua giao diện.
- `GET /api/admin/dashboard` hiện trả `ADMIN_ACCESS_GRANTED`, không trả KPI. Vendor cũng chưa có API dashboard/báo cáo riêng.
- React có các route và dữ liệu mock; Flutter có các màn hình dùng mock database/repository. Cần nối API theo từng luồng, không suy ra khả năng vận hành từ việc trang đã tồn tại.
- Một tài khoản hiện có trường `role` đơn. Đăng ký hồ sơ vendor đổi tài khoản sang VENDOR; không có contract chuyển qua lại CUSTOMER/VENDOR theo nút chọn trên UI. Các endpoint đặt/ thanh toán nhiều nơi chỉ cho CUSTOMER. Không dùng bộ chuyển role trong bản phát triển làm cơ chế cấp quyền thật.

## 2. Web — Trang dùng chung và xác thực

Các trang này dùng chung cho ba vai trò, sau đăng nhập điều hướng theo role do backend trả về. Admin không đăng ký công khai bằng lựa chọn vai trò.

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| G01 | Đăng nhập / đăng ký — `/auth` | Tab đăng nhập, đăng ký khách hàng, Google OAuth; `POST /api/auth/login`, `/register`, `/oauth2/google`. Vendor mới bắt đầu từ tài khoản khách rồi đăng ký hồ sơ đối tác. | Có API | P0 |
| G02 | Xác minh email — `/auth/verify-email` | Nhập OTP, gửi lại mã, đếm thời gian và báo vượt giới hạn; `POST /api/auth/otp/send`, `/otp/verify`. Hai API này cần phiên đăng nhập, khác OTP khôi phục mật khẩu. | Có API | P0 |
| G03 | Khôi phục mật khẩu — `/auth/forgot-password` | Hai bước yêu cầu mã và nhập OTP/mật khẩu mới; `POST /api/auth/forgot-password`, `/reset-password`. Sau thành công quay về đăng nhập. Có thể tách `/auth/reset-password` nếu cần deep link. | Có API | P0 |
| G04 | Cài đặt cá nhân — `/settings` hoặc tab tài khoản | Ngôn ngữ VI/EN, đăng xuất; `PATCH /api/users/me`, `POST /api/auth/logout`. Gửi `Accept-Language`; cấu hình push và các tùy chọn thông báo chưa có contract riêng. | Một phần | P1 |
| G05 | Hướng dẫn, chính sách, điều khoản an toàn — `/help`, `/policies` | Cách đặt dịch vụ, hủy/hoàn, quyền riêng tư, điều khoản tham gia, liên hệ hỗ trợ. Chính sách phải thống nhất backend; chứng nhận đã chấp thuận waiver cần bổ sung contract lưu vết. | Client/tĩnh; waiver thiếu API | P1 |
| G06 | Trạng thái truy cập và lỗi — `/403`, `/404`, màn hình lỗi dùng chung | Sai vai trò, phiên hết hạn, tài khoản khóa, dữ liệu không còn tồn tại, lỗi mạng/rate limit và thử lại. Refresh phiên bằng `POST /api/auth/refresh`; không cần trang riêng cho refresh/logout. | Client/tĩnh + API auth | P0 |

## 3. Web — Khách hàng

Khách chưa đăng nhập có thể xem catalog, danh mục, slot công khai và thời tiết. Yêu thích, AI, đặt/ thanh toán và dữ liệu cá nhân cần đăng nhập theo security hiện tại.

### 3.1. Khám phá và đặt dịch vụ

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| C01 | Trang chủ — `/` | Danh mục, trải nghiệm nổi bật, thời tiết, đã xem gần đây; `GET /api/categories`, `/api/services`, `/api/recently-viewed`, `/api/weather/current`. Khách đăng nhập có khối gợi ý từ `POST /api/ai/recommendations`. | Có API; nội dung banner có thể tĩnh | P0 |
| C02 | Tìm kiếm / khám phá — `/search` | Keyword, danh mục, khoảng giá, vị trí/bán kính, phân trang. Tìm bằng ngôn ngữ tự nhiên qua `POST /api/ai/search`; khám phá gần đây qua `/api/ai/nearby`. Các bộ lọc ngày, rating, vendor, sort nâng cao cần bổ sung nếu dùng cho toàn catalog. | Có API cơ bản; lọc nâng cao một phần | P0 |
| C03 | Chi tiết trải nghiệm — `/experiences/:serviceId` | Ảnh, nội dung, vị trí, giá, option ghép/thuê riêng, chọn ca và số lượng; `GET /api/services/{id}`, `/api/services/{id}/slots`. Khối thời tiết dùng `/api/weather/advance-safety-check`, `POST /api/ai/weather`; tóm tắt review dùng `GET /api/ai/review-summaries/{serviceId}`. Chi tiết vendor, review gốc và waiver khách đọc chưa được trả đầy đủ. | Một phần | P0 |
| C04 | So sánh trải nghiệm — `/compare` | Chọn nhiều dịch vụ và ngữ cảnh chuyến đi, so sánh giá/khả dụng/đặc điểm; `POST /api/ai/services/compare`. Có thể làm drawer trên C02/C03. | Có API | P1 |
| C05 | Yêu thích — `/account/wishlist` | Xem, thêm, bỏ dịch vụ yêu thích; `GET /api/wishlists`, `POST/DELETE /api/wishlists/{serviceId}`. | Có API | P1 |
| C06 | Giỏ trải nghiệm — `/cart` | Nhiều dịch vụ, option, slot, số người/gói, giá tạm tính; kiểm tra lại slot trước checkout. Giỏ lưu client hiện tại; không có CRUD cart server. Chỉ tạo hold khi khách tiếp tục đặt. | Client + API catalog/slot/hold | P0 |
| C07 | Checkout / giữ chỗ — `/checkout` | Tóm tắt từng item, thời hạn hold, tổng tiền, điều kiện an toàn, chọn cổng; `POST /api/bookings/hold`, `GET /api/bookings/{id}`, `DELETE /api/bookings/hold/{holdId}`, `POST /api/orders`, `POST /api/payments/{orderId}/create-intent`. Mã giảm giá và lưu waiver còn thiếu. | Một phần | P0 |
| C08 | Kết quả thanh toán — `/checkout/result` | Một route có trạng thái đang xử lý, thành công, thất bại/hết hạn; đọc `GET /api/payments/{orderId}/status`, `/api/payments/detail/{paymentId}`, `GET /api/orders/{id}`. PayPal return gọi `POST /api/payments/paypal/capture` theo contract. Chỉ xác nhận booking khi backend đã xác nhận thanh toán đủ. | Có API | P0 |

### 3.2. Theo dõi và xử lý sau đặt

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| C09 | Đơn hàng / booking của tôi — `/account/orders` | Danh sách đơn, booking giữ chỗ/chờ thanh toán/đã xác nhận; `GET /api/orders`, `GET /api/bookings`. Tách tab booking và order, tránh hiển thị cùng một giao dịch thành hai đơn khác nhau. Bộ lọc order nâng cao chưa có. | Có API cơ bản | P0 |
| C10 | Chi tiết đơn — `/account/orders/:orderId` | Master order và từng sub-order, dịch vụ/ca, thanh toán, hoàn tiền, vé và hành động hợp lệ; `GET /api/orders/{id}`, `/api/bookings/{id}`, `/api/payments/{orderId}/payments`, `/api/orders/{id}/refunds`. Bổ sung dữ liệu liên hệ/tham gia nếu DTO chưa đủ. | Một phần | P0 |
| C11 | Vé điện tử / QR — `/account/orders/:orderId/tickets/:subOrderId` | Vé theo từng dịch vụ, ca và thời hạn; `POST /api/bookings/{id}/qr-code`. UI dùng **subOrderId** làm id để hỗ trợ đơn nhiều dịch vụ, không truyền bookingId chỉ vì tên route là bookings. | Có API | P0 |
| C12 | Hủy đặt / yêu cầu hoàn — `/account/orders/:orderId/cancel` | Xem mức hoàn trước khi xác nhận qua `GET /api/orders/{id}/cancellation-preview`. Hủy booking qua `PATCH /api/bookings/{id}/cancel`; yêu cầu hoàn độc lập dùng `POST /api/orders/{id}/refund-request`. Chọn đúng lệnh theo nghiệp vụ, không gọi cả hai mặc định. | Có API | P1 |
| C13 | Theo dõi hoàn tiền — `/account/orders/:orderId/refunds` | Từng refund, số tiền, lý do, PENDING/PROCESSED/FAILED; `GET /api/orders/{id}/refunds`. Có thể là tab C10. | Có API | P1 |
| C14 | Khiếu nại của tôi — `/account/disputes` | Danh sách và trạng thái xử lý các khiếu nại. Hiện thiếu API list dispute của khách. | Thiếu API | P2 |
| C15 | Tạo khiếu nại — `/account/orders/:orderId/disputes/new` | Chọn sub-order, lý do, mô tả, chứng cứ; `POST /api/orders/{id}/disputes`, body có `subOrderId` và `evidenceUrls`. Chưa có endpoint upload chứng cứ dispute. | Một phần | P1 |
| C16 | Chi tiết khiếu nại — `/account/disputes/:disputeId` | Nội dung, minh chứng, phản hồi vendor, kết luận admin, bổ sung chứng cứ; chưa có API detail/follow-up cho khách. | Thiếu API | P2 |
| C17 | Viết / quản lý đánh giá — `/account/reviews` | Viết đánh giá từ sub-order đủ điều kiện, đọc đánh giá đã gửi/phản hồi vendor. Có model review nhưng chưa có API tạo, danh sách, upload ảnh hay sửa đánh giá. Không nhầm API tóm tắt review AI với API review. | Thiếu API | P2 |
| C30 | Đổi lịch dịch vụ — `/account/orders/:orderId/reschedule` | Xem ca thay thế, chênh lệch giá, điều kiện đổi và xác nhận. Chưa có API chuyển booking/order sang ca mới; sửa lịch AI không thực hiện thay đổi này. | Thiếu API | P2 |

### 3.3. AI, liên lạc và tài khoản

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| C18 | Trợ lý AI — `/assistant` | Hội thoại tư vấn, dịch vụ gợi ý, confirmation card và chuyển sang checkout; `POST /api/assistant/chat`, `GET /api/assistant/conversations/{id}`, `.../{id}/history`, `POST .../{id}/confirm`. Chưa có API liệt kê toàn bộ hội thoại AI của user. | Có API hội thoại cụ thể; danh sách một phần | P1 |
| C19 | Lịch trình của tôi / tạo lịch trình — `/account/itineraries` | Form ngày, ngân sách, số người/sở thích và danh sách lịch đã lưu; `GET/POST /api/ai/itineraries`. Form có thể là tab hoặc trang con `/new`. | Có API | P1 |
| C20 | Chi tiết / điều chỉnh lịch trình — `/account/itineraries/:itineraryId` | Xem hoạt động, giá snapshot, cảnh báo và đề xuất thay thế; `GET /api/ai/itineraries/{id}`, `POST .../{id}/replan` với `expectedVersion`. Đặt các mục phù hợp qua hold/checkout thông thường. | Có API | P1 |
| C21 | Hỗ trợ đơn hàng — `/account/orders/:orderId/support` | Tra cứu và giải thích trạng thái/chính sách bằng `POST /api/ai/support`. Có thể gộp C10/C18. API hỗ trợ không tự gửi ticket, hủy, hoàn hoặc đổi lịch. | Có API tư vấn | P1 |
| C22 | Hộp thư khách–vendor — `/messages` | Danh sách hội thoại, khách liên hệ vendor từ dịch vụ hoặc đơn, unread. Chưa có API hội thoại giữa người dùng. | Thiếu API | P2 |
| C23 | Hội thoại khách–vendor — `/messages/:conversationId` | Gửi/nhận tin nhắn, ảnh/tệp, lịch sử, trạng thái đọc; cần API chat và cơ chế cập nhật. Có thể dùng khung hai cột chung route C22. | Thiếu API | P2 |
| C24 | Thông báo — `/notifications` | Đơn, thanh toán, hoàn tiền, an toàn; `GET /api/notifications`. Có thể mở panel và điều hướng tới đối tượng liên quan. Đánh dấu đã đọc, unread count, push token còn thiếu. | Một phần | P1 |
| C25 | Tài khoản cá nhân — `/account` | Thông tin tài khoản, email, ngôn ngữ, liên kết đơn/AI/yêu thích; `GET /api/users/me`. | Có API | P0 |
| C26 | Chỉnh sửa hồ sơ — `/account/profile` | Các trường backend cho phép cập nhật và locale; `PATCH /api/users/me`. Upload avatar cần contract riêng nếu có yêu cầu giao diện. | Có API trường hồ sơ; upload một phần | P1 |
| C27 | Đổi mật khẩu — `/account/change-password` | Mật khẩu cũ/mới; `POST /api/users/me/change-password`. Xử lý phiên cũ bị thu hồi và quay về đăng nhập. | Có API | P1 |
| C28 | Hồ sơ vendor công khai — `/vendors/:vendorId` | Doanh nghiệp, xác minh, dịch vụ và review của vendor. Hiện chưa có API hồ sơ vendor công khai hoặc lọc catalog theo vendor; không dùng `/api/admin/vendors/{id}` cho khách. | Thiếu API | P2 |
| C29 | Ưu đãi / mã giảm giá — `/promotions` hoặc tab checkout | Voucher toàn sàn/vendor, điều kiện áp dụng, preview giảm giá; chưa có API voucher. Có thể chỉ là khối trong C01/C07 khi hoàn thiện backend. | Thiếu API | P2 |

**Điều hướng web khách hàng:** Trang chủ → Khám phá → Chi tiết → Giỏ → Checkout → Kết quả → Đơn/vé; nhóm phụ là Yêu thích, AI/lịch trình, Tin nhắn, Thông báo và Tài khoản. C04, C12, C13, C15, C21, C26, C27 có thể là drawer/tab khi vẫn hỗ trợ điều hướng và tải lại dữ liệu.

## 4. Web — Vendor

Vendor cần cả luồng đăng ký/xác minh và luồng vận hành hằng ngày. Không bỏ màn hình hồ sơ chờ duyệt vì chỉ thiết kế dashboard cho vendor đã được chấp thuận.

### 4.1. Hồ sơ và quản lý dịch vụ

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| V01 | Tổng quan vendor — `/vendor` | Đơn sắp diễn ra, cảnh báo, doanh thu, tác vụ nhanh. Có thể lấy danh sách từ `/api/vendor/services`, `/bookings`, `/orders`, `/settlements`; thiếu API KPI toàn kỳ, không cộng một trang phân trang rồi gọi đó là tổng doanh thu. | Một phần | P1 |
| V02 | Đăng ký trở thành vendor — `/vendor/onboarding` | Thông tin doanh nghiệp, thuế, địa chỉ, ngân hàng; `POST /api/vendor/profile`. Sau đổi role cần lấy lại phiên/claims phù hợp. Không tự cấp role bằng lựa chọn trên giao diện. | Có API | P0 |
| V03 | Hồ sơ xác minh / giấy tờ — `/vendor/verification` | Trạng thái PENDING/APPROVED/REJECTED, tải BUSINESS_LICENSE và SAFETY_CERT; `GET /api/vendor/profile`, `GET/POST /api/vendor/documents`. Thiếu luồng duyệt giấy tờ vendor và gửi lại hồ sơ bị từ chối; xem mục 7. | Một phần | P0 |
| V04 | Hồ sơ doanh nghiệp / ngân hàng — `/vendor/profile` | Xem/sửa doanh nghiệp và tài khoản nhận tiền; `GET/PATCH /api/vendor/profile`. Có thể tách tab doanh nghiệp/ngân hàng. | Có API | P0 |
| V05 | Danh sách dịch vụ — `/vendor/services` | Xem trạng thái, mở soạn thảo, gửi duyệt, tạm ngừng/mở lại, xóa khi hợp lệ; `GET /api/vendor/services`, `POST .../{id}/submit`, `PATCH .../{id}/pause`, `.../{id}/resume`, `DELETE .../{id}`. | Có API | P0 |
| V06 | Tạo / chỉnh sửa dịch vụ — `/vendor/services/new`, `/vendor/services/:serviceId/edit` | Nội dung VI/EN, danh mục, vị trí, thời lượng, waiver và weather-sensitive; `POST /api/vendor/services`, `GET/PATCH .../{id}`. Các tab ảnh, option, chứng từ phải có đủ chức năng như mô tả dưới bảng. | Có API chính; đọc chứng từ một phần | P0 |
| V07 | Chi tiết / tiến trình duyệt dịch vụ — `/vendor/services/:serviceId` | Nội dung đã lưu, ảnh, trạng thái, lý do từ chối, option/ca và điều kiện xuất bản; `GET /api/vendor/services/{id}`, `.../{id}/options`, `.../{id}/slots`. Không dùng trang public để xem DRAFT/REJECTED. | Có API chính | P0 |
| V08 | Lịch dịch vụ / sức chứa — `/vendor/schedule` | Lịch ngày/tuần, chọn dịch vụ, booked/held/khả dụng và trạng thái ca; `GET /api/vendor/services/{id}/slots`. API hiện theo từng dịch vụ, chưa có lịch tổng toàn vendor hoặc lịch lặp hàng loạt. | Có API theo dịch vụ | P0 |
| V09 | Tạo / cấu hình ca — `/vendor/services/:serviceId/slots/new`, `.../slots/:slotId` | `POST/PATCH /api/vendor/services/{id}/slots...`; PERSON_LIMIT hoặc SHARED_CAPACITY_UNITS, sức chứa từng đơn vị, OPEN/CLOSED. PATCH hiện sửa status/capacity/units, không đổi date/start/end. | Có API; đổi giờ ca chưa có | P0 |

**Các tab bắt buộc trong V06**, cũng phải có trên mobile:

1. **Nội dung và an toàn:** form nghiệp vụ, bản xem trước, lưu nháp và gửi duyệt. Tùy chọn hỗ trợ phân loại qua `POST /api/ai/classifications/service`, rà soát văn bản qua `POST /api/ai/content-assessments`; kết quả là gợi ý để người dùng xem xét, không tự duyệt nội dung.
2. **Ảnh dịch vụ:** upload, xóa, đổi thứ tự bằng `POST /api/vendor/services/{serviceId}/images`, `DELETE .../images/{imageId}`, `PATCH .../images/reorder`. Đây là tab, không cần ba trang riêng.
3. **Lựa chọn bán:** `GET/POST /api/vendor/services/{id}/options`, `PATCH .../options/{optionId}`; SHARED/PRIVATE, PER_PERSON/PER_PACKAGE, giá và giới hạn khách/gói, kích hoạt/ngừng bán.
4. **Chứng từ an toàn dịch vụ:** upload qua `POST /api/vendor/services/{serviceId}/safety-documents`, xem trạng thái và lý do từ chối. Vendor chưa có GET riêng cho danh sách chứng từ này, ServiceResult cũng chưa trả danh sách; cần bổ sung để tải lại trang vẫn theo dõi được.

### 4.2. Vận hành, tài chính và liên lạc

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| V10 | Đơn / lịch khách đặt — `/vendor/orders` | Tab booking item và sub-order; `GET /api/vendor/bookings`, `/api/vendor/orders`. Booking có bộ lọc trạng thái; order hiện chủ yếu phân trang. | Có API cơ bản | P0 |
| V11 | Chi tiết đơn của vendor — `/vendor/orders/:subOrderId` | Ca, option, số lượng, khách tham gia/liên hệ, tình trạng thực hiện và từ chối item có lý do; `PATCH /api/vendor/bookings/{bookingItemId}/reject`. List DTO chưa đủ hồ sơ khách/chi tiết và chưa có GET detail vendor. Cần liên kết đúng bookingItemId với subOrderId. | Một phần | P0 |
| V12 | Check-in QR — `/vendor/check-in` | Camera, nhập token thủ công, xác minh và kết quả; `POST /api/vendor/checkin/verify`. Hiển thị hết hạn, đã dùng, sai vendor, sai trạng thái/khung giờ. Quét ảnh là công việc client, backend nhận token. | Có API | P0 |
| V13 | Chi tiết hoàn tiền của đơn vendor — `/vendor/orders/:subOrderId/refunds` | Xem phần hoàn liên quan đến doanh thu vendor. Chưa có API refund được giới hạn theo vendor; không gọi API admin hoặc dữ liệu khách để thay thế. | Thiếu API | P2 |
| V14 | Hộp thư — `/vendor/messages` | Danh sách hội thoại với khách, theo đơn/dịch vụ và unread. | Thiếu API | P2 |
| V15 | Hội thoại khách hàng — `/vendor/messages/:conversationId` | Tin nhắn, ảnh/tệp, lịch sử và trạng thái đọc. Có thể dùng cùng route/khung với V14. AI Smart Reply vẫn là đề xuất, không phải API đã có. | Thiếu API | P2 |
| V16 | Thông báo / cảnh báo an toàn — `/vendor/notifications` | `GET /api/notifications`, dẫn tới dịch vụ/ca/đơn; xem thời tiết của ca bằng API weather. Thiếu read/unread, push và hàng chờ cảnh báo riêng của vendor. | Một phần | P1 |
| V17 | Đánh giá / phản hồi khách — `/vendor/reviews` | Đọc đánh giá, lọc theo dịch vụ, trả lời và báo vi phạm. Chưa có API review/vendor reply. | Thiếu API | P2 |
| V18 | Đối soát / doanh thu — `/vendor/settlements` | Kỳ đối soát, gross, hoàn, hoa hồng, net và trạng thái; `GET /api/vendor/settlements`. Không đồng nhất số này với tiền đã chuyển ngân hàng. Dashboard/báo cáo doanh thu theo kỳ tùy chọn vẫn thiếu. | Có API đối soát | P1 |
| V19 | Chi tiết kỳ đối soát — `/vendor/settlements/:settlementId` | Các dòng sub-order, khoản hoàn/khấu trừ/hoa hồng và số phải trả; `GET /api/vendor/settlements/{id}`. | Có API | P1 |
| V20 | Yêu cầu chi trả — `/vendor/payouts` | Danh sách, tạo yêu cầu từ kỳ đủ điều kiện, ngân hàng nhận tiền. Có model payout request nhưng chưa có API. | Thiếu API | P2 |
| V21 | Chi tiết yêu cầu chi trả — `/vendor/payouts/:payoutId` | REQUESTED/APPROVED/PAID/REJECTED, chứng từ chuyển tiền và lý do từ chối. | Thiếu API | P2 |
| V22 | Khiếu nại liên quan — `/vendor/disputes` | Danh sách vụ việc thuộc vendor, trạng thái và hạn phản hồi. | Thiếu API | P2 |
| V23 | Chi tiết / phản hồi khiếu nại — `/vendor/disputes/:disputeId` | Nội dung khách phản ánh, gửi giải trình/chứng cứ, đọc kết luận admin. | Thiếu API | P2 |
| V24 | Khuyến mãi vendor — `/vendor/promotions` | Tạo, sửa, bật/tắt voucher, dịch vụ áp dụng và giới hạn sử dụng. Có giao diện mẫu, chưa có contract backend. | Thiếu API | P2 |
| V25 | Tài khoản người vận hành — `/vendor/account` | Hồ sơ user gắn vendor, locale, đăng xuất; `GET/PATCH /api/users/me`, API auth. Phân biệt user account với business profile V04. Không có quản lý đội ngũ nhiều nhân viên riêng. | Có API tài khoản cá nhân | P1 |
| V26 | Đổi mật khẩu — `/vendor/account/change-password` | `POST /api/users/me/change-password`; xử lý thu hồi phiên. Có thể là tab V25. | Có API | P1 |

**Điều hướng web vendor:** Tổng quan; Dịch vụ; Lịch/sức chứa; Đơn/check-in; Đối soát/chi trả; Tin nhắn; Đánh giá; Khiếu nại; Khuyến mãi; Hồ sơ/xác minh; Thông báo. Các mục thiếu API cần nằm trong kế hoạch hoàn thiện, không hiển thị như thao tác thật khi còn dùng mock.

## 5. Web — Admin

Admin cần quản trị cả vận hành và ngoại lệ, không chỉ các trang duyệt dịch vụ. Dùng tài khoản có role ADMIN; API chẩn đoán RBAC không phải chức năng quản trị người dùng.

### 5.1. Tài khoản, đối tác, catalog và an toàn

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| A01 | Tổng quan điều hành — `/admin` | Số đơn/doanh thu/vendor chờ duyệt/dịch vụ chờ duyệt/dispute/cảnh báo. `/api/admin/dashboard` chỉ kiểm tra truy cập; cần API KPI/overview. | Một phần | P1 |
| A02 | Người dùng — `/admin/users` | Tìm kiếm, lọc role/trạng thái khóa, phân trang; `GET /api/admin/users`. Hiện không có API gán role tùy ý hoặc tạo admin công khai. | Có API đọc/khóa | P1 |
| A03 | Chi tiết người dùng — `/admin/users/:userId` | `GET /api/admin/users/{id}`, `PATCH .../{id}/lock`, `.../{id}/unlock`, lý do khóa. Lịch sử đơn/vendor có thể cần endpoint liên kết bổ sung. | Có API chính | P1 |
| A04 | Danh sách / hàng chờ vendor — `/admin/vendors` | Lọc trạng thái xác minh và phân trang; `GET /api/admin/vendors`. | Có API | P0 |
| A05 | Chi tiết / duyệt vendor — `/admin/vendors/:vendorId` | Doanh nghiệp, ngân hàng, toàn bộ documents; `GET /api/admin/vendors/{id}`, `PATCH .../{id}/approve`, `.../{id}/reject`. Luồng duyệt documents đang thiếu dù approve yêu cầu document APPROVED. | Một phần, chặn onboarding đầy đủ | P0 |
| A06 | Danh mục trải nghiệm — `/admin/categories` | Cây root/child, tạo/sửa/ngừng hoạt động; `GET/POST /api/admin/categories`, `PATCH .../{id}`, `.../{id}/deactivate`. Dùng dialog tạo/sửa; không cần một trang cho mỗi thao tác. | Có API | P0 |
| A07 | Dịch vụ / hàng chờ duyệt — `/admin/services` | Danh sách theo status, mở hồ sơ kiểm duyệt; `GET /api/admin/services`. API trả list, chưa có tìm kiếm/phân trang đầy đủ. | Có API cơ bản | P0 |
| A08 | Chi tiết / duyệt dịch vụ và chứng từ — `/admin/services/:serviceId` | Nội dung, ảnh, giá, waiver, an toàn; `PATCH /api/admin/services/{id}/approve`, `.../{id}/reject`; `GET /api/admin/services/{serviceId}/safety-documents`, `PATCH .../{docId}/approve`, `.../{docId}/reject`. Chi tiết listing hiện lấy từ list, chưa có GET admin detail; phải bổ sung để deep link DRAFT/PENDING hoạt động độc lập. | Một phần | P0 |
| A09 | Cảnh báo thời tiết / an toàn — `/admin/weather-alerts` | Ca, mức cảnh báo, số khách bị ảnh hưởng và thông số biển; `GET /api/admin/weather-alerts`, `POST .../{evaluationId}/resolve`. Action hiện là CANCEL_AND_REFUND hoặc DISMISSED; không thiết kế nút đổi lịch như thao tác đã có. | Có API xử lý hiện tại | P1 |
| A10 | Quy tắc an toàn theo danh mục — `/admin/safety-rules` | Xem/sửa ngưỡng thời tiết/biển; `GET /api/admin/category-safety-rules`, `PATCH .../{categoryId}`. | Có API | P1 |

### 5.2. Đơn hàng, tiền, khiếu nại và chi trả

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| A11 | Đơn hàng toàn sàn — `/admin/orders` | Tìm theo khách/vendor/trạng thái/thời gian, mở master/sub-order. Chưa có GET danh sách order toàn sàn; `/api/orders` chỉ danh sách CUSTOMER. | Thiếu API | P1 |
| A12 | Chi tiết đơn toàn sàn — `/admin/orders/:orderId` | `GET /api/orders/{id}` và `GET /api/bookings/{id}` có kiểm tra admin; truy vấn giao dịch qua `/api/admin/payments?orderId=`. Thiếu timeline/liên kết dispute/khách và một số chi tiết vận hành. | Một phần | P1 |
| A13 | Giao dịch thanh toán — `/admin/payments` | Phân trang, lọc status/provider/orderId; `GET /api/admin/payments`. | Có API | P1 |
| A14 | Chi tiết giao dịch — `/admin/payments/:paymentId` | `GET /api/admin/payments/{id}`; trạng thái, mã cổng, số tiền và liên kết order. Chưa có API admin gửi lại capture/đối soát thủ công; chỉ cung cấp thao tác đọc đã có. | Có API đọc | P1 |
| A15 | Theo dõi hoàn tiền — `/admin/refunds` | Phân trang, lọc status/reason/subOrderId; `GET /api/admin/refunds`. Hoàn theo policy/worker, không thêm bước duyệt thủ công mặc định. | Có API đọc | P1 |
| A16 | Chi tiết hoàn tiền / ngoại lệ — `/admin/refunds/:refundId` | `GET /api/admin/refunds/{id}`; nguyên nhân PENDING/FAILED, mã thao tác/cổng và số tiền. Cần contract riêng nếu muốn retry hoặc xác nhận giao dịch ngoại lệ bằng thao tác admin. | Có API đọc; thao tác ngoại lệ thiếu | P1 |
| A17 | Hàng chờ khiếu nại — `/admin/disputes` | Danh sách theo status/reason và phân trang; `GET /api/admin/disputes`. | Có API | P1 |
| A18 | Chi tiết / phân xử khiếu nại — `/admin/disputes/:disputeId` | Mô tả, evidenceUrls, sub-order, tỷ lệ hoàn và ghi chú; `PATCH /api/admin/disputes/{id}/resolve`. Chưa có GET detail riêng hoặc thread giải trình; list có nội dung để xem ban đầu. | Một phần | P1 |
| A19 | Kỳ đối soát — `/admin/settlements` | Lọc vendor/status/from/to; `GET /api/admin/settlements`; tạo kỳ bằng `POST .../generate`. | Có API | P1 |
| A20 | Chi tiết / chốt kỳ đối soát — `/admin/settlements/:settlementId` | Dòng đơn, khấu trừ/hoa hồng/net; `GET /api/admin/settlements/{id}`, `PATCH .../{id}/finalize`. Form generate có thể là dialog A19. FINALIZED là chốt sổ, chưa phải PAID. | Có API chốt sổ | P1 |
| A21 | Yêu cầu chi trả / thực hiện chi trả — `/admin/payouts` | Danh sách và detail, duyệt/từ chối, xác nhận chuyển tiền/chứng từ. Chưa có API payout admin và chưa có endpoint chuyển settlement sang PAID theo bằng chứng chi trả. | Thiếu API | P2 |

### 5.3. Nội dung, AI và quản trị chung

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| A22 | Quản lý đánh giá — `/admin/reviews` | Review bị báo cáo, nội dung, nguồn đơn, xử lý ẩn/hiện theo quyền. Chưa có API review moderation; text assessment AI không tự sửa review. | Thiếu API | P2 |
| A23 | Khuyến mãi toàn sàn — `/admin/promotions` | Voucher, điều kiện, thời hạn, ngân sách/số lượt, phân biệt mã vendor và mã sàn. | Thiếu API | P2 |
| A24 | Thống kê / báo cáo — `/admin/reports` | Doanh thu, booking, hoàn, vendor, khoảng ngày và xuất dữ liệu. Chưa có API báo cáo/export; không lấy một trang list để suy ra tổng toàn sàn. | Thiếu API | P2 |
| A25 | Hàng chờ đánh giá AI — `/admin/ai/assessment-cases` | Case văn bản và rủi ro giao dịch, lọc status/limit; `GET /api/admin/ai/assessment-cases`. Không hiển thị nhãn AI như kết luận vi phạm/gian lận đã được xác nhận. | Có API | P1 |
| A26 | Chi tiết / xử lý case AI — `/admin/ai/assessment-cases/:caseId` | Evidence, model/rubric, tín hiệu backend, note; `POST /api/admin/ai/assessment-cases/{id}/resolve`. Tạo risk case từ order bằng `POST /api/admin/ai/risk-cases`; rà văn bản qua `/api/ai/content-assessments`. Thiếu GET case detail độc lập, hiện đọc từ queue. Resolve không khóa user hay sửa tiền/publication. | Một phần | P1 |
| A27 | Thông báo admin — `/admin/notifications` | `GET /api/notifications` cho user hiện tại. Read/unread và soạn thông báo hàng loạt chưa có. | Một phần | P1 |
| A28 | Nhật ký kiểm toán — `/admin/audit-logs` | Danh sách có phân trang; `GET /api/admin/audit-logs`. Bộ lọc actor/action/thời gian và export cần bổ sung. | Có API cơ bản | P1 |
| A29 | Chi tiết nhật ký — `/admin/audit-logs/:logId` | `GET /api/admin/audit-logs/{id}`; đối tượng, người thao tác, thời điểm và nội dung đã lưu. Có thể dùng drawer A28. | Có API | P1 |
| A30 | Cấu hình hệ thống — `/admin/settings` | Hoa hồng, policy, thanh toán/thông báo và tham số vận hành có thể chỉnh qua UI sau khi thiết kế contract. Hiện chủ yếu cấu hình server; không có API settings CRUD. Quy tắc thời tiết đã có trang A10 riêng. | Thiếu API | P2 |
| A31 | Tài khoản admin / bảo mật — `/admin/account` | `GET/PATCH /api/users/me`, `POST /api/users/me/change-password`, locale và logout. Dùng tab hồ sơ/bảo mật, không cần trang mới cho mọi thao tác. | Có API | P1 |

**Điều hướng web admin:** Tổng quan; Người dùng; Vendor; Dịch vụ/danh mục; An toàn; Đơn hàng; Thanh toán/hoàn tiền; Khiếu nại; Đối soát/chi trả; Đánh giá; Khuyến mãi; AI; Báo cáo; Nhật ký; Cấu hình; Tài khoản.

## 6. Mobile — Khách hàng và Vendor

Mobile dùng cùng API và quy tắc quyền với web. Mã C/V/G trong bảng liên kết chức năng, API, mức hỗ trợ và ưu tiên của các mục trên; không có API mobile riêng. Các bottom sheet và tab vẫn phải hoàn thành chức năng dù không là route độc lập.

### 6.1. App khách hàng — `mobile/`

| Mã | Màn hình cần có | Nội dung / đối chiếu web |
| --- | --- | --- |
| MC01 | Khởi động / phục hồi phiên | Đọc phiên đã lưu, refresh cookie/token theo contract, kiểm tra role, điều hướng; G06. Không bắt buộc tutorial nhiều bước. |
| MC02 | Đăng nhập / đăng ký / Google | G01; đăng ký khách hàng và chuyển sang xác minh email. |
| MC03 | Xác minh email OTP | G02; gửi lại mã và hiển thị giới hạn thử. |
| MC04 | Quên / đặt lại mật khẩu | G03; flow hai bước có thể cùng screen. |
| MC05 | Trang chủ | C01; danh mục, thời tiết, gợi ý và recently viewed thành các section. |
| MC06 | Tìm kiếm / khám phá gần tôi | C02; bộ lọc bottom sheet, smart search và vị trí theo quyền người dùng. |
| MC07 | Chi tiết trải nghiệm / chọn ca | C03; sheet chọn option, slot, người/gói, split nhóm và thông tin an toàn. Review và thời tiết là tab/section. |
| MC08 | So sánh dịch vụ | C04; chọn nhiều dịch vụ, xem kết quả phù hợp màn hình nhỏ. |
| MC09 | Yêu thích | C05. |
| MC10 | Giỏ trải nghiệm | C06; nhiều item, sửa lựa chọn, kiểm tra availability. |
| MC11 | Checkout / giữ chỗ | C07; countdown theo expiresAt, tổng tiền, điều khoản, mở cổng thanh toán. |
| MC12 | Kết quả thanh toán / quay lại app | C08; return/deep link hoặc browser callback → đọc lại trạng thái backend, xử lý đang chờ/thành công/thất bại/hết hạn. |
| MC13 | Đơn hàng / booking của tôi | C09; tab trạng thái và phục hồi đơn đang chờ thanh toán. |
| MC14 | Chi tiết đơn / sub-order | C10; hành động theo trạng thái, thanh toán, hoàn, hỗ trợ. |
| MC15 | Vé điện tử QR | C11; mỗi sub-order một vé, expiresAt và trạng thái đã check-in. Ảnh QR đã lưu offline không chứng minh vé còn hợp lệ. |
| MC16 | Xem trước hủy / xác nhận hủy | C12; có thể là sheet mở từ MC14. |
| MC17 | Theo dõi hoàn tiền | C13; phần của MC14 hoặc màn hình riêng. |
| MC18 | Khiếu nại của tôi | C14; thiếu API list khách. |
| MC19 | Tạo khiếu nại | C15; chọn sub-order, chứng cứ; quyền ảnh/camera nếu hỗ trợ upload. |
| MC20 | Chi tiết / bổ sung khiếu nại | C16; thiếu API detail/phản hồi khách. |
| MC21 | Viết / xem đánh giá của tôi | C17; mở từ đơn đủ điều kiện, cần backend review. |
| MC22 | Trợ lý AI | C18; lịch sử theo conversationId, card cần xác nhận rõ trước khi giữ chỗ. |
| MC23 | Danh sách / tạo lịch trình | C19; form ngân sách, thời gian, nhóm khách. |
| MC24 | Chi tiết / replan lịch trình | C20; hiển thị mục thay đổi, xung đột version và chuyển checkout khi muốn đặt. |
| MC25 | Hỗ trợ đơn hàng | C21; có thể là tab chat AI có orderId, không cần một chatbot khác. |
| MC26 | Danh sách hội thoại khách–vendor | C22; cần API chat. |
| MC27 | Chat với vendor | C23; cần API chat, không dùng `/api/assistant` thay cho nhắn tin người dùng. |
| MC28 | Thông báo | C24; dẫn tới đơn, refund, ca hoặc lịch trình; push/read-unread cần bổ sung. |
| MC29 | Tài khoản | C25; truy cập hồ sơ, đơn, cài đặt, hỗ trợ và logout. |
| MC30 | Sửa hồ sơ / đổi mật khẩu | C26/C27; hai màn hoặc hai tab, session sau đổi mật khẩu phải được xử lý. |
| MC31 | Hồ sơ vendor công khai | C28; cần API public vendor. |
| MC32 | Ưu đãi / áp dụng voucher | C29; có thể là sheet trong checkout, cần backend voucher. |
| MC33 | Đổi lịch thực tế | C30; cần API reschedule, độc lập với lịch trình AI. |
| MC34 | Cài đặt / chính sách / quyền thiết bị | G04/G05/G06; VI/EN, quyền vị trí, camera/ảnh, deep link, mất mạng và thử lại. Chỉ xin quyền khi người dùng mở chức năng cần quyền. |

**Bottom navigation đề xuất:** Trang chủ — Khám phá — Đơn hàng — Tin nhắn/AI — Tài khoản. Có thể để AI là nút nhanh và giữ Tin nhắn là tab riêng; thông báo/yêu thích truy cập từ header hoặc tài khoản.

### 6.2. App vendor — `vendor_mobile/`

| Mã | Màn hình cần có | Nội dung / đối chiếu web |
| --- | --- | --- |
| MV01 | Khởi động / kiểm tra phiên và role | G06; VENDOR mới đăng ký và hồ sơ chờ duyệt phải có điểm vào phù hợp. |
| MV02 | Đăng nhập / tạo tài khoản / Google | G01; tài khoản mới không tự có VENDOR, chuyển sang MV05 sau flow đăng ký. |
| MV03 | Xác minh email | G02. |
| MV04 | Quên / đặt lại mật khẩu | G03. |
| MV05 | Đăng ký vendor | V02; thông tin doanh nghiệp và ngân hàng, làm mới quyền sau đổi role. |
| MV06 | Giấy tờ / trạng thái xác minh | V03; chụp/chọn tài liệu, upload, theo dõi trạng thái, lý do từ chối. |
| MV07 | Tổng quan vận hành | V01; đơn sắp tới, cảnh báo và nút quét QR nhanh; KPI còn thiếu API. |
| MV08 | Hồ sơ doanh nghiệp / ngân hàng | V04; dữ liệu doanh nghiệp tách hồ sơ người đăng nhập. |
| MV09 | Danh sách dịch vụ | V05; trạng thái, pause/resume, gửi duyệt và mở editor. |
| MV10 | Tạo / sửa dịch vụ | V06; nội dung, option/giá, ảnh, waiver và chứng từ. Dùng các bước hoặc tab, không cắt bớt option/tồn khi lên mobile. |
| MV11 | Chi tiết / kết quả duyệt dịch vụ | V07; lý do từ chối, thông tin hiện tại, liên kết lịch. |
| MV12 | Chứng từ an toàn dịch vụ | Tab/tuyến con của V06; upload và xem lại trạng thái, cần GET vendor safety-documents bổ sung. |
| MV13 | Lịch / tồn chỗ | V08; lịch theo dịch vụ, booked/held/khả dụng. |
| MV14 | Tạo / cấu hình ca | V09; PERSON_LIMIT/SHARED_CAPACITY_UNITS, units, đóng/mở và bảo vệ tồn đã cam kết. |
| MV15 | Đơn / booking item | V10; bộ lọc đúng API và phân trang. |
| MV16 | Chi tiết đơn / từ chối item | V11; cần GET detail và thông tin khách; reject dùng bookingItemId. |
| MV17 | Quét QR / kết quả check-in | V12; camera, nhập token thủ công, trạng thái kiểm tra và retry theo lỗi. Có thể gộp kết quả vào cùng screen. |
| MV18 | Chi tiết hoàn tiền liên quan | V13; cần API refund vendor. |
| MV19 | Danh sách hội thoại | V14; cần API chat. |
| MV20 | Chat với khách | V15; cần API chat. |
| MV21 | Thông báo / cảnh báo an toàn | V16; không mượn API cảnh báo admin. |
| MV22 | Đánh giá / phản hồi | V17; cần API review. |
| MV23 | Đối soát / doanh thu | V18; không gọi số tiền FINALIZED là tiền đã nhận. |
| MV24 | Chi tiết đối soát | V19. |
| MV25 | Danh sách / tạo yêu cầu chi trả | V20; cần API payout. |
| MV26 | Chi tiết yêu cầu chi trả | V21; cần API payout/status/chứng từ. |
| MV27 | Khiếu nại liên quan | V22; cần API vendor disputes. |
| MV28 | Chi tiết / gửi giải trình | V23; cần API detail/phản hồi/upload chứng cứ. |
| MV29 | Voucher / khuyến mãi | V24; cần backend voucher. |
| MV30 | Tài khoản người vận hành / đổi mật khẩu | V25/V26; hồ sơ user, bảo mật, locale, logout. |
| MV31 | Cài đặt / chính sách / trạng thái lỗi | G04/G05/G06; VI/EN, quyền camera/ảnh, mất mạng, hết phiên, deep link vào đơn/ca. |

**Bottom navigation đề xuất:** Tổng quan — Dịch vụ — Lịch — Đơn — Tài khoản. Quét QR là hành động nổi bật từ Tổng quan/Đơn; Tin nhắn và Thông báo có điểm truy cập rõ ràng; tài chính/khiếu nại/đánh giá nằm trong menu bổ sung.

App khách và app vendor có thể tiếp tục tách riêng như repository hiện tại. Nếu sau này gộp một app, vẫn cần hai bộ navigation và backend phải hỗ trợ cơ chế role tương ứng; không giả định tài khoản hiện tại có hai role đồng thời.

## 7. Các khoảng trống phải xử lý để trang vận hành đầy đủ

Các API được nhắc dưới đây là **nhóm contract cần bổ sung hoặc thống nhất**, không phải endpoint đã triển khai. Mục 9 chỉ liệt kê endpoint hiện hữu.

| Nhóm còn thiếu / chưa khép kín | Trang bị ảnh hưởng | Công việc cần hoàn thiện |
| --- | --- | --- |
| Duyệt giấy tờ vendor | V03, A05, MV06 | Upload lưu document PENDING, `ApproveVendorUseCase` yêu cầu đủ BUSINESS_LICENSE/SAFETY_CERT ở APPROVED, nhưng chưa có API chuyển document sang APPROVED. Thống nhất duyệt documents trong lệnh duyệt toàn hồ sơ hoặc thêm thao tác duyệt documents; không thể chỉ làm nút approve UI. |
| Gửi lại hồ sơ vendor bị từ chối | V03, A05, MV06 | Bổ sung contract chỉnh sửa/nộp lại và chuyển REJECTED → PENDING có kiểm soát; GET/PATCH profile hiện không đủ luồng resubmit. |
| Chi tiết vận hành / liên kết ID của vendor | V11, MV16 | GET detail sub-order/booking item theo vendor, khách/liên hệ, option/participants, liên kết bookingItemId ↔ subOrderId, bộ lọc thời gian/trạng thái phù hợp. Không dùng GET booking của khách cho vendor khi quyền không cho phép. |
| Đọc lại chứng từ an toàn của vendor | V06, V07, MV12 | GET safety-documents theo dịch vụ thuộc vendor hoặc mở rộng DTO; hiện chỉ có upload vendor và GET admin. |
| Hồ sơ vendor công khai | C03, C28, MC31 | Public business profile đã lọc thông tin nhạy cảm, dịch vụ theo vendor và review; không lộ ngân hàng/documents qua API admin. |
| Review và trạng thái sau trải nghiệm | C03, C17, V17, A22, MC21, MV22 | API tạo/đọc review và trả lời vendor, moderation/upload ảnh; định nghĩa rõ điều kiện đủ để đánh giá và nếu cần chuyển CHECKED_IN → COMPLETED. Chưa có endpoint complete riêng hiện tại. |
| Chat khách–vendor | C22/C23, V14/V15, MC26/MC27, MV19/MV20 | List/detail conversation, gửi/đọc message, attachments, unread, cập nhật realtime hoặc polling, kiểm tra participant. Bảng conversations/messages không phải API chat đã có. |
| Theo dõi / giải trình dispute | C14/C16, V22/V23, A18, mobile tương ứng | List/detail theo khách/vendor, GET detail admin, upload chứng cứ và phản hồi; đảm bảo ownership và lịch sử xử lý. API hiện chỉ tạo dispute khách, list/resolve admin. |
| Voucher / khuyến mãi | C29, V24, A23 và mobile | CRUD đúng role, eligibility/preview, áp dụng vào order, hạn lượt dùng và xử lý cạnh tranh. Có discountAmount/model không đồng nghĩa mã giảm giá hoạt động. |
| Chi trả vendor | V20/V21, A21, MV25/MV26 | Tạo/đọc payout request, admin duyệt/từ chối, xác nhận PAID bằng giao dịch/chứng từ, chống chi trùng. Chốt settlement chưa khép kín trả tiền. |
| Dashboard / reports / export | V01, A01, A24 | Aggregate theo quyền và kỳ thời gian, phân biệt doanh thu/hoàn/hoa hồng/net/đã trả. API `/api/admin/dashboard` hiện không có KPI. |
| Admin đọc độc lập các đối tượng | A08, A11, A18, A26 | GET danh sách order toàn sàn và detail dịch vụ/dispute/AI case theo id để refresh/deep link hoạt động; không phụ thuộc record còn nằm trong list/filter hiện tại. |
| Thông báo và push | C24, V16, A27 và mobile | Read/unread, unread count, đăng ký token thiết bị và xử lý deep link; API notification hiện chỉ list. |
| Waiver, khách tham gia, biên nhận | C03/C07/C10, V11 và mobile | Public waiver có nội dung/version; chấp thuận theo item, thời điểm/actor; dữ liệu tham gia/liên hệ cần cho vận hành và endpoint biên nhận nếu muốn tải chứng từ. Không chỉ đặt checkbox client. |
| Đổi lịch thực tế | C30, A09 và mobile | Preview/confirm reschedule, giữ/nhả tồn đúng, kiểm tra giá/chênh lệch và trạng thái payment. AI replan và weather DISMISSED không đổi booking. |
| Cấu hình vận hành qua UI | A30 | Contract có quyền/audit cho commission/policy/settings nếu đưa vào phạm vi sản phẩm. Nếu tiếp tục quản lý bằng server config, không làm form lưu giả. |
| Thao tác giao dịch ngoại lệ | A14/A16 | Contract truy vấn cổng/retry có kiểm soát, trạng thái kết quả chưa rõ và chứng cứ xử lý; không thêm nút mark-success/mark-refunded chỉ để đóng vụ việc. |
| Danh sách hội thoại AI / tự động replan | C18/C20 và mobile | Có GET theo conversationId nhưng chưa list hội thoại AI; replan hiện theo yêu cầu, chưa có background tự đổi lịch hay push khi replan. Tách rõ chức năng đang có và phần bổ sung. |

### 7.1. Các quy tắc giao diện phải lấy từ contract hiện tại

- **Loại ID:** booking/holdId, bookingItemId, masterOrderId và subOrderId khác nhau. QR dùng subOrderId; reject vendor dùng bookingItemId; dispute dùng masterOrderId ở path và subOrderId trong body. Không suy ra ID từ tên màn hình.
- **Giá và tồn:** SHARED/PER_PERSON dùng quantity là số người; PRIVATE/PER_PACKAGE dùng quantity là số gói, participantsCount là số người mỗi gói. Nhóm có số khách/gói khác nhau gửi thành item riêng. `allowSplit=false` mặc định; UI chỉ bật khi khách đồng ý chia nhóm. PRIVATE dùng tồn SHARED_CAPACITY_UNITS phù hợp.
- **Hold:** countdown dựa vào `holdExpiresAt` backend trả; hết hạn hoặc đổi giá/slot thì đọc lại và yêu cầu khách xác nhận. Giỏ/lịch trình AI không giữ chỗ.
- **Thanh toán:** hiện tạo intent hỗ trợ VNPAY/PAYPAL; MOMO chưa cấu hình dù tồn tại webhook. Không hiển thị MoMo như lựa chọn dùng được. Thanh toán toàn master order; cổng có trang thanh toán bên ngoài, không cần xây lại form nhập thông tin ngân hàng/thẻ.
- **Return và webhook:** trang kết quả nhận khách quay lại, webhook nhận thông báo từ cổng. Query/deep link thành công không đủ để chuyển UI sang PAID; phải đọc trạng thái backend. Dùng Idempotency-Key ổn định cho cùng một thao tác tạo order/intent/capture/refund; không sinh thao tác tiền mới mỗi lần retry màn hình.
- **Hủy và hoàn:** lấy tỷ lệ/số tiền thực tế từ cancellation-preview và kết quả lệnh. `RefundPolicyEngine` hiện phân tầng 100%/70%/30%/0% theo mốc 48h/24h/2h, khác mô tả 24h cũ trong checklist. Cần đồng bộ policy hiển thị với code trước nghiệm thu; không hardcode mô tả cũ. Refund PENDING là đang xử lý, chỉ PROCESSED mới thể hiện hoàn xong.
- **Check-in và hoàn tất:** check-in cập nhật CHECKED_IN, không phải COMPLETED. Không tự đổi nhãn hoàn tất hoặc cho review chỉ vì quét QR thành công nếu contract chưa cho phép.
- **Đối soát và chi trả:** DRAFT → FINALIZED là tạo/chốt kỳ; PAID cần bước chi trả và chứng cứ riêng. Vendor đọc đối soát không có nghĩa có quyền thực hiện chi trả.
- **Thời tiết:** backend rule engine quyết định an toàn; action DISMISSED chỉ xử lý bản đánh giá cảnh báo. Không gọi đó là đổi lịch. API safety không thay thế việc kiểm tra lại tồn/giá khi đặt.
- **AI:** AI trả gợi ý và snapshot; các trạng thái NEEDS_INPUT/UNAVAILABLE cần UI yêu cầu bổ sung hoặc fallback. Replan dùng expectedVersion, không thay booking. Content/risk case resolve không tự duyệt dịch vụ, khóa tài khoản, cập nhật safety hoặc sửa tiền. AI vendor copilot/smart reply/business insights/provider quality còn là đề xuất, không cần thêm trang độc lập để giả lập các API chưa có.
- **Phân quyền:** route guard và nút ẩn/hiện dựa vào role/ownership, nhưng backend vẫn là nguồn kiểm tra quyền. Không giả định ADMIN được gọi mọi API CUSTOMER/VENDOR; dùng đúng endpoint admin khi có.
- **Phiên và mobile:** refresh hiện dựa vào cookie `refresh_token` HttpOnly, không có API refresh token body riêng cho mobile. Flutter cần HTTP client/cookie jar phù hợp và lưu access token theo nền tảng; kiểm tra return từ trình duyệt ngoài. Đổi/reset mật khẩu thu hồi phiên cũ.
- **Ngôn ngữ:** profile locale và Accept-Language cần thống nhất giữa web/mobile; đổi ngôn ngữ hội thoại AI phải xử lý conversation locale thay vì tiếp tục một conversation khác locale.

## 8. Luồng liên trang và thứ tự triển khai

### 8.1. Những luồng phải kiểm tra xuyên vai trò

1. **Mở bán:** G01/G02 → V02 → V03 → A05 → V06/V08/V09 → A08 → C02/C03. Đây là luồng phải giải quyết khoảng trống duyệt documents vendor trước khi gọi onboarding hoàn chỉnh.
2. **Đặt và thanh toán:** C03 → C06 → C07 → cổng VNPay/PayPal → C08 → C09/C10 → C11. Khách mobile dùng MC07 → MC10–MC15 với cùng dữ liệu.
3. **Thực hiện dịch vụ:** C11/MC15 → V12/MV17 → V10/V11 → A19/A20 → V18/V19. Chi trả tiếp tục qua A21/V20/V21 khi bổ sung API.
4. **Hủy/hoàn:** C10 → C12 → C13; phía admin A15/A16. Vendor reject từ V11/MV16 cũng phát sinh hoàn theo backend, không xác nhận tiền đã hoàn ngay sau reject.
5. **An toàn:** C03/C07 đọc safety → A09 xử lý cảnh báo → thông báo khách/vendor → C10/C13 khi hủy/hoàn. Đổi lịch chỉ dùng C30 sau khi có contract riêng.
6. **Khiếu nại:** C15 → A17/A18 → A15/A16 nếu có hoàn. C14/C16/V22/V23 bổ sung theo dõi và giải trình để khép kín hai phía.
7. **AI:** C18 hoặc C19/C20 → lựa chọn và xác nhận của khách → C07; A25/A26 hỗ trợ người duyệt, hành động nghiệp vụ thực hiện ở trang/API gốc.

### 8.2. Thứ tự triển khai đề xuất

- **Đợt 1 — P0:** xác thực/OTP/reset/role guard; đăng ký–duyệt vendor; danh mục và listing với option/ảnh/chứng từ; ca/tồn; khám phá, giỏ, hold, order/payment; chi tiết đơn, QR và vendor check-in. Hoàn thiện các khoảng trống P0 cùng giao diện, nhất là documents vendor và detail vận hành.
- **Đợt 2 — P1:** weather/rules/alert; hủy/hoàn/dispute admin; giao dịch admin; đối soát; tài khoản/notification; AI hiện có, nhật ký và admin order list/overview còn thiếu.
- **Đợt 3 — P2:** chat người dùng, review, voucher, payout, dispute hai phía, public vendor, reschedule, báo cáo và settings. Đây là phần còn thiếu để nghiệm thu đầy đủ các nhóm trong thiết kế hiện tại.

Không cần thêm admin mobile, trang dành cho webhook, trang cho mỗi API refresh/logout hay trang riêng cho từng tác vụ AI. Có thể gộp tab/dialog để giảm số route nhưng không bỏ thao tác, trạng thái hoặc quyền đã liệt kê.

## 9. Đối chiếu toàn bộ API hiện hữu với trang

Danh mục bên dưới lấy từ annotation controller hiện tại, bao gồm alias. Đây là kiểm kê mã nguồn, không phải kết quả gọi thử toàn bộ endpoint hoặc nghiệm thu thanh toán bên ngoài. Các nhóm thiếu API ở mục 7 không được đưa vào danh mục hiện hữu.

Các mã trang G/C/V/A được định nghĩa ở mục 2–5; mobile tương ứng theo mục 6. Alias không tạo thêm trang. API ngoài vai trò của một trang không được dùng chỉ vì có tên route giống nhau.

### 9.1. Tài khoản, xác thực và quyền

| Controller | API hiện hữu | Trang / cách sử dụng |
| --- | --- | --- |
| AuthenticationController | `POST /api/auth/oauth2/google` | G01 |
| AuthenticationController | `POST /api/auth/login` | G01 |
| AuthenticationController | `POST /api/auth/register` | G01 |
| AuthenticationController | `POST /api/auth/refresh` | G06 (phục hồi phiên nền) |
| AuthenticationController | `POST /api/auth/logout` | G04/C25/V25/A31 (hành động, không cần route) |
| AuthenticationController | `POST /api/auth/otp/send` | G02 |
| AuthenticationController | `POST /api/auth/otp/verify` | G02 |
| AuthenticationController | `POST /api/auth/forgot-password` | G03 |
| AuthenticationController | `POST /api/auth/reset-password` | G03 |
| UserController | `GET /api/users/me` | C25, V25, A31 |
| UserController | `PATCH /api/users/me` | G04, C26, V25, A31 |
| UserController | `POST /api/users/me/change-password` | C27, V26, A31 |
| AdminController | `GET /api/admin/dashboard` | A01 (chỉ kiểm tra quyền, chưa có KPI) |
| AuthorizationController | `GET /api/authorization/admin` | Không có trang sản phẩm; endpoint chẩn đoán profile dev/test |
| AuthorizationController | `GET /api/authorization/vendor` | Không có trang sản phẩm; endpoint chẩn đoán profile dev/test |
| AuthorizationController | `GET /api/authorization/user` | Không có trang sản phẩm; endpoint chẩn đoán profile dev/test |
| AuthorizationController | `GET /api/authorization/product-read` | Không có trang sản phẩm; endpoint chẩn đoán profile dev/test |

### 9.2. Người dùng admin, vendor và nhật ký

| Controller | API hiện hữu | Trang / cách sử dụng |
| --- | --- | --- |
| AdminUserController | `GET /api/admin/users` | A02 |
| AdminUserController | `GET /api/admin/users/{id}` | A03 |
| AdminUserController | `PATCH /api/admin/users/{id}/lock` | A03 |
| AdminUserController | `PATCH /api/admin/users/{id}/unlock` | A03 |
| AdminVendorController | `GET /api/admin/vendors` | A04 |
| AdminVendorController | `GET /api/admin/vendors/{id}` | A05 |
| AdminVendorController | `PATCH /api/admin/vendors/{id}/approve` | A05 |
| AdminVendorController | `PATCH /api/admin/vendors/{id}/reject` | A05 |
| VendorProfileController | `POST /api/vendor/profile` | V02 |
| VendorProfileController | `GET /api/vendor/profile` | V03/V04 |
| VendorProfileController | `PATCH /api/vendor/profile` | V04 |
| VendorProfileController | `POST /api/vendor/documents` | V03 |
| VendorProfileController | `GET /api/vendor/documents` | V03 |
| AdminAuditLogController | `GET /api/admin/audit-logs` | A28 |
| AdminAuditLogController | `GET /api/admin/audit-logs/{id}` | A29 |

### 9.3. Catalog, danh mục, dịch vụ, option và tồn

| Controller | API hiện hữu | Trang / cách sử dụng |
| --- | --- | --- |
| CatalogController | `GET /api/services` | C01/C02 |
| CatalogController | `GET /api/v1/catalog` | C01/C02; alias catalog/weather |
| CatalogController | `GET /api/services/{id}` | C03/C06/C10 |
| CatalogController | `GET /api/v1/catalog/{id}` | C03/C06/C10; alias catalog/weather |
| CategoryController | `GET /api/categories` | C01/C02, V06 |
| RecentlyViewedController | `GET /api/recently-viewed` | C01 |
| WishlistController | `POST /api/wishlists/{serviceId}` | C05, nút lưu ở C02/C03 |
| WishlistController | `DELETE /api/wishlists/{serviceId}` | C05, nút lưu ở C02/C03 |
| WishlistController | `GET /api/wishlists` | C05, nút lưu ở C02/C03 |
| PublicServiceSlotController | `GET /api/services/{id}/slots` | C03/C06/C07 |
| AdminCategoryController | `GET /api/admin/categories` | A06 |
| AdminCategoryController | `POST /api/admin/categories` | A06 |
| AdminCategoryController | `PATCH /api/admin/categories/{id}` | A06 |
| AdminCategoryController | `PATCH /api/admin/categories/{id}/deactivate` | A06 |
| VendorServiceController | `POST /api/vendor/services` | V06 |
| VendorServiceController | `GET /api/vendor/services` | V05 |
| VendorServiceController | `GET /api/vendor/services/{id}` | V06/V07 |
| VendorServiceController | `PATCH /api/vendor/services/{id}` | V06 |
| VendorServiceController | `POST /api/vendor/services/{id}/submit` | V05/V06/V07 |
| VendorServiceController | `PATCH /api/vendor/services/{id}/pause` | V05/V07 |
| VendorServiceController | `PATCH /api/vendor/services/{id}/resume` | V05/V07 |
| VendorServiceController | `DELETE /api/vendor/services/{id}` | V05/V07 |
| VendorServiceImageController | `POST /api/vendor/services/{serviceId}/images` | V06 |
| VendorServiceImageController | `DELETE /api/vendor/services/{serviceId}/images/{imageId}` | V06 |
| VendorServiceImageController | `PATCH /api/vendor/services/{serviceId}/images/reorder` | V06 |
| VendorServiceOptionController | `GET /api/vendor/services/{id}/options` | V06/V07 |
| VendorServiceOptionController | `POST /api/vendor/services/{id}/options` | V06/V07 |
| VendorServiceOptionController | `PATCH /api/vendor/services/{id}/options/{optionId}` | V06/V07 |
| VendorServiceSlotController | `GET /api/vendor/services/{id}/slots` | V08/V09 |
| VendorServiceSlotController | `POST /api/vendor/services/{id}/slots` | V09 |
| VendorServiceSlotController | `PATCH /api/vendor/services/{id}/slots/{slotId}` | V09 |
| VendorSafetyDocumentController | `POST /api/vendor/services/{serviceId}/safety-documents` | V06 |
| AdminServiceController | `GET /api/admin/services` | A07/A08 |
| AdminServiceController | `PATCH /api/admin/services/{id}/approve` | A08 |
| AdminServiceController | `PATCH /api/admin/services/{id}/reject` | A08 |
| AdminSafetyDocumentController | `GET /api/admin/services/{serviceId}/safety-documents` | A08 |
| AdminSafetyDocumentController | `PATCH /api/admin/services/{serviceId}/safety-documents/{docId}/approve` | A08 |
| AdminSafetyDocumentController | `PATCH /api/admin/services/{serviceId}/safety-documents/{docId}/reject` | A08 |

### 9.4. Booking, order, payment, refund và check-in

| Controller | API hiện hữu | Trang / cách sử dụng |
| --- | --- | --- |
| BookingController | `GET /api/bookings/{id}` | C07/C10, A12 |
| BookingController | `GET /api/bookings` | C09 |
| BookingController | `PATCH /api/bookings/{id}/cancel` | C12 |
| BookingHoldController | `POST /api/bookings/hold` | C07 |
| BookingHoldController | `POST /api/bookings/{holdId}/confirm` | C08 (sau thanh toán đủ) |
| BookingHoldController | `DELETE /api/bookings/hold/{holdId}` | C07/C08 (hủy hold hợp lệ) |
| VendorBookingController | `PATCH /api/vendor/bookings/{id}/reject` | V11 (id là bookingItemId) |
| VendorBookingController | `GET /api/vendor/bookings` | V10 |
| OrderController | `POST /api/orders` | C07 |
| OrderController | `GET /api/orders` | C09 |
| OrderController | `GET /api/orders/{id}` | C08/C10, A12 |
| OrderController | `POST /api/orders/{id}/refund-request` | C12 |
| OrderController | `GET /api/orders/{id}/refunds` | C10/C13 |
| OrderController | `GET /api/orders/{id}/cancellation-preview` | C12/C21 |
| VendorOrderController | `GET /api/vendor/orders` | V10/V11 |
| PaymentController | `POST /api/payments/{orderId}/create-intent` | C07, C08 (retry có idempotency) |
| PaymentController | `GET /api/payments/{orderId}/status` | C08/C10 |
| PaymentController | `GET /api/payments/{orderId}/payments` | C10 (alias status) |
| PaymentController | `GET /api/payments/detail/{paymentId}` | C08/C10 |
| PaymentController | `POST /api/payments/paypal/capture` | C08 (return PayPal) |
| PaymentController | `GET /api/payments/webhook/vnpay` | Không có trang; callback từ VNPay |
| PaymentController | `POST /api/payments/webhook/vnpay` | Không có trang; callback từ VNPay |
| PaymentController | `POST /api/payments/webhook/momo` | Không có trang; hiện từ chối vì MoMo chưa cấu hình |
| PaymentController | `POST /api/payments/webhook/paypal` | Không có trang; callback có chữ ký từ PayPal |
| PaymentController | `POST /api/payments/webhook/paypal/refund` | Không có trang; callback refund có chữ ký từ PayPal |
| InternalPaymentWebhookController | `POST /api/payments/webhook/internal/{provider}` | Không có trang sản phẩm; profile test, ADMIN + chữ ký |
| AdminPaymentController | `GET /api/admin/payments` | A13, A12 |
| AdminPaymentController | `GET /api/admin/payments/{id}` | A14 |
| AdminRefundController | `GET /api/admin/refunds` | A15 |
| AdminRefundController | `GET /api/admin/refunds/{id}` | A16 |
| CustomerBookingQrController | `POST /api/bookings/{id}/qr-code` | C11 (id ưu tiên subOrderId) |
| VendorCheckinController | `POST /api/vendor/checkin/verify` | V12 |

### 9.5. Khiếu nại, thông báo, thời tiết và đối soát

| Controller | API hiện hữu | Trang / cách sử dụng |
| --- | --- | --- |
| CustomerDisputeController | `POST /api/orders/{id}/disputes` | C15 |
| AdminDisputeController | `GET /api/admin/disputes` | A17/A18 |
| AdminDisputeController | `PATCH /api/admin/disputes/{id}/resolve` | A18 |
| NotificationController | `GET /api/notifications` | C24, V16, A27 |
| WeatherController | `GET /api/weather/current` | C01/C03/C07, V16, A09 |
| WeatherController | `GET /api/v1/weather/current` | C01/C03/C07, V16, A09; alias catalog/weather |
| WeatherController | `GET /api/weather/advance-safety-check` | C01/C03/C07, V16, A09 |
| WeatherController | `GET /api/v1/weather/advance-safety-check` | C01/C03/C07, V16, A09; alias catalog/weather |
| AdminCategorySafetyRuleController | `GET /api/admin/category-safety-rules` | A10 |
| AdminCategorySafetyRuleController | `PATCH /api/admin/category-safety-rules/{categoryId}` | A10 |
| AdminWeatherAlertController | `GET /api/admin/weather-alerts` | A09 |
| AdminWeatherAlertController | `POST /api/admin/weather-alerts/{evaluationId}/resolve` | A09 |
| AdminSettlementController | `POST /api/admin/settlements/generate` | A19 (form tạo kỳ) |
| AdminSettlementController | `GET /api/admin/settlements` | A19 |
| AdminSettlementController | `GET /api/admin/settlements/{id}` | A20 |
| AdminSettlementController | `PATCH /api/admin/settlements/{id}/finalize` | A20 |
| VendorSettlementController | `GET /api/vendor/settlements` | V18 |
| VendorSettlementController | `GET /api/vendor/settlements/{id}` | V19 |

### 9.6. AI và lịch trình

| Controller | API hiện hữu | Trang / cách sử dụng |
| --- | --- | --- |
| AssistantController | `POST /api/assistant/chat` | C18 |
| AssistantController | `POST /api/assistant/conversations/{id}/confirm` | C18 |
| AssistantController | `GET /api/assistant/conversations/{id}` | C18 |
| AssistantController | `GET /api/assistant/conversations/{id}/history` | C18 |
| CustomerAiController | `POST /api/ai/search` | C02 |
| CustomerAiController | `POST /api/ai/recommendations` | C01/C02 |
| CustomerAiController | `POST /api/ai/nearby` | C02 |
| CustomerAiController | `POST /api/ai/services/compare` | C04 |
| CustomerAiController | `POST /api/ai/weather` | C03/C07 |
| CustomerAiController | `GET /api/ai/review-summaries/{serviceId}` | C03 |
| CustomerAiController | `POST /api/ai/support` | C21/C18 |
| CustomerAiController | `POST /api/ai/itineraries` | C19 (tạo) |
| CustomerAiController | `GET /api/ai/itineraries` | C19 |
| CustomerAiController | `GET /api/ai/itineraries/{id}` | C20 (chi tiết) |
| CustomerAiController | `POST /api/ai/itineraries/{id}/replan` | C20 |
| AiAssessmentController | `POST /api/ai/content-assessments` | V06, A26 (gợi ý duyệt văn bản) |
| AiAssessmentController | `POST /api/ai/classifications/service` | V06 (gợi ý category) |
| AdminAiController | `POST /api/admin/ai/risk-cases` | A26 (tạo từ order) |
| AdminAiController | `GET /api/admin/ai/assessment-cases` | A25/A26 |
| AdminAiController | `POST /api/admin/ai/assessment-cases/{id}/resolve` | A26 |

## 10. Nguồn đối chiếu

- Controller và DTO trong [modules](../src/main/java/com/danasea/backend/modules), [authentication](../src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java) và [authorization](../src/main/java/com/danasea/backend/security/authorization/presentation).
- [SecurityConfig](../src/main/java/com/danasea/backend/configs/SecurityConfig.java), quyền method-level và kiểm tra ownership trong use case.
- [API checklist](danasea-api-tracking.md), [báo cáo API trước đó](API-completeness-review-2026-10-06.md), [AI implementation](AI/AI-IMPLEMENTATION.md). Khi tài liệu cũ khác mã nguồn, danh sách này ghi theo mã nguồn hiện tại và chỉ ra chỗ cần thống nhất.
- [Router React hiện tại](../../frontend/src/App.tsx), [customer mobile](../../mobile/lib/features), [vendor mobile](../../vendor_mobile/lib/features) và [vendor app routes](../../vendor_mobile/lib/main.dart).
- [Vendor web reference](../../frontend/docs/vendor_web/DANASEA_VENDOR_WEB_REVISED/README.md): bản HTML mẫu ghi rõ chưa nối backend; dùng để đối chiếu nhóm màn hình, không dùng làm bằng chứng vận hành.

**Giới hạn xác minh:** tài liệu được kiểm tra bằng inventory tĩnh, đối chiếu quyền/DTO/use case và tham chiếu giao diện. Chưa chạy thử E2E web/mobile hoặc test backend trong nhiệm vụ viết tài liệu này; “Có API” không phải cam kết production readiness hay nghiệm thu giao dịch thật.
