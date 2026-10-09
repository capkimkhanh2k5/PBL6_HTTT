# DANASEA — Master API & Test Checklist (EPIC-01 → EPIC-08)

**Cập nhật:** 09/10/2026 — đối chiếu controller trên nhánh `implement_admin_reports_dashboard`; bổ sung báo cáo, dashboard và kiểm chứng hồi quy tài chính, đối chiếu nhánh `implement_review_rating_system`; bổ sung API review/rating, kiểm duyệt và bảo vệ thao tác đồng thời,  nhánh `implement_password_reset_api`; bổ sung forgot/reset password, bảo vệ OTP dùng một lần và thu hồi phiên cũ.

**Quy ước:** Với mục API, `[x]` nghĩa là endpoint đã có trong controller; không đồng nghĩa đã kiểm chứng toàn bộ nghiệp vụ hoặc tích hợp cổng thanh toán thật. Với mục Test, hạ tầng và quyết định nghiệp vụ, giữ trạng thái checklist đã ghi nhận; `[ ]` là việc còn thiếu/chưa xác nhận. Lần cập nhật này không đánh dấu các test chưa xác nhận thành đã pass.

**Phạm vi kiểm kê:** 123 tổ hợp HTTP method/path từ 41 controller trong profile `test`, gồm 4 đường dẫn alias, 4 endpoint chẩn đoán ở profile `dev/test` và 1 webhook nội bộ chỉ ở `test`. Swagger/Actuator do thư viện cung cấp không nằm trong số API controller này. Các API cần hoàn thiện nghiệp vụ được ghi chú tại mục tương ứng; xem thêm [báo cáo rà soát](API-completeness-review-2026-10-06.md) và [kết quả sandbox](payment-sandbox-verification-2026-10-06.md).

---

## EPIC-01 · IAM

### Auth Module — API
- [x] POST /api/auth/register
- [x] POST /api/auth/login
- [x] POST /api/auth/oauth2/google
- [x] POST /api/auth/refresh
- [x] POST /api/auth/logout
- [x] POST /api/auth/otp/send
- [x] POST /api/auth/otp/verify
- [x] POST /api/auth/forgot-password
- [x] POST /api/auth/reset-password

### Auth Module — Test
- [x] RegisterUseCaseTest (thành công, email trùng, publish event)
- [x] LoginUseCaseTest (thành công, sai mật khẩu, user locked, email chưa verify)
- [x] GoogleOAuth2LoginUseCaseTest (login user cũ, auto register user mới, user locked, token invalid)
- [x] GoogleTokenVerifierAdapterTest (verify thành công, token rỗng, Google error, audience mismatch, email chưa verify, server 500)
- [x] GoogleOAuth2RequestTest (token fallback, priority, whitespace trim, null handling)
- [x] GoogleUserInfoTest (record mapping, optional fields)
- [x] RefreshTokenUseCaseTest (rotation, family revocation, hết hạn, user locked)
- [x] LogoutUseCaseTest
- [x] SendVerificationOtpUseCaseTest
- [x] VerifyOtpUseCaseTest (đúng, sai, hết hạn, vượt max attempts, one-time-use)
- [x] ForgotPasswordUseCaseTest (thành công, chống enumeration, user locked, event publishing)
- [x] ResetPasswordUseCaseTest (thành công, OTP sai, vượt max attempts, token hết hạn, tăng phiên bản session)
- [x] PasswordResetSecurityIntegrationTest (PostgreSQL 16/Redis thật: 17 ca kiểm tra OTP, HTTP response đồng nhất kể cả publisher lỗi, race reset/cấp mã/login/refresh, rollback, JWT cũ và cache stale)
- [x] OtpEmailConsumerTest (mail lỗi → DLQ)
- [x] PasswordResetEmailConsumerTest (gửi thành công, mail lỗi → DLQ)
- [x] AuthenticationControllerTest (login/register/refresh/logout/OTP/forgot-password/reset-password, cookie httpOnly)
- [x] RateLimitFilterIntegrationTest
- [x] Test Family Revocation persist thật qua DB sau khi fix noRollbackFor
- [x] Test X-Forwarded-For không bypass được rate limit (sau khi cấu hình forward-headers-strategy)
- [x] Test emailVerified đồng nhất giữa Login và Refresh

### Password reset — Bảo đảm nghiệp vụ
- [x] OTP riêng trong `password_reset_tokens`, hash SHA-256, hết hạn 15 phút; UUID do Hibernate sinh.
- [x] Lưu số lần sai trong DB; lần sai thứ 5 khóa token và commit trước khi trả lỗi.
- [x] Cấp mã/reset/login/refresh khóa cùng tài khoản; request đồng thời không dùng OTP hai lần hoặc tạo phiên cũ sau reset.
- [x] Reset tăng `session_version` và revoke mọi refresh token; JWT filter kiểm tra phiên bản trên dữ liệu tài khoản không qua cache.
- [x] Migration `V20__password_reset_security.sql` giữ dữ liệu cũ; JWT không có version được coi là 0 cho đến lần reset đầu tiên.

**Kiểm chứng 08/10/2026:** `./mvnw verify` thành công; 1.623 test, 0 failure/error, 135 skipped theo cấu hình suite. Cả 17 ca `PasswordResetSecurityIntegrationTest` chạy và pass với PostgreSQL 16/Redis thật; migration V20 áp dụng và schema Hibernate validate thành công. SMTP/publisher được mock trong kiểm thử, chưa phải kiểm tra gửi email ngoài hệ thống.

### RBAC — Hạ tầng
- [x] Role trong JWT claims + map GrantedAuthority (ROLE_*)
- [x] @EnableMethodSecurity + @PreAuthorize theo endpoint
- [x] CustomAccessDeniedHandler (JSON, không phải HTML)

### RBAC — Test
- [x] SecurityConfigIntegrationTest (403 role sai, 401 không token/hết hạn)
- [x] ServiceIntegrationE2ETest (login thật → gọi endpoint role-protected + cần userId)
- [x] AccessDeniedHandlerTest (response JSON đúng format)

### RBAC — API kiểm tra quyền (chỉ profile dev/test)
- [x] GET /api/authorization/admin
- [x] GET /api/authorization/vendor
- [x] GET /api/authorization/user
- [x] GET /api/authorization/product-read

### Admin quản lý tài khoản — API
- [x] GET /api/admin/users
- [x] GET /api/admin/users/{id}
- [x] PATCH /api/admin/users/{id}/lock
- [x] PATCH /api/admin/users/{id}/unlock
- [x] GET /api/admin/audit-logs (danh sách có phân trang)
- [x] GET /api/admin/audit-logs/{id} (chi tiết nhật ký)
- [x] GET /api/admin/dashboard (số liệu thật, bộ lọc ngày/vendor; giữ ADMIN_ACCESS_GRANTED)

### Admin quản lý tài khoản — Test
- [x] LockUserUseCaseTest (thành công, không tồn tại, đã lock rồi, self-lock chặn)
- [x] UnlockUserUseCaseTest
- [x] Test liên module: refresh token của user vừa bị lock thất bại NGAY (không đợi hết hạn)
- [x] AdminUserControllerTest (list/filter, 404, role sai → 403)
- [x] AuditLogServiceTest (ghi đúng field, không rollback hành động chính nếu audit lỗi)

### Thống kê, dashboard và báo cáo — API
- [x] GET /api/admin/reports/revenue?from=&to=&groupBy=&vendorId=
- [x] GET /api/admin/reports/bookings?from=&to=&groupBy=&vendorId=
- [x] GET /api/admin/reports/vendors?from=&to=&vendorId=
- [x] GET /api/admin/reports/export?type=&from=&to=&groupBy=&vendorId= (CSV)
- [x] GET /api/vendor/dashboard?from=&to=
- [x] GET /api/vendor/reports/revenue?from=&to=&groupBy=
- [x] GET /api/vendor/reports/bookings?from=&to=&groupBy=
- [x] GET /api/vendor/reports/export?type=&from=&to=&groupBy= (CSV)

### Thống kê, dashboard và báo cáo — Quy ước và kiểm chứng
- Vendor được lấy từ tài khoản đăng nhập; `vendorId` từ client không thay đổi phạm vi vendor.
- `groupBy=day|week|quarter|year`, mặc định `day`; khoảng ngày tối đa 3660 ngày, múi giờ `Asia/Ho_Chi_Minh`, cận cuối SQL là đầu ngày sau `to` và không bao gồm cận này.
- Tiền thu ghi nhận theo `payments.paid_at`; payment đã chuyển `REFUNDED` vẫn giữ sự kiện thu gốc. Refund chỉ ghi nhận `PROCESSED` theo `processed_at`.
- Hoa hồng hoàn toàn bộ về 0; hoàn một phần điều chỉnh theo tổng refund và tỷ lệ hoa hồng của đơn. Payout/hoa hồng kỳ có thể âm khi điều chỉnh giao dịch kỳ trước.
- Booking là cohort đơn tạo trong khoảng ngày với trạng thái hiện tại; lý do hủy đếm từng sub-order một lần. `unknownCancellationCount` biểu thị dữ liệu không xác định lý do; compensation không tự coi là hủy đơn.
- CSV UTF-8 BOM, escape RFC 4180 và trung hòa công thức trong tên vendor; số tiền âm vẫn là số.
- Migration `V22__report_financial_events.sql`: `payments.paid_at`, `sub_orders.cancellation_reason`, index tài chính. Thời điểm thanh toán của dữ liệu cũ là ước lượng từ timestamp có sẵn; chưa thể coi là đối soát lịch sử chính xác với cổng.
- [x] ReportFinancialIntegrationTest: PostgreSQL thật, refund qua kỳ, pending/failed, phân bổ tiền, ranh giới ngày, tính nhất quán dashboard/vendor, CSV và lý do hủy.
- [x] ReportSecurityAdversarialIntegrationTest: phân quyền và vendor isolation qua Spring Security.
- Kiểm chứng 09/10/2026 tại worktree này: `clean verify` BUILD SUCCESS, 1875 test, 0 failures/errors, 135 skipped; riêng report 288 test không có test bỏ qua. Chưa áp dụng migration lên database ứng dụng đang chạy.
- Chi tiết hợp đồng và ví dụ: [reports-dashboard-contract.md](reports-dashboard-contract.md).

### User Module — API
- [x] GET /api/users/me
- [x] PATCH /api/users/me
- [x] POST /api/users/me/change-password

### User Module — Test
- [x] UpdateProfileUseCaseTest (thành công, chống mass assignment)
- [x] ChangePasswordUseCaseTest (thành công, sai mật khẩu cũ, trùng mật khẩu mới, revoke toàn bộ token)
- [x] UserControllerTest: xác nhận token cũ vô hiệu ngay sau đổi mật khẩu (test ở tầng controller/integration)

---

## EPIC-02 · Vendor & Catalog

### Vendor Profile — API
- [x] POST /api/vendor/profile
- [x] GET /api/vendor/profile
- [x] PATCH /api/vendor/profile
- [x] POST /api/vendor/documents
- [x] GET /api/vendor/documents
- [x] GET /api/admin/vendors?status=
- [x] GET /api/admin/vendors/{id} (trả kèm toàn bộ document để Admin đọc)
- [x] PATCH /api/admin/vendors/{id}/approve — Rule đã chốt: Admin đọc toàn bộ document trong hồ sơ, ấn Approve nếu thỏa mãn (không approve từng document riêng lẻ)
- [x] PATCH /api/admin/vendors/{id}/reject

### Vendor Profile — Test
- [x] RegisterVendorProfileUseCaseTest (5 case)
- [x] UpdateVendorProfileUseCaseTest (6 case)
- [x] UploadVendorDocumentUseCaseTest (9 case)
- [x] VendorProfileControllerTest (12 case)
- [x] ApproveVendorUseCaseTest / RejectVendorUseCaseTest
- [x] Test approve khi vendor không ở trạng thái PENDING → chặn
- [x] Test GET /api/admin/vendors/{id} trả đủ danh sách document cho Admin đọc trước khi approve

### Categories — API
- [x] GET /api/categories
- [x] GET /api/admin/categories
- [x] POST /api/admin/categories
- [x] PATCH /api/admin/categories/{id}
- [x] PATCH /api/admin/categories/{id}/deactivate

### Categories — Test
- [x] CreateCategoryUseCaseTest (root, child, parent không tồn tại, slug trùng, vòng lặp phân cấp)
- [x] GetCategoryTreeUseCaseTest (cây nhiều tầng, inactive ẩn/hiện đúng theo role)
- [x] DeactivateCategoryUseCaseTest (chặn khi có service active)
- [x] CategoryHierarchyAdversarialUseCaseTest
- [x] CategoryDeactivationAndBusinessConstraintUseCaseTest

### Vendor Services — API
- [x] POST /api/vendor/services
- [x] GET /api/vendor/services
- [x] GET /api/vendor/services/{id}
- [x] PATCH /api/vendor/services/{id}
- [x] POST /api/vendor/services/{id}/submit
- [x] PATCH /api/vendor/services/{id}/pause
- [x] PATCH /api/vendor/services/{id}/resume
- [x] DELETE /api/vendor/services/{id}
- [x] GET /api/admin/services?status=
- [x] PATCH /api/admin/services/{id}/approve (đã gọi canPublish() safety guard)
- [x] PATCH /api/admin/services/{id}/reject
- [x] GET /api/vendor/services/{id}/options (Xem lựa chọn đặt của dịch vụ)
- [x] POST /api/vendor/services/{id}/options (Tạo lựa chọn đặt SHARED hoặc PRIVATE)
- [x] PATCH /api/vendor/services/{id}/options/{optionId} (Sửa hoặc ngừng bán lựa chọn)
- [x] GET /api/vendor/services/{id}/slots (Xem ca và cấu hình tồn chỗ)
- [x] POST /api/vendor/services/{id}/slots (Tạo ca: PERSON_LIMIT hoặc SHARED_CAPACITY_UNITS)
- [x] PATCH /api/vendor/services/{id}/slots/{slotId} (Sửa ca, hạn mức, bảo vệ cam kết hoặc đóng/mở)

### Vendor Services — Test
- [x] CreateServiceUseCaseTest (6 case)
- [x] SubmitServiceForReviewUseCaseTest (4 case)
- [x] UpdateServiceUseCaseTest (3 case)
- [x] ApproveServiceUseCaseTest (3 case)
- [x] RejectServiceUseCaseTest (3 case)
- [x] DeleteServiceUseCaseTest (3 case)
- [x] ServiceControllerTest (13 case RBAC)
- [x] ServiceOptionsAndInventoryAllocationIntegrationTest: vendor khác không được đọc/tạo/sửa option; booking phải chọn option hoạt động thuộc dịch vụ. Không dùng tên test class chưa tồn tại.
- [x] ServiceOptionsAndInventoryAllocationIntegrationTest: ca trùng bị chặn; sửa tồn tính cả hold còn hiệu lực; giữ nguyên bookedCount; khóa ca với checkout; đơn vị thuê riêng không được đổi sức chứa trong thời gian cam kết. PATCH không nhận ngày/giờ nên không đổi được lịch ca.

### Service Images & Safety Documents — API
- [x] POST /api/vendor/services/{serviceId}/images
- [x] DELETE /api/vendor/services/{serviceId}/images/{imageId}
- [x] PATCH /api/vendor/services/{serviceId}/images/reorder
- [x] POST /api/vendor/services/{serviceId}/safety-documents
- [x] GET /api/admin/services/{serviceId}/safety-documents
- [x] PATCH /api/admin/services/{serviceId}/safety-documents/{docId}/approve
- [x] PATCH /api/admin/services/{serviceId}/safety-documents/{docId}/reject

### Service Images & Safety Documents — Test
- [x] UploadServiceImageUseCaseTest (7 case: IDOR, file type, max 10 ảnh)
- [x] ReorderServiceImagesUseCaseTest (6 case)
- [x] UploadSafetyDocumentUseCaseTest (5 case)
- [x] ApproveSafetyDocumentUseCaseTest (9 case, Publish Guard)

### Public Catalog + Wishlist + Recently Viewed — API
- [x] GET /api/services
- [x] GET /api/services/{id} (trả chi tiết dịch vụ kèm danh sách options để khách chọn)
- [x] GET /api/services/{id}/slots?optionId=&from=&to=&quantity=&allowSplit= (Khách xem ca khả dụng theo lựa chọn đặt)
- [x] GET /api/v1/catalog (alias của GET /api/services)
- [x] GET /api/v1/catalog/{id} (alias của GET /api/services/{id})
- [x] POST /api/wishlists/{serviceId}
- [x] DELETE /api/wishlists/{serviceId}
- [x] GET /api/wishlists
- [x] GET /api/recently-viewed

### Public Catalog + Wishlist + Recently Viewed — Test
- [x] SearchServicesUseCaseTest
- [x] GetServiceDetailUseCaseTest (atomic increment, DRAFT→404, trả options hoạt động)
- [x] RecordRecentlyViewedUseCaseTest (upsert userId/sessionId)
- [x] WishlistUseCaseTest (idempotent add/remove)
- [x] CatalogControllerTest
- [x] ServiceSpecificationsTest, MapperTest, RepositoryAdapterTest (hạ tầng)

---

## EPIC-03 · Booking Engine & Inventory Locking

### Quyết định nghiệp vụ
- [x] Cơ chế lock: dùng phương án thuận tiện/đúng chuẩn nhất — đề xuất Redis SETNX + Lua script (atomic, tận dụng hạ tầng Redis đã có)
- [x] Hủy trễ: mất toàn bộ tiền dịch vụ (không có khái niệm đặt cọc)
- [x] 1 booking có thể chứa nhiều dịch vụ
- [x] Booking bắt buộc thanh toán toàn bộ mới được xác nhận (không đặt cọc)

### Quyết định nghiệp vụ
- [x] Mốc thời gian cụ thể coi là "hủy trễ" (trước giờ dịch vụ 24h)
- [x] Hủy SỚM hơn mốc đó thì hoàn toàn bộ
- [x] Vendor có API từ chối booking item ở trạng thái CONFIRMED; hiện tạo yêu cầu hoàn 100% với trạng thái PENDING và nhả chỗ của item đó
- [ ] Hoàn thiện gửi yêu cầu refund tới cổng gốc, thông báo khách hàng và gợi ý vendor khác còn slot để khách tự chọn lại

### API
- [x] POST /api/bookings/hold (giữ chỗ nhiều dịch vụ/lựa chọn trong 1 lần, hỗ trợ SHARED và PRIVATE, TTL 10-15 phút)
- [x] POST /api/bookings/{holdId}/confirm (chỉ xác nhận sau khi thanh toán toàn bộ thành công)
- [x] DELETE /api/bookings/hold/{holdId}
- [x] GET /api/bookings/{id}
- [x] GET /api/bookings
- [x] GET /api/vendor/bookings
- [x] PATCH /api/bookings/{id}/cancel (áp rule mất toàn bộ tiền nếu trễ)
- [x] PATCH /api/vendor/bookings/{id}/reject (id là bookingItemId; chỉ từ chối item CONFIRMED thuộc vendor)
- [x] Scheduled job dọn Redis hold hết hạn + rollback inventory

### Test
- [x] CreateBookingHoldUseCaseTest (nhiều dịch vụ, slot hết, TTL, giới hạn tổng quantity); validate option/max pax và allocation được kiểm tra bằng ServiceOptionsAndInventoryAllocationIntegrationTest.
- [x] ConfirmBookingUseCaseTest (chỉ confirm khi đã thanh toán đủ, hold hết hạn → lỗi, sai owner → 403)
- [x] Test race condition đa luồng thật (2 request giữ slot cuối cùng, dùng Lua script atomic - ServiceOptionsAndInventoryAllocationIntegrationTest)
- [x] CancelBookingUseCaseTest (đúng mốc thời gian mất toàn bộ tiền)
- [x] BookingExpiryJobTest (rollback đúng, không rollback nhầm hold đã confirm)
- [x] BookingControllerTest (IDOR: khách A/B, vendor không liên quan)
- [x] ServiceOptionsAndInventoryAllocationIntegrationTest (33 test với PostgreSQL 16 + Redis thật): 10 tình huống cơ bản và 23 hồi quy cho nhiều item cùng ca, giữ cả nhóm, split có đồng ý, rollback toàn bộ hold, booked/held tách biệt, ca đã bắt đầu/ca trùng, option ngừng bán/sai dịch vụ, participantsCount âm, vendor ownership, trả tồn private khi từ chối, trả tồn idempotent, hold hết hạn, đơn vị private không bị mở thêm chỗ, checkout cạnh tranh với sửa sức chứa, và hủy hold cạnh tranh với xác nhận thanh toán, và item chưa xác nhận không được trừ tồn đã đặt của booking khác.
- [x] BookingHoldControllerTest: request cũ không có allowSplit vẫn hoạt động; allowSplit=true được truyền đúng; participantsCount âm bị chặn trước use case.
- [x] RefundProcessingServiceTest: hoàn tiền của item có allocation gọi cơ chế trả tồn theo bookingItemId đúng một lần; dữ liệu legacy không có bookingItemId giữ nhánh cũ.

### Hợp đồng booking và tồn dùng chung

**Kiểm chứng ngày 2026-10-08:** `./mvnw verify` → BUILD SUCCESS; 1.624 tests, 0 failures, 0 errors, 135 skipped. Bao gồm 33 test tồn/option tích hợp trên PostgreSQL 16 + Redis 7, 11 test BookingHoldController, 13 test RefundProcessingService, và 2 test migration (database mới + nâng V18→V19). Guard inventory xác nhận 122 endpoint. `git diff --check` đạt. Đây là bằng chứng backend/test, không thay thế nghiệm thu thanh toán sandbox qua giao diện.

- `slotId` là ca/chuyến; `optionId` xác định SHARED/PER_PERSON hoặc PRIVATE/PER_PACKAGE. Có nhiều option hoạt động thì phải gửi optionId; ngừng bán tất cả option không được fallback về giá service.
- `quantity` là số người với SHARED, số gói với PRIVATE. `participantsCount` nếu có phải >= 1 và là số khách mỗi gói trong item; các gói có số khách khác nhau gửi thành các item riêng.
- `allowSplit` mặc định false (kể cả request cũ hoặc null): cả nhóm ở một đơn vị đủ chỗ; chỉ chia qua nhiều đơn vị khi true. API availability dùng cùng cờ để tính `bookable`.
- PERSON_LIMIT dành cho khách ghép. PRIVATE chỉ được đặt trên SHARED_CAPACITY_UNITS; giữ toàn bộ đơn vị đủ sức chứa theo giới hạn gói, không dựa vào số khách thực tế để chọn đơn vị nhỏ hơn.
- Mọi item cùng slot trong một hold dùng chung kế hoạch phân bổ tích lũy. Không đủ tồn ở bất kỳ item nào thì không giữ một phần.
- Checkout, xác nhận và sửa tồn khóa cùng bản ghi slot. Hủy hold và xác nhận cùng khóa booking; Redis hold chỉ được nhả sau khi transaction hủy commit. `bookedCount` chỉ tính booking đã xác nhận; hold được tính riêng, bỏ qua hold hết hạn.
- Hủy/từ chối/hoàn tiền trả đúng allocation theo bookingItemId; cờ `capacity_released` ngăn trả lặp. Các sub-order legacy không có bookingItemId dùng nhánh trả quantity cũ.
- V19 bổ sung cờ trả tồn, đối soát lại bộ đếm shared units theo allocation đã xác nhận, và unique(service_id, date, start_time). PostgreSqlMigrationIntegrationTest kiểm tra database mới và nâng V18→V19: bỏ bộ đếm hold hết hạn/item đã từ chối nhưng giữ nguyên allocation còn hiệu lực. Ca được kiểm tra theo Asia/Ho_Chi_Minh.

Ví dụ hai gói riêng có số khách khác nhau trong cùng ca:
```json
{
  "items": [
    {"slotId": "<slotId>", "optionId": "<privateOptionId>", "quantity": 1, "participantsCount": 4},
    {"slotId": "<slotId>", "optionId": "<privateOptionId>", "quantity": 1, "participantsCount": 7}
  ]
}
```
Hai item phải nhận hai đơn vị khác nhau và tính giá hai gói. Nhóm ghép 4 người khi đơn vị 1 còn 2 chỗ sẽ được đưa nguyên nhóm vào đơn vị 2 nếu đủ chỗ.


---

## EPIC-04 · Order & Payment

### Quyết định nghiệp vụ
- [x] Không hỗ trợ đặt cọc — bắt buộc thanh toán toàn bộ
- [x] Thanh toán 1 lần cho toàn bộ Master Order (không thanh toán riêng từng Sub-Order)
- [x] Hoàn tiền tự động (không cần Admin duyệt thủ công)
- [x] Cổng thanh toán: Trong nước (VNPay, MoMo) & Quốc tế (PayPal Sandbox). Bỏ SePay, thay bằng PayPal.

### API
- [x] POST /api/orders (tạo Master Order từ booking, chờ thanh toán)
- [x] GET /api/orders/{id}
- [x] GET /api/orders/{id}/cancellation-preview (xem trước số tiền hoàn theo policy)
- [x] GET /api/orders
- [x] GET /api/vendor/orders
- [x] POST /api/payments/{orderId}/create-intent (thanh toán toàn bộ Master Order 1 lần)
- [x] POST /api/payments/paypal/capture (capture đơn PayPal sau khi khách duyệt thanh toán)
- [x] GET /api/payments/webhook/vnpay (IPN callback dạng GET từ VNPay)
- [x] POST /api/payments/webhook/vnpay (IPN callback dạng POST từ VNPay)
- [x] POST /api/payments/webhook/momo (route còn tồn tại nhưng từ chối xử lý vì chưa tích hợp MoMo)
- [x] POST /api/payments/webhook/paypal (webhook chuẩn PayPal với transmission headers)
- [x] POST /api/payments/webhook/internal/{provider} (chỉ profile `test`, cần ADMIN và HMAC; không tồn tại ở dev/production)
- [x] POST /api/payments/webhook/paypal/refund (xác minh đủ 5 transmission headers; không dùng HMAC/fallback nội bộ)
- [x] POST /api/orders/{id}/refund-request (yêu cầu hoàn theo policy; đã hoàn thiện persistence, idempotency và provider transaction ID)
- [x] GET /api/orders/{id}/refunds (danh sách yêu cầu hoàn tiền theo đơn hàng cho khách sở hữu)
- [x] GET /api/payments/{orderId}/status (truy vấn trạng thái thanh toán theo đơn hàng cho khách)
- [x] GET /api/payments/{orderId}/payments (alias danh sách thanh toán theo đơn)
- [x] GET /api/payments/detail/{paymentId} (chi tiết thanh toán cho khách)
- [x] GET /api/admin/payments (danh sách giao dịch thanh toán toàn sàn cho admin, lọc theo status/provider/orderId, có phân trang)
- [x] GET /api/admin/payments/{id} (chi tiết giao dịch thanh toán cho admin)
- [x] GET /api/admin/refunds (danh sách yêu cầu hoàn tiền toàn sàn cho admin, lọc theo status/reason/subOrderId, có phân trang)
- [x] GET /api/admin/refunds/{id} (chi tiết yêu cầu hoàn tiền cho admin)

**Thay đổi callback:** bỏ hai endpoint mô phỏng `/webhook/vnpay/refund` và `/webhook/momo/refund`. VNPay refund được worker xác minh qua response API/querydr có checksum; PayPal refund dùng callback chuẩn có chữ ký. Hợp đồng cổng được đối chiếu với [PayPal Payments v2](https://developer.paypal.com/api/payments/v2/captures-refund) và [VNPay querydr/refund](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html).

### Order & Payment — Việc cần hoàn thiện
- [x] Nối API create-intent với payment lưu trong DB và xử lý lặp theo Idempotency-Key để webhook tra cứu được
- [x] Commit paymentId trước khi gọi gateway; retry sau timeout giữ cùng ID, PayPal dùng PayPal-Request-Id
- [x] Chặn replay payment SUCCESS/FAILED/REFUNDED; commit FAILED khi intent hết hạn trước khi trả lỗi
- [x] Chỉ cho tạo intent với VNPAY/PAYPAL; MOMO/SEPAY bị từ chối, không chuyển ngầm sang PayPal
- [x] PayPal create-intent không trả token/URL mô phỏng khi API thật lỗi; trả HTTP 502
- [x] Kiểm tra sandbox ngày 06/10/2026: PayPal OAuth/order/replay thành công, VNPay mở được trang chọn ngân hàng; chưa chứng minh giao dịch hoàn tất và callback thật
- [x] Docker Compose dev truyền cấu hình PayPal/VNPay vào backend; rebuild ngày 07/10/2026, Flyway đến V17, health UP; refund PayPal thiếu header trả 400, callback refund mô phỏng đã bỏ trả 404
- [x] Lưu refund PENDING trước; worker chỉ gọi cổng sau commit, dùng provider/capture ID gốc và mã thao tác riêng cho mỗi refund
- [x] Kiểm tra hợp đồng callback bằng test: PayPal xác minh transmission headers, order/capture ID, số tiền/tiền tệ và trạng thái; VNPay IPN xác minh HMAC SHA512, provider và số tiền. Chưa có bằng chứng callback sandbox thật
- [x] Hoàn thiện PayPal capture (POST /api/payments/paypal/capture) với pessimistic locking Payment -> MasterOrder, tách biệt provider_order_id (Flyway V16) và capture_id
- [x] Flyway V17 lưu số tiền/tiền tệ cổng gốc, ngày VNPay gốc, mã thao tác capture/refund và thời điểm gửi; commit mã thao tác trước khi gọi cổng
- [x] Timeout capture/refund giữ PENDING và marker GATEWAY_TIMEOUT_AWAITING_VERIFICATION; retry/worker truy vấn cổng, không gửi lại lệnh tài chính khi kết quả chưa rõ hoặc query thất bại
- [x] PayPal refund PENDING không chuyển PROCESSED; callback chuẩn /webhook/paypal cũng nhận refund và đối chiếu API refund trước khi cập nhật
- [x] VNPay refund dùng TxnRef/ngày giao dịch gốc, loại 02/03 đúng full/partial và kiểm tra checksum response; querydr loại 01 không được coi là hoàn tiền thành công
- [x] Worker capture tự đối soát các thao tác đã gửi; worker refund có backoff, timeout không tiêu hao retry để chuyển FAILED sai
- [x] Refund thành công không nhả inventory lần nữa nếu cancellation/vendor rejection đã nhả chỗ
- [ ] Đăng ký URL HTTPS callback PayPal và APP_PAYPAL_WEBHOOK_ID; nghiệm thu capture/refund và callback trên sandbox thật
- [ ] Đối soát thủ công trường hợp mất provider refund ID và không có callback chứa invoice_id/mã thao tác; tiếp tục PENDING, không tự gửi lại refund
- [ ] Bản ghi payment cũ thiếu gateway amount/currency/ngày VNPay cần khôi phục dữ liệu từ giao dịch cổng gốc; không suy đoán bằng tỷ giá hiện tại
- [x] Kiểm tra owner trước khi trả order đã có ở cả nhánh bookingId và idempotency key; sai owner trả 403 trước khi đọc sub-order

### Test
- [x] CreateOrderUseCaseTest (split đúng Sub-Order theo vendor, chờ thanh toán toàn bộ)
- [x] OrderOwnershipHttpRegressionTest (5 ca MockMvc standalone: owner tạo/replay được; user khác bị chặn ở booking mới và hai nhánh replay)
- [x] OrderSplittingTest (RabbitMQ message đúng số lượng vendor)
- [x] PaymentWebhookHmacTest (chữ ký sai → từ chối)
- [x] PaymentWebhookIdempotencyTest (trùng transactionId → xử lý 1 lần)
- [x] PaymentWebhookTest (thành công → confirm Master Order + mọi Sub-Order cùng lúc; thất bại → rollback inventory toàn bộ)
- [x] PayPalWebhookVerificationTest (test controller với service mock; chưa chứng minh chữ ký/callback PayPal thật)
- [x] PaymentIntentPersistenceIntegrationTest (PostgreSQL/Flyway thật, gateway mock: timeout/retry, concurrency, terminal state, expiry, webhook nội bộ và HTTP 502/409)
- [x] PayPalPaymentAdapterTest (HTTP mock: provider idempotency, lỗi OAuth/order, thiếu approval URL, chặn provider chưa hỗ trợ và xác minh payload webhook dạng object)
- [x] RequestRefundUseCaseTest (policy, owner và lưu yêu cầu PENDING; không gọi cổng trong transaction tạo refund)
- [x] RefundProcessingServiceTest (12 case: COMPLETED/PENDING/FAILED/UNKNOWN, query lỗi không resend, FX snapshot, hạn mức, callback idempotent và không nhả inventory hai lần)
- [x] AdminRefundControllerTest (truy vấn danh sách và chi tiết refund cho admin)
- [x] RefundPersistenceIntegrationTest (H2, gateway mock: commit PENDING rồi worker xử lý, idempotency, đồng bộ trạng thái và nhả inventory)
- [x] PaymentGatewaySafetyTest (14 case hồi quy: money/currency/order/provider/terminal guards, capture timeout và đối soát, refund callback không fallback HMAC)
- [x] PaymentCaptureConcurrencyIntegrationTest (PostgreSQL/Flyway V1–V17, gateway mock: capture vs webhook đúng một lần; mã thao tác đọc được từ connection khác trước external request; timeout bền vững; refund rollback không gọi cổng)
- [x] InternalPaymentWebhookProfileTest (5 case: chỉ tồn tại ở test; dev/default/prod/production không có bean)
- [x] VNPayPaymentAdapterTest (HTTP mock, checksum response: reference/date/type/request ID gốc, trạng thái PENDING, không nhầm thanh toán với refund, không nhầm hai partial refund)
- [x] Kiểm tra toàn bộ backend ngày 07/10/2026: 1.569 test, 0 failure, 0 error, 135 skip; không thay thế nghiệm thu sandbox thật
- [x] GetCustomerOrdersUseCaseTest (phân trang, IDOR boundary, mapping SubOrders)
- [x] GetCancellationPreviewUseCaseTest (tính toán preview hoàn tiền, chính sách phân tầng theo giờ)
- [x] OrderControllerTest (IDOR customer/vendor)

---

## EPIC-05 · Weather & Safety Rules

### Quyết định nghiệp vụ
- [x] Nguồn Open-Meteo cho weather/marine; cache của API current có TTL 30 phút
- [x] Đã có cảnh báo, admin resolve và cơ chế tự hủy khi cảnh báo RED không được xử lý; yêu cầu refund được lưu PENDING, chưa đồng nghĩa đã hoàn tiền qua cổng

### API
- [x] GET /api/weather/current?lat=&lng=
- [x] GET /api/v1/weather/current?lat=&lng= (alias của GET /api/weather/current)
- [x] GET /api/weather/advance-safety-check (kiểm tra theo slotId hoặc danh mục, tọa độ và ngày/giờ)
- [x] GET /api/v1/weather/advance-safety-check (alias của GET /api/weather/advance-safety-check)
- [x] GET /api/admin/category-safety-rules
- [x] PATCH /api/admin/category-safety-rules/{categoryId}
- [x] GET /api/admin/weather-alerts
- [x] POST /api/admin/weather-alerts/{evaluationId}/resolve (evaluationId là ID bản đánh giá an toàn; action CANCEL_AND_REFUND hoặc DISMISSED)

### Jobs và việc cần hoàn thiện
- [x] Job thu thập weather/marine và job giám sát an toàn các slot sắp diễn ra
- [x] Nối refund PENDING từ cảnh báo với luồng thực thi tại cổng thanh toán
- [ ] Hoàn thiện đổi lịch thực tế; action DISMISSED hiện chỉ cập nhật bản đánh giá, không chuyển booking sang slot mới

### Test
- [ ] WeatherServiceAdapterTest (xử lý timeout/lỗi API bên thứ 3)
- [ ] WeatherRuleEngineTest (vượt ngưỡng chặn, không vượt cho phép, không weather_sensitive bỏ qua)
- [ ] WeatherAlertJobTest (không cảnh báo trùng)

---

## EPIC-06 · AI Smart Assistant

### Phạm vi hiện tại
- [x] Tư vấn qua tool calling, tạo confirmation card và xác nhận để tạo booking hold; thanh toán theo luồng order/payment
- [x] Có rate limit theo IP/user qua Redis
- [ ] Xác nhận giới hạn chi phí gọi LLM và kiểm chứng giới hạn dưới tải thực

### API
- [x] POST /api/assistant/chat
- [x] POST /api/assistant/conversations/{id}/confirm (tạo hold từ confirmation card)
- [x] GET /api/assistant/conversations/{id}
- [x] GET /api/assistant/conversations/{id}/history

### AI — Việc cần hoàn thiện
- [x] Kiểm tra owner ở API đọc hội thoại và lịch sử; sai owner trả 403, UUID không tồn tại trả 404
- [x] Ràng buộc owner, conversationId và confirmation card trước khi tạo hold; chặn ghép card của hội thoại khác

### Test
- [x] AssistantOwnershipHttpRegressionTest (10 ca MockMvc standalone với service/use case thật và repository mock: đọc/confirm đúng owner, chặn truy cập chéo và trả 404 khi không tồn tại)
- [x] Nghiệm thu quyền sở hữu ngày 08/10/2026: 38 test tập trung pass; 11 probe bổ sung ngoài repository dùng JWT/Spring Security/PostgreSQL thật pass (card store và lệnh tạo hold được mock). Toàn bộ `mvn verify` chạy 1.587 test, 0 failure, 2 error, 135 skip: hai error ở fixture Dispute đặt chuyến 09:00 ngày chạy, đã tái hiện khi chạy trước 09:00; chưa xác nhận full suite xanh
- [ ] AssistantChatUseCaseTest (tool calling đúng function)
- [ ] Test rate limit endpoint chat
- [ ] Test fallback khi LLM lỗi/timeout

---

## EPIC-07 · Operations / Settlement / Review

### Review & Rating — API
- [x] POST /api/sub-orders/{id}/reviews (chủ đơn COMPLETED; một review mỗi sub-order)
- [x] PUT /api/sub-orders/{id}/reviews (sửa qua sub-order, trong 7 ngày)
- [x] PUT /api/reviews/{id} (chỉ tác giả, trong 7 ngày)
- [x] GET /api/services/{id}/reviews (công khai, phân trang, chỉ review visible; không trả metadata nội bộ)
- [x] GET /api/vendor/reviews (chỉ review của vendor hiện tại)
- [x] POST /api/vendor/reviews/{id}/reply (chỉ vendor sở hữu review)
- [x] GET /api/admin/reviews (lọc service/vendor/flag/visibility, phân trang)
- [x] PATCH /api/admin/reviews/{id}/visibility (chỉ ADMIN; ghi chú kiểm duyệt riêng)
- [x] POST /api/reviews/{id}/flag (yêu cầu đăng nhập; review visible; response chỉ gồm id/isFlagged)

### Review & Rating — Bảo đảm nghiệp vụ / Test
- [x] Unique constraint `uq_reviews_sub_order` bảo vệ một review mỗi trải nghiệm; trùng trả 409.
- [x] Khóa review khi sửa/reply/flag/ẩn/hiện; sửa qua sub-order tra cứu ID bằng scalar trước khi khóa.
- [x] Khóa vendor trước tổng hợp điểm; tính điểm/count và badge chỉ từ review visible, trong cùng transaction.
- [x] Tối đa 5 ảnh HTTP(S), mỗi URL tối đa 2048 ký tự; JSON lưu trong TEXT.
- [x] Validation dùng khóa i18n Anh/Việt; flag không đọc được nội dung review bị ẩn.
- [x] ReviewUseCaseTest, ReviewControllerTest, ReviewRatingIntegrationTest.
- [x] ReviewConcurrencyIntegrationTest (PostgreSQL 16 thật: 11 ca unique, tổng điểm, cạnh tranh edit/reply/hide, alias, privacy, ảnh, ownership và badge).
- [x] Migration `V21__review_enhancements.sql` thay V18 của nhánh review để tránh trùng số với nhánh slot; giữ V18/V19 cho slot và V20 cho password reset.

**Kiểm chứng 08/10/2026:** `./mvnw clean verify` thành công; 1.621 test, 0 failure/error, 135 skipped theo cấu hình suite. Cả 11 test PostgreSQL thật chạy và pass; Flyway áp dụng V21 và Hibernate validate schema thành công.

Chính sách hiện tại: khách sửa review trong 7 ngày; chưa cung cấp API xóa. Admin ẩn/hiện để kiểm duyệt; ẩn loại review khỏi điểm tổng hợp, hiện tính lại điểm.

### Khiếu nại — Tranh chấp
- [x] POST /api/orders/{id}/disputes
- [x] GET /api/admin/disputes
- [x] PATCH /api/admin/disputes/{id}/resolve (yêu cầu refund từ dispute được lưu PENDING; cần luồng thực thi tại cổng)
- [ ] Test: khiếu nại trùng, resolve cập nhật đúng Order/Refund

### QR Check-in
- [x] POST /api/bookings/{id}/qr-code
- [x] POST /api/vendor/checkin/verify
- [ ] Test: check-in trùng, QR hết hạn/không hợp lệ

### Settlement
- [x] GET /api/admin/settlements
- [x] POST /api/admin/settlements/generate
- [x] GET /api/admin/settlements/{id}
- [x] PATCH /api/admin/settlements/{id}/finalize (chốt kỳ FINALIZED; chưa phải xác nhận chi trả PAID)
- [x] GET /api/vendor/settlements
- [x] GET /api/vendor/settlements/{id}
- [ ] Test: tính hoa hồng đúng, không tính trùng đơn hoàn/hủy

### Thông báo — API
- [x] GET /api/notifications (danh sách thông báo của user hiện tại, có phân trang)

### Chính sách Hoàn/Hủy
- [ ] Rule engine % hoàn tiền theo mốc thời gian (dùng chung logic "mất toàn bộ nếu hủy trễ" đã chốt ở EPIC-03)
- [ ] Test: mốc thời gian biên tính đúng

### Regression
- [ ] Chạy lại toàn bộ test suite Sprint 1-3

---

## EPIC-08 · Cross-Platform Client & Deployment
- [ ] CI/CD GitHub Actions build→test→deploy GCP/AWS
- [ ] Build thử Android APK (Flutter)
- [ ] Hardening toàn hệ thống (rà lại toàn bộ nợ kỹ thuật đã tích lũy)
- [ ] Test: pipeline CI/CD chạy end-to-end trên staging

---


# ĐỀ XUẤT

1. **Quản lý lịch và tồn chỗ — đã có API** Lịch, option và tồn dùng chung đã được triển khai trong EPIC-02/03; các mục API bên dưới đã nằm trong inventory, không còn là API thiếu. Tạo lịch lặp, đồng bộ tồn từ kênh ngoài và điều phối phương tiện thực tế nằm ngoài đợt triển khai này.
   - `GET /api/services/{id}/slots`
   - `GET/POST /api/vendor/services/{id}/slots`
   - `PATCH /api/vendor/services/{id}/slots/{slotId}`

2. **Thống kê và báo cáo** Đã có API admin/vendor, dashboard thật, nhóm ngày/tuần/quý/năm và CSV trên nhánh `implement_admin_reports_dashboard`. Hợp đồng và giới hạn dữ liệu lịch sử được ghi tại mục Thống kê ở trên; nghiệm thu runtime theo kết quả kiểm chứng của worktree này.

3. **Đánh giá sau trải nghiệm** Đã triển khai 9 endpoint và kiểm thử nghiệp vụ/đồng thời; xem checklist Review & Rating tại EPIC-07.

4. **Chi trả vendor** `FINALIZED` hiện chưa chứng minh vendor đã nhận tiền.
   - `POST/GET /api/vendor/payout-requests`
   - API Admin duyệt và xác nhận chi trả.
   - Cấu hình hoa hồng thay cho tỷ lệ mặc định cố định.

5. **Theo dõi và quản trị giao dịch** Khách cần biết hoàn tiền đang xử lý hay đã hoàn tất; Admin cần tìm giao dịch toàn sàn.
   - `GET /api/orders/{id}/payments`
   - `GET /api/orders/{id}/refunds`
   - `GET /api/admin/orders`, `/payments`, `/refunds`

6. **Phục hồi tài khoản và xác nhận an toàn.**
   - `POST /api/auth/forgot-password`
   - `POST /api/auth/reset-password`
   - Mở rộng service detail và checkout để khách đọc, xác nhận waiver; lưu nội dung/version và thời điểm xác nhận.

Sau các nhóm trên, nên bổ sung **đổi lịch do thời tiết**, **voucher**, **nhắn tin khách–vendor**, **theo dõi/phản hồi khiếu nại**, **read/unread thông báo** và **biên nhận thanh toán**. Đây đều là những phần đã được nêu trong phạm vi đề tài hoặc giúp khép kín luồng sử dụng.
