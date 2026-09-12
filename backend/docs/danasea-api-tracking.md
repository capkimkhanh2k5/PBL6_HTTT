# DANASEA — Master API & Test Checklist (EPIC-01 → EPIC-08)

**Cập nhật:** 11/09/2026 — `[x]` đã xong · `[ ]` chưa xong/chưa xác nhận

---

## EPIC-01 · IAM

### Auth Module — API
- [x] POST /api/auth/register
- [x] POST /api/auth/login
- [x] POST /api/auth/refresh
- [x] POST /api/auth/logout
- [x] POST /api/auth/otp/send
- [x] POST /api/auth/otp/verify

### Auth Module — Test
- [x] RegisterUseCaseTest (thành công, email trùng, publish event)
- [x] LoginUseCaseTest (thành công, sai mật khẩu, user locked, email chưa verify)
- [x] RefreshTokenUseCaseTest (rotation, family revocation, hết hạn, user locked)
- [x] LogoutUseCaseTest
- [x] SendVerificationOtpUseCaseTest
- [x] VerifyOtpUseCaseTest (đúng, sai, hết hạn, vượt max attempts, one-time-use)
- [x] OtpEmailConsumerTest (mail lỗi → DLQ)
- [x] AuthenticationControllerTest (login/register/refresh/logout/OTP, cookie httpOnly)
- [x] RateLimitFilterIntegrationTest
- [x] Test Family Revocation persist thật qua DB sau khi fix noRollbackFor
- [x] Test X-Forwarded-For không bypass được rate limit (sau khi cấu hình forward-headers-strategy)
- [x] Test emailVerified đồng nhất giữa Login và Refresh

### RBAC — Hạ tầng
- [x] Role trong JWT claims + map GrantedAuthority (ROLE_*)
- [x] @EnableMethodSecurity + @PreAuthorize theo endpoint
- [x] CustomAccessDeniedHandler (JSON, không phải HTML)

### RBAC — Test
- [x] SecurityConfigIntegrationTest (403 role sai, 401 không token/hết hạn)
- [x] ServiceIntegrationE2ETest (login thật → gọi endpoint role-protected + cần userId)
- [x] AccessDeniedHandlerTest (response JSON đúng format)

### Admin quản lý tài khoản — API
- [x] GET /api/admin/users
- [x] GET /api/admin/users/{id}
- [x] PATCH /api/admin/users/{id}/lock
- [x] PATCH /api/admin/users/{id}/unlock
- [x] GET /api/admin/audit-logs (list — đã chốt: không cần API chi tiết riêng)

### Admin quản lý tài khoản — Test
- [x] LockUserUseCaseTest (thành công, không tồn tại, đã lock rồi, self-lock chặn)
- [x] UnlockUserUseCaseTest
- [x] Test liên module: refresh token của user vừa bị lock thất bại NGAY (không đợi hết hạn)
- [x] AdminUserControllerTest (list/filter, 404, role sai → 403)
- [x] AuditLogServiceTest (ghi đúng field, không rollback hành động chính nếu audit lỗi)

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

### Vendor Services — Test
- [x] CreateServiceUseCaseTest (6 case)
- [x] SubmitServiceForReviewUseCaseTest (4 case)
- [x] UpdateServiceUseCaseTest (3 case)
- [x] ApproveServiceUseCaseTest (3 case)
- [x] RejectServiceUseCaseTest (3 case)
- [x] DeleteServiceUseCaseTest (3 case)
- [x] ServiceControllerTest (13 case RBAC)

### Service Images & Safety Documents — API
- [x] POST /api/vendor/services/{id}/images
- [x] DELETE /api/vendor/services/{id}/images/{imageId}
- [x] PATCH /api/vendor/services/{id}/images/reorder
- [x] POST /api/vendor/services/{id}/safety-documents
- [x] GET /api/admin/services/{id}/safety-documents
- [x] PATCH /api/admin/services/{id}/safety-documents/{docId}/approve|reject

### Service Images & Safety Documents — Test
- [x] UploadServiceImageUseCaseTest (7 case: IDOR, file type, max 10 ảnh)
- [x] ReorderServiceImagesUseCaseTest (6 case)
- [x] UploadSafetyDocumentUseCaseTest (5 case)
- [x] ApproveSafetyDocumentUseCaseTest (9 case, Publish Guard)

### Public Catalog + Wishlist + Recently Viewed — API
- [x] GET /api/services
- [x] GET /api/services/{id}
- [x] POST /api/wishlists/{serviceId}
- [x] DELETE /api/wishlists/{serviceId}
- [x] GET /api/wishlists
- [x] GET /api/recently-viewed

### Public Catalog + Wishlist + Recently Viewed — Test
- [x] SearchServicesUseCaseTest
- [x] GetServiceDetailUseCaseTest (atomic increment, DRAFT→404)
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
- [ ] Mốc thời gian cụ thể coi là "hủy trễ" (trước giờ dịch vụ 24h)
- [ ] Hủy SỚM hơn mốc đó thì hoàn toàn bộ
- [ ] Vendor có quyền từ chối booking đã đặt (trước thanh toán), sau đó sẽ thông báo với khách hàng và recommend các vendor khác có slot trống (không tự động chuyển sang vendor khác), và nếu khách không đồng ý thì xoá booking + hoàn tiền mục từ chối đó

### API
- [ ] POST /api/bookings/hold (giữ chỗ nhiều dịch vụ trong 1 lần, TTL 10-15 phút)
- [ ] POST /api/bookings/{holdId}/confirm (chỉ xác nhận sau khi thanh toán toàn bộ thành công)
- [ ] DELETE /api/bookings/hold/{holdId}
- [ ] GET /api/bookings/{id}
- [ ] GET /api/bookings
- [ ] GET /api/vendor/bookings
- [ ] PATCH /api/bookings/{id}/cancel (áp rule mất toàn bộ tiền nếu trễ)
- [ ] PATCH /api/vendor/bookings/{id}/reject (nếu được cho phép)
- [ ] Scheduled job dọn Redis hold hết hạn + rollback inventory

### Test
- [ ] CreateBookingHoldUseCaseTest (nhiều dịch vụ trong 1 hold, slot hết → chặn, TTL đúng)
- [ ] ConfirmBookingUseCaseTest (chỉ confirm khi đã thanh toán đủ, hold hết hạn → lỗi, sai owner → 403)
- [ ] Test race condition đa luồng thật (2 request giữ slot cuối cùng, dùng Lua script atomic)
- [ ] CancelBookingUseCaseTest (đúng mốc thời gian mất toàn bộ tiền)
- [ ] BookingExpiryJobTest (rollback đúng, không rollback nhầm hold đã confirm)
- [ ] BookingControllerTest (IDOR: khách A/B, vendor không liên quan)

---

## EPIC-04 · Order & Payment

### Quyết định nghiệp vụ 
- [x] Không hỗ trợ đặt cọc — bắt buộc thanh toán toàn bộ
- [x] Thanh toán 1 lần cho toàn bộ Master Order (không thanh toán riêng từng Sub-Order)
- [x] Hoàn tiền tự động (không cần Admin duyệt thủ công)

### API
- [ ] POST /api/orders (tạo Master Order từ booking, chờ thanh toán)
- [ ] GET /api/orders/{id}
- [ ] GET /api/orders
- [ ] GET /api/vendor/orders
- [ ] POST /api/payments/{orderId}/create-intent (thanh toán toàn bộ Master Order 1 lần)
- [ ] POST /api/payments/webhook/vnpay
- [ ] POST /api/payments/webhook/momo
- [ ] POST /api/payments/webhook/sepay
- [ ] POST /api/orders/{id}/refund-request (trigger hoàn tiền tự động theo policy)

### Test
- [ ] CreateOrderUseCaseTest (split đúng Sub-Order theo vendor, chờ thanh toán toàn bộ)
- [ ] OrderSplittingTest (RabbitMQ message đúng số lượng vendor)
- [ ] PaymentWebhookHmacTest (chữ ký sai → từ chối)
- [ ] PaymentWebhookIdempotencyTest (trùng transactionId → xử lý 1 lần)
- [ ] PaymentWebhookTest (thành công → confirm Master Order + mọi Sub-Order cùng lúc; thất bại → rollback inventory toàn bộ)
- [ ] RefundRequestUseCaseTest (tự động hoàn tiền đúng số tiền, đúng cổng thanh toán gốc)
- [ ] OrderControllerTest (IDOR customer/vendor)

---

## EPIC-05 · Weather & Safety Rules

### Quyết định nghiệp vụ
- [ ] Nguồn dữ liệu thời tiết cụ thể, cache TTL
- [ ] Thời tiết xấu ảnh hưởng booking đã confirm: tự động hủy + hoàn tiền (khớp rule hoàn tiền tự động ở EPIC-04) hay chỉ cảnh báo

### API
- [ ] GET /api/weather/current?lat=&lng=
- [ ] Job quét booking sắp diễn ra đối chiếu ngưỡng thời tiết
- [ ] POST /api/admin/weather-alerts/{bookingId}/resolve

### Test
- [ ] WeatherServiceAdapterTest (xử lý timeout/lỗi API bên thứ 3)
- [ ] WeatherRuleEngineTest (vượt ngưỡng chặn, không vượt cho phép, không weather_sensitive bỏ qua)
- [ ] WeatherAlertJobTest (không cảnh báo trùng)

---

## EPIC-06 · AI Smart Assistant

### Quyết định nghiệp vụ — CHƯA CHỐT
- [ ] Phạm vi: chỉ gợi ý, hay thực thi hành động (đặt chỗ) qua chat
- [ ] Giới hạn rate/cost gọi LLM

### API
- [ ] POST /api/assistant/chat
- [ ] GET /api/assistant/conversations/{id}

### Test
- [ ] AssistantChatUseCaseTest (tool calling đúng function)
- [ ] Test rate limit endpoint chat
- [ ] Test fallback khi LLM lỗi/timeout

---

## EPIC-07 · Operations / Settlement / Review

### Khiếu nại — Tranh chấp
- [ ] POST /api/orders/{id}/disputes
- [ ] GET /api/admin/disputes
- [ ] PATCH /api/admin/disputes/{id}/resolve
- [ ] Test: khiếu nại trùng, resolve cập nhật đúng Order/Refund

### QR Check-in
- [ ] POST /api/bookings/{id}/qr-code
- [ ] POST /api/vendor/checkin/verify
- [ ] Test: check-in trùng, QR hết hạn/không hợp lệ

### Settlement
- [ ] GET /api/admin/settlements
- [ ] POST /api/admin/settlements/generate
- [ ] GET /api/vendor/settlements
- [ ] Test: tính hoa hồng đúng, không tính trùng đơn hoàn/hủy

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
