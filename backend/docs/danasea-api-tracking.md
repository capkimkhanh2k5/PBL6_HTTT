# DANASEA — Master API & Test Checklist (EPIC-01 → EPIC-08)

**Cập nhật:** 10/10/2026 — đồng bộ các API đã triển khai với nhánh main và giữ đầy đủ nghiệp vụ của từng module.

**Phạm vi kiểm kê:** Các tổ hợp HTTP method/path được đối chiếu tự động với controller trong profile `test` qua `BackendApplicationTests`; inventory mới nhất được xuất tại `backend/target/test-artifacts/api-endpoints.txt`. Swagger/Actuator do thư viện cung cấp không thuộc inventory này. Các số lượng trong ghi nhận nghiệm thu bên dưới thuộc snapshot được nêu tại thời điểm kiểm chứng.

**Quy ước:** Với mục API, `[x]` nghĩa là endpoint đã có trong controller; không đồng nghĩa đã kiểm chứng toàn bộ nghiệp vụ hoặc tích hợp cổng thanh toán thật. Với mục Test, hạ tầng và quyết định nghiệp vụ, giữ trạng thái checklist đã ghi nhận; `[ ]` là việc còn thiếu/chưa xác nhận. Lần cập nhật này không đánh dấu các test chưa xác nhận thành đã pass.

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

### Vendor Public Profile — API
- [x] GET /api/vendors/{id} (hồ sơ công khai, không trả thông tin tài khoản ngân hàng/thuế)
- [x] GET /api/vendors/{id}/services (dịch vụ đã công khai của vendor, có phân trang và sắp xếp)

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
- [x] GET /api/orders/{id}/receipt (chủ đơn/admin; JSON hoặc `format=pdf`; yêu cầu bản ghi thanh toán thành công, vẫn tải được sau hoàn thành/hoàn tiền)
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
- [x] GET /api/orders/{id}/payments (lịch sử intent/giao dịch, chỉ chủ đơn hoặc admin)
- [x] GET /api/orders/{id}/refunds (danh sách yêu cầu hoàn tiền theo đơn hàng cho chủ đơn hoặc admin)
- [x] GET /api/payments/{orderId}/status (truy vấn trạng thái thanh toán theo đơn hàng cho khách)
- [x] GET /api/payments/{orderId}/payments (alias danh sách thanh toán theo đơn)
- [x] GET /api/payments/detail/{paymentId} (chi tiết thanh toán cho khách)
- [x] GET /api/admin/orders (danh sách đơn hàng toàn sàn cho admin, lọc theo status/paymentStatus/vendorId/customerId/thời gian, có phân trang)
- [x] GET /api/admin/payments (danh sách giao dịch thanh toán toàn sàn cho admin, lọc theo status/provider/orderId/vendorId/customerId/thời gian, có phân trang)
- [x] GET /api/admin/payments/{id} (chi tiết giao dịch thanh toán cho admin)
- [x] GET /api/admin/refunds (danh sách yêu cầu hoàn tiền toàn sàn cho admin, lọc theo status/reason/subOrderId/orderId/provider/vendorId/customerId/thời gian, có phân trang)
- [x] GET /api/admin/refunds/{id} (chi tiết yêu cầu hoàn tiền cho admin)
- [x] GET /api/admin/discount-codes (danh sách mã khuyến mãi toàn sàn cho admin, lọc theo code/scope/vendorId/isActive)
- [x] GET /api/admin/discount-codes/{id} (chi tiết mã giảm giá cho admin)
- [x] POST /api/admin/discount-codes (admin tạo mã giảm giá toàn sàn hoặc tài trợ)
- [x] PATCH /api/admin/discount-codes/{id} (admin cập nhật trạng thái/thông tin mã giảm giá)
- [x] GET /api/vendor/discount-codes (vendor xem danh sách mã giảm giá thuộc phạm vi của mình)
- [x] GET /api/vendor/discount-codes/{id} (chi tiết mã giảm giá thuộc vendor)
- [x] POST /api/vendor/discount-codes (vendor tạo mã giảm giá giới hạn phạm vi dịch vụ/cửa hàng của mình)
- [x] PATCH /api/vendor/discount-codes/{id} (vendor cập nhật mã giảm giá của mình, chống IDOR)
- [x] POST /api/checkout/discount-preview (khách hàng kiểm tra voucher trước khi thanh toán, tính toán phân bổ dự kiến)

### Discount Management — Hợp đồng nghiệp vụ
- Giá backend quyết định; `MasterOrder.totalAmount` là tổng thực trả, bằng tổng `SubOrder.finalAmount`; `discountAmount` là phần giảm riêng.
- Phân bổ trên item đủ điều kiện theo tỷ trọng subtotal và Largest Remainder; tổng phần giảm item bằng mức giảm toàn đơn.
- VENDOR tài trợ: cơ sở hoa hồng = subtotal - vendorDiscount. PLATFORM tài trợ: cơ sở hoa hồng không giảm; vendor vẫn nhận tiền theo cơ sở đó.
- Tạo order khóa booking rồi khóa voucher; kiểm tra lại quota toàn mã, từng khách, thời hạn và trạng thái sau khi giữ khóa. Chỉnh sửa voucher cũng khóa cùng bản ghi.
- Tạo order giữ lượt bằng redemption; thanh toán thành công giữ nguyên lượt. Hủy đơn chưa thanh toán hoặc hết hạn xóa reservation và trả lượt đúng một lần trong cùng transaction; lỗi rollback toàn bộ.
- Vendor chỉ sửa voucher VENDOR do mình tài trợ; mã PLATFORM tài trợ cho dịch vụ của vendor chỉ được admin sửa. serviceId bị giới hạn phải thuộc vendor tương ứng.
- Hoàn tiền dùng finalAmount cho các nhánh customer cancel, vendor reject, dispute và weather; không tạo refund tiền mặt bằng 0. Hoàn một phần đảo cơ sở hoa hồng và trợ giá theo tỷ lệ cashRefund/finalAmount; settlement ghi cashRefund thực tế.
- Chặn phần trăm >100, khoảng ngày không hợp lệ và cấu hình tài trợ sai phạm vi. Preview không nhận booking đã hết hạn/không còn HOLD.
- Tổng thực trả phải >0; voucher làm toàn đơn miễn phí bị từ chối với DISCOUNT_ZERO_PAYABLE_UNSUPPORTED trước khi giữ lượt. Checkout miễn phí nằm ngoài phạm vi hiện tại.
- Flyway V24 bổ sung schema, backfill finalAmount/commissionBasis và customerId của redemption; không dùng V21 vì phiên bản đó đã được các tính năng khác sử dụng.

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
- [x] DiscountAllocationEngineTest (8 test case: phân bổ theo tỷ trọng subtotal, Largest Remainder xử lý số lẻ, chặn quá maxDiscount, voucher cố định và %, cô lập scope vendor/service)
- [x] DiscountControllersTest (4 unit test dùng Mockito: mapping response, phân giải vendor, admin tạo mã và preview; không thay thế kiểm tra HTTP RBAC)
- [x] DiscountManagementIntegrationTest (6 unit test dùng Mockito: tạo mã admin/vendor, chặn mã trùng/vendor khác, preview và tạo đơn có snapshot giảm giá)
- [x] DiscountConcurrencyIntegrationTest (12 ca với PostgreSQL 16 thật/Flyway V24 và Redis Testcontainers: quota toàn mã/từng khách, sửa hoặc tắt voucher trong checkout, intent dùng tiền net, replay, hủy/hết hạn lặp, bảo vệ paid, rollback, chặn checkout miễn phí, hoàn net khi cancel/vendor reject; chỉ gateway/publisher/commission được mock)
- [x] DiscountConfigurationTest (6 unit test: cấm vendor sửa mã sàn tài trợ, cấm service của vendor khác, phần trăm, ngày hiệu lực và phạm vi tài trợ)
- [x] DiscountRefundRegressionTest (4 unit test: hoàn tiền net, ghi cashRefund/tổng refund, đảo trợ giá khi hoàn một phần, nhận diện hoàn đủ số tiền net)
- [x] DiscountLocalizationTest (3 unit test: thông báo VI/EN, booking hết hạn, preview từ chối tổng thực trả bằng 0)
- [x] Kiểm tra toàn backend ngày 09/10/2026 trên nhánh này: `./mvnw clean verify` BUILD SUCCESS; Maven báo 1.707 test, 0 failure, 0 error, 135 skip; PostgreSQL 16/Flyway V24 và schema validate pass. Gateway trong test khuyến mãi được mock, chưa thay thế nghiệm thu giao dịch voucher trên sandbox thật.
- [x] OrderExpiryEventListenerTest (4 unit test: hủy khi pending, giữ nguyên paid, không có đơn, trả reservation khi hết hạn)
- [x] SettlementCalculationTest (15 unit test: tính settlement, loại trừ, hoa hồng và snapshot giảm giá; trường hợp đồng tài trợ ở mức engine, API hiện chỉ có PLATFORM hoặc VENDOR)

### Quản trị giao dịch — hợp đồng đối soát ngày 09/10/2026

**Kiểm chứng 09/10/2026:** `./mvnw clean verify` → BUILD SUCCESS; 1.711 test cases, 1576 thực chạy, 135 skipped, 0 failures, 0 errors. Trong đó 29 ca `AdminTransactionIntegrationTest` trên PostgreSQL 16/Flyway/Redis 7 riêng và 57 ca tập trung reconciliation/adapter/job. Kiểm kê runtime: 126 method/path; migration mới V23 đã được kiểm tra trên DB mới và nâng từ V20. Gateway được mock; sandbox thật chưa được chạy trong lần này.

- Ba danh sách `/api/admin/orders`, `/api/admin/payments`, `/api/admin/refunds` dùng bộ lọc động, chạy với PostgreSQL khi bỏ trống ngày hoặc chỉ có một cận. Ngày bao gồm cả hai biên, so sánh theo instant; `from > to` trả 400. Lọc vendor dùng `EXISTS` để không nhân bản đơn nhiều sub-order.
- Query cổng chạy ngoài transaction áp dụng kết quả. Sau query, khóa payment rồi master order, đọc lại trạng thái và đối chiếu intent đã lưu. Webhook/capture thắng trước thì đối soát no-op, không ghi đè trạng thái cuối.
- Chỉ nhận `SUCCESS` khi ID giao dịch, số tiền và tiền tệ cổng hợp lệ. VNPay phải có checksum/merchant đúng, `querydr` thành công, TxnRef khớp và TransactionType=01. Response refund 02/03 không chứng minh thanh toán thành công.
- Timeout, UNKNOWN, trạng thái VNPay đảo/nghi ngờ/hoàn trả không tự chuyển payment thành FAILED; local `expiresAt` không chứng minh chưa thu tiền. Query chưa rõ lưu `GATEWAY_QUERY_AWAITING_VERIFICATION`. Dấu hiệu capture đã gửi, kể cả marker timeout của dữ liệu cũ thiếu operation ID, vẫn giữ `GATEWAY_TIMEOUT_AWAITING_VERIFICATION` và không gửi lại capture.
- PayPal chưa approve/capture vẫn cho phép khách thực hiện capture đầu tiên sau approval; tra cứu trước approval không chặn thao tác này.
- Nếu xác nhận booking hoặc ghi audit thất bại, transaction kết quả rollback; payment tiếp tục PENDING với `RECONCILIATION_APPLY_REQUIRES_REVIEW` và lịch đối soát tiếp theo. Không tự xác nhận hold hết hạn/bán vượt tồn; trường hợp này cần kiểm tra vận hành, không tự gửi một lệnh tài chính mới.
- Job lấy tối đa 50 payment PAYPAL/VNPAY đến hạn mỗi lượt, không quét trùng capture trong cùng lượt. Claim được commit trước query; backoff 30/60/120/240/300 giây. Payment chưa giải quyết không chiếm mãi batch đầu.
- Audit `RECONCILE_PAYMENT_SUCCESS`, `RECONCILE_PAYMENT_FAILED`, `RECONCILE_PAYMENT_PENDING`, `RECONCILE_PAYMENT_SKIPPED` ghi cùng transaction trạng thái, gồm cả nhánh phục hồi capture. Metadata JSON dùng TEXT; không phát audit SUCCESS cho transaction bị rollback.
- `paymentStatus` chuyển PAID sau thanh toán, REFUNDED khi hoàn toàn bộ; hoàn một phần vẫn PAID vì enum hiện không có PARTIALLY_REFUNDED. Hủy không hoàn chuyển NO_REFUND; hủy có refund PENDING chưa được ghi REFUNDED.
- Payment response bổ sung `lastError`, `reconciliationAttempts`, `reconciliationNextAttemptAt`, `lastReconciledAt`. Refund detail bổ sung `verificationAttempts`, `nextAttemptAt`, `gatewayRequestedAt`, `paymentId`; `retryCount` không thay thế số lần query xác minh.
- Migration `V23__admin_transaction_reconciliation.sql` bổ sung lịch đối soát/index, metadata TEXT và backfill PAID/REFUNDED từ payment cũ. Không sửa migration đã phát hành; V21/V22 đã được dùng ở các nhánh AI/review/reporting khác.
- Bộ kiểm chứng: `AdminTransactionIntegrationTest` dùng PostgreSQL 16/Flyway và Redis 7 riêng; HTTP/RBAC/owner, lọc kết hợp/phân trang/biên ngày, query cạnh tranh với webhook, rollback booking/audit, money mismatch, đổi intent khi query, capture đầu tiên, marker timeout legacy, IPN chưa rõ, hoàn toàn phần/một phần, backoff/batch và nâng DB V20→V23. Gateway/confirm booking được mock; không thay thế giao dịch sandbox thật.

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

### AI workflow, assessment, preferences and support — API
- [x] GET /api/admin/ai/assessment-cases
- [x] GET /api/admin/support/requests
- [x] GET /api/admin/support/requests/{id}
- [x] GET /api/ai/itineraries
- [x] GET /api/ai/itineraries/page
- [x] GET /api/ai/itineraries/{id}
- [x] GET /api/ai/itineraries/{id}/proposals
- [x] GET /api/ai/itineraries/{id}/revisions
- [x] GET /api/ai/preferences
- [x] GET /api/ai/review-summaries/{serviceId}
- [x] GET /api/ai/support/requests
- [x] GET /api/ai/support/requests/{id}
- [x] POST /api/admin/ai/assessment-cases/{id}/resolve
- [x] POST /api/admin/ai/risk-cases
- [x] POST /api/admin/support/requests/{id}/handle
- [x] POST /api/ai/classifications/service
- [x] POST /api/ai/content-assessments
- [x] POST /api/ai/itineraries
- [x] POST /api/ai/itineraries/preview
- [x] POST /api/ai/itineraries/previews/{previewId}/save
- [x] POST /api/ai/itineraries/{id}/accept
- [x] POST /api/ai/itineraries/{id}/archive
- [x] POST /api/ai/itineraries/{id}/proposals/{proposalId}/accept
- [x] POST /api/ai/itineraries/{id}/proposals/{proposalId}/reject
- [x] POST /api/ai/itineraries/{id}/replan
- [x] POST /api/ai/nearby
- [x] POST /api/ai/recommendations
- [x] POST /api/ai/recommendations/{id}/feedback
- [x] POST /api/ai/search
- [x] POST /api/ai/services/compare
- [x] POST /api/ai/support
- [x] POST /api/ai/support/requests
- [x] POST /api/ai/support/requests/preview
- [x] POST /api/ai/support/requests/{id}/cancel
- [x] POST /api/ai/weather
- [x] PUT /api/ai/preferences

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
- [x] GET /api/notifications (danh sách thông báo của user hiện tại, có phân trang, `isRead`/`readAt`)
- [x] GET /api/notifications/unread-count (số thông báo chưa đọc của user hiện tại)
- [x] PATCH /api/notifications/{id}/read (chỉ chủ thông báo; đánh dấu đọc idempotent)
- [x] PATCH /api/notifications/read-all (chỉ đánh dấu thông báo của user hiện tại)

### Discovery, notification & receipt — Bảo đảm nghiệp vụ
- [x] Search lọc ca tương lai còn đủ chỗ trước phân trang/count; trừ hold Redis còn hiệu lực và kiểm tra đơn vị dùng chung/gói riêng.
- [x] `slots` trong detail trả từng cặp slot/option: `optionId`, `pricingUnit`, `maxPaxPerPackage`, `inventoryType`, `bookable`; `availableCapacity` tính theo đơn vị của option. `capacity` là sức chứa người của ca, không phải số gói.
- [x] `availableSlots` cũ chỉ chứa ca còn đặt được; `sortBy=bookings_desc` dựa trên số sub-order đã thanh toán đang xác nhận/đã check-in/đang diễn ra/hoàn thành.
- [x] Luồng thanh toán/hoàn tiền thực phát event khi xác nhận SUCCESS/PROCESSED; thông báo IN_APP lưu trong cùng giao dịch PostgreSQL, rollback cùng trạng thái.
- [x] Migration `V27__notification_read_tracking_and_idempotency.sql` bổ sung unique key; event/job đồng thời không tạo trùng thông báo.
- [x] Nhắc lịch theo giờ Việt Nam, trong 24 giờ trước khởi hành, chỉ sub-order CONFIRMED thuộc master PAID/PARTIALLY_COMPLETED đã thanh toán; không nhắc trải nghiệm bị hủy/từ chối/hoàn thành.
- [x] Nội dung thông báo theo locale người nhận (vi/en); gửi push sau commit.
- [x] Biên nhận dùng snapshot tiền đã lưu; PDF phân trang, xuống dòng, nhúng font tiếng Việt và giữ đủ item/tổng tiền.
- [ ] Push thiết bị thật: adapter hiện tại chỉ preview log trong dev và trả `false`; cần cấu hình/tích hợp nhà cung cấp push trước khi nghiệm thu delivery thực.

### Discovery, notification & receipt — Test nghiệm thu
- [x] 93 kiểm tra tập trung strict: 69 test P2, 17 hồi quy, 2 PDF, 2 migration, 2 context/inventory và 1 luồng hoàn tiền; không skip.
- [x] Test đối chiếu toàn bộ 158 method/path controller với tracking; phát hiện API thiếu/thừa trong tài liệu.
- [x] `clean verify`: Maven báo 2.168 test, 0 failure/error, 135 skip ở bộ service E2E cũ; 2.033 test thực thi pass. Không tính test skip là đã nghiệm thu.

Availability là dữ liệu tham khảo; `POST /api/bookings/hold` vẫn quyết định tồn nguyên tử. Khi có nhiều option, khách gửi đúng `optionId` và quantity theo đơn vị option. Search `guests` là số người; nhóm tour ghép mặc định không chia sang nhiều đơn vị, còn gói riêng có thể cần nhiều gói theo giới hạn người/gói.

Nếu DB dev đã áp dụng migration notification V25 cũ, cần kiểm tra chính xác bản ghi Flyway và phối hợp nâng version trước khi ghép nhánh. Không xóa/reset DB hay sửa history tự động; V27 chưa giải quyết việc hai nhánh AI/chat khác cùng sử dụng V25.

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

5. **Theo dõi và quản trị giao dịch [ĐÃ KIỂM CHỨNG BACKEND]** Khách xem được lịch sử payment/refund của đơn; admin tìm và lọc giao dịch toàn sàn. Đối soát chỉ tra cứu, kiểm tra identity/money, cập nhật dưới khóa và ghi audit cùng transaction. Gateway trong test được mô phỏng; nghiệm thu sandbox thật vẫn là mục riêng.
   - `GET /api/orders/{id}/payments`
   - `GET /api/orders/{id}/refunds`
   - `GET /api/admin/orders`, `/payments`, `/refunds`

6. **Phục hồi tài khoản và xác nhận an toàn.**
   - `POST /api/auth/forgot-password`
   - `POST /api/auth/reset-password`
   - Mở rộng service detail và checkout để khách đọc, xác nhận waiver; lưu nội dung/version và thời điểm xác nhận.

Sau các nhóm trên, nên bổ sung **đổi lịch do thời tiết**, **voucher**, **nhắn tin khách–vendor**, **theo dõi/phản hồi khiếu nại**, **read/unread thông báo** và **biên nhận thanh toán**. Đây đều là những phần đã được nêu trong phạm vi đề tài hoặc giúp khép kín luồng sử dụng.
