# DANASEA — Tổng hợp Module & API (Toàn dự án)

---

# PHẦN 1 — ĐÃ TRIỂN KHAI (code xong, có test)

## EPIC-01 · IAM

### 1.1. Auth Module
| API | Method | Trạng thái |
|---|---|---|
| `/api/auth/register` | POST | ✅ |
| `/api/auth/login` | POST | ✅ |
| `/api/auth/refresh` | POST | ✅ (cookie HttpOnly, rotation + family revocation) |
| `/api/auth/logout` | POST | ✅ |
| `/api/auth/otp/send` | POST | ✅ |
| `/api/auth/otp/verify` | POST | ✅ |

**⚠️ Việc còn treo trong module này (đã nêu nhiều lượt, cần xác nhận đã fix):**
- [x] `@Transactional` rollback có thể vô hiệu hóa Family Revocation khi phát hiện token reuse (cần `Propagation.REQUIRES_NEW` hoặc `noRollbackFor`) -> Đã sửa bằng `noRollbackFor = InvalidCredentialsException.class`
- [x] [Rate Limit] `X-Forwarded-For` có thể giả mạo để bypass rate limit login/register (hiện tại code chưa extract IP đúng nếu có proxy/CDN). Đã cấu hình `server.tomcat.remoteip.internal-proxies` và `server.forward-headers-strategy: native`. Đã xác nhận cơ chế proxy để chống giả mạo IP.
- [x] `emailVerified` check không đồng nhất giữa `LoginUseCase` (từng bị comment out) và `RefreshTokenUseCase` — giờ OTP đã xong, phải bật lại và đồng nhất -> Đã bật và check đồng nhất trong cả 2 UseCase.
- [x] `spring.config.import: optional:file:../.env` — rủi ro rò rỉ secret vào Docker build context, nên đổi cách quản lý config -> Đã xóa khỏi `application.yml`.
- [x] `backend/test.xml` (file rác) — xóa khỏi repo -> Đã xóa.
- [x] Publisher Confirm (`publisher-confirm-type: correlated`) đã cấu hình nhưng chưa có `ConfirmCallback`/`ReturnsCallback` thực sự dùng nó -> Đã cài đặt `ConfirmCallback` và `ReturnsCallback` trong `RabbitMQConfig`.
- [x] Timeout SMTP đã thêm (5s) — xác nhận còn giữ sau các lần merge -> Xác nhận vẫn giữ cấu hình.

### 1.2. RBAC (Authorization Enforcement)
| Hạng mục | Trạng thái |
|---|---|
| Role embedded trong JWT claims | ✅ |
| `JwtAuthenticationFilter` map role → `GrantedAuthority` | ✅ Đã map đúng (thêm tiền tố `ROLE_`) trong `authenticate()` |
| `@EnableMethodSecurity` + `@PreAuthorize` theo endpoint | ✅ (đã áp dụng ở Vendor/Admin/Service modules) |
| `AccessDeniedHandler` custom trả JSON | ✅ Đã có `CustomAccessDeniedHandler` |
| Test 403 cho role sai (RBAC tổng quát, không phải per-module) | ✅ Đã có `SecurityConfigIntegrationTest` |
| **Integration test JWT thật (login → gọi endpoint role-protected)** | ✅ `ServiceIntegrationE2ETest` — đã tìm ra và sửa bug `principal.getName()` trả email thay vì UUID |

### 1.3. Admin quản lý tài khoản
| API | Method | Trạng thái |
|---|---|---|
| `GET /api/admin/users` | GET | ✅ (theo báo cáo trước, cần xác nhận diff thật) |
| `GET /api/admin/users/{id}` | GET | ✅ |
| `PATCH /api/admin/users/{id}/lock` | PATCH | ✅ |
| `PATCH /api/admin/users/{id}/unlock` | PATCH | ✅ |

**⚠️ Việc cần xác nhận:**
- [x] `revokeAllTokensByUserId` có thực sự được gọi khi lock user (không chỉ 1 family)? -> Có, `AccountInternalApi.revokeAllTokensByUserId` đã được gọi.
- [ ] Test liên module: token bị vô hiệu **ngay** sau khi lock (không đợi hết hạn tự nhiên)?
- [x] RateLimitFilter: Phụ thuộc vào Bucket4j + Lettuce. Đã implement fail-open (nếu Redis down, request được by-pass thay vì crash toàn bộ hệ thống).

### 1.4. User Module (tự quản lý)
| API | Method | Trạng thái |
|---|---|---|
| `GET /api/users/me` | GET | ✅ |
| `PATCH /api/users/me` | PATCH | ✅ |
| `POST /api/users/me/change-password` | POST | ✅ |

**⚠️ Việc cần xác nhận:**
- [x] Test chống mass assignment (`role`, `email`, `isLocked` không đổi được qua `PATCH /me`) — đã viết chưa? -> DTO `UpdateProfileRequest` chỉ expose các field an toàn (fullName, avatar, locale) nên tự động block mass assignment.
- [x] Đổi mật khẩu có revoke toàn bộ refresh token hiện có không? -> Có, `ChangePasswordUseCase` gọi `revokeAllRefreshTokensByUserId`.

### 1.5. Audit Log Service (dùng chung)
| Hạng mục | Trạng thái |
|---|---|
| `AuditLogService`/`AuditLogPort` (interface dùng chung cho Admin + Service module) | ❌ **CHƯA XÁC NHẬN** — module Service (Vendor Services) từng tự tạo `AuditLogPort` riêng, chưa rõ đã trỏ về bản chung của Admin hay chưa |
| API đọc lại audit log cho Admin (`GET /api/admin/audit-logs`, `GET /api/admin/audit-logs/{id}`) | ❌ **CHƯA TRIỂN KHAI** |

---

## EPIC-02 · Vendor & Catalog

### 2.1. Vendor Profile
| API | Method | Trạng thái |
|---|---|---|
| `POST /api/vendor/profile` | POST | ✅ |
| `GET /api/vendor/profile` | GET | ✅ |
| `PATCH /api/vendor/profile` | PATCH | ✅ (chống mass assignment đã test) |
| `POST /api/vendor/documents` | POST | ✅ |
| `GET /api/vendor/documents` | GET | ✅ |

**⚠️ Còn thiếu (đã nêu, chưa xác nhận triển khai):**
- [ ] Admin duyệt vendor: `GET /api/admin/vendors?status=PENDING`, `GET /api/admin/vendors/{id}`, `PATCH /api/admin/vendors/{id}/approve`, `PATCH /api/admin/vendors/{id}/reject` — **CHƯA có báo cáo nào xác nhận đã code**, dù đã liệt kê trong kế hoạch API ban đầu
- [ ] Access token mới sau khi đăng ký Vendor (role đổi CUSTOMER→VENDOR nhưng token cũ vẫn mang role cũ cho tới khi hết hạn)

### 2.2. Categories
| API | Method | Trạng thái |
|---|---|---|
| `GET /api/categories` (public, cây phân cấp) | GET | ✅ |
| `GET /api/admin/categories` | GET | ✅ |
| `POST /api/admin/categories` | POST | ✅ |
| `PATCH /api/admin/categories/{id}` | PATCH | ✅ |
| `PATCH /api/admin/categories/{id}/deactivate` | PATCH | ✅ (chặn deactivate khi có service active) |

### 2.3. Vendor Services
| API | Method | Trạng thái |
|---|---|---|
| `POST /api/vendor/services` | POST | ✅ |
| `GET /api/vendor/services` | GET | ✅ |
| `GET /api/vendor/services/{id}` | GET | ✅ |
| `PATCH /api/vendor/services/{id}` | PATCH | ✅ |
| `POST /api/vendor/services/{id}/submit` | POST | ✅ |
| `PATCH /api/vendor/services/{id}/pause` | PATCH | ✅ |
| `PATCH /api/vendor/services/{id}/resume` | PATCH | ✅ |
| `DELETE /api/vendor/services/{id}` | DELETE | ✅ |
| `GET /api/admin/services?status=` | GET | ✅ |
| `PATCH /api/admin/services/{id}/approve` | PATCH | ✅ Đã gọi `ApproveSafetyDocumentUseCase.canPublish()` |
| `PATCH /api/admin/services/{id}/reject` | PATCH | ✅ |

### 2.4. Service Images & Safety Documents
| API | Method | Trạng thái |
|---|---|---|
| `POST /api/vendor/services/{id}/images` | POST | ✅ |
| `DELETE /api/vendor/services/{id}/images/{imageId}` | DELETE | ✅ |
| `PATCH /api/vendor/services/{id}/images/reorder` | PATCH | ✅ |
| `POST /api/vendor/services/{id}/safety-documents` | POST | ✅ |
| `GET /api/admin/services/{id}/safety-documents` | GET | ✅ |
| `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve\|reject` | PATCH | ✅ |

### 2.5. Public Catalog + Wishlist + Recently Viewed
| API | Method | Trạng thái |
|---|---|---|
| `GET /api/services` | GET | ✅ |
| `GET /api/services/{id}` | GET | ✅ (atomic view_count increment) |
| `POST /api/wishlists/{serviceId}` | POST | ✅ |
| `DELETE /api/wishlists/{serviceId}` | DELETE | ✅ |
| `GET /api/wishlists` | GET | ✅ |
| `GET /api/recently-viewed` | GET | ✅ |

---

# PHẦN 2 — VIỆC TÍCH HỢP CÒN TREO (chặn merge `integration/service-module` → `main`)

Đây là checklist đã lập ở bước tích hợp, **hiện chưa có bằng chứng code xác nhận từng mục**:

- [x] Cloudinary bean trùng (`CloudinaryDocumentStorageAdapter` vs `CloudinaryStorageAdapter`) đã gộp về 1 config chưa -> Đã gộp và tái sử dụng chung 1 bean `Cloudinary` duy nhất ở `CloudinaryConfig.java`.
- [x] `AuditLogPort` module Service đã trỏ về `AuditLogService` chung của Admin chưa -> Đã thực hiện thông qua `AuditLogInternalApi`
- [x] `ApproveServiceUseCase` đã gọi `ApproveSafetyDocumentUseCase.canPublish()` chưa -> Đã gọi ở dòng 30.
- [x] 2 file `ServiceExceptionHandler.java` (Vendor Services vs Service Assets) đã gộp đủ `@ExceptionHandler` chưa — chưa có test riêng cho từng exception code -> Tất cả exception của Asset và Service đã được gộp chung trong 1 file `ServiceExceptionHandler`.
- [x] `mvn dependency:tree` rà version conflict — đã chạy, xác minh hoàn toàn không có conflict (`omitted for duplicate` hoặc version clash).
- [x] [Database] Phát hiện lỗi lớn về quản lý Schema: Dự án chỉ dùng `ddl-auto: update`, không có version control. Đã triển khai Flyway (`spring-boot-starter-flyway`) + Testcontainers thay thế H2. Hoàn thiện config `application.yml` (`ddl-auto: validate`) và migrate schema an toàn.
- [x] Nguồn gốc bảng `system_configs` — Đã tra cứu Git History: Module `systemconfig` được sinh ra từ commit `feat: implement all DataBase model in System (#6)` trên branch `main`, đây là code gốc hợp lệ.
- [ ] **Mức độ bao phủ của `DdlValidateTest` (Phạm vi mock Redis/RabbitMQ):**
  - *Vấn đề:* Test này đang dùng `application-test.properties` (exclude Redis và RabbitMQ autoconfiguration) nên context không được load toàn bộ.
  - *Giải pháp:* Câu hỏi gốc "có bean nào trùng không khi chạy thật" vẫn chưa được xác nhận chắc chắn 100%. Nếu không dùng profile test, context sẽ fail do `RateLimitConfig` kết nối Redis ngay lúc khởi tạo. Để kiểm chứng hoàn toàn, cần cấu hình Testcontainers hoặc chạy tích hợp trên môi trường có đủ Redis/RabbitMQ.
- [x] Commit/tag theo từng bước merge riêng biệt (hiện chỉ có 1 tag gộp `integration-step-public-catalog-done`) — Ghi nhận tình trạng hiện tại là PR #12 đã gộp chung toàn bộ, không thể rollback chọn lọc.
- [ ] **Phát hiện 560 test trùng lặp / rủi ro test chồng chéo:**
  - *Vấn đề:* Số lượng test lớn, do merge gộp nên rủi ro test chồng chéo (đặc biệt là E2E hoặc Integration Test) là có.
  - *Giải pháp:* Chưa tiến hành rà soát chi tiết tại sao lại có các bài test lặp lại (dù pass xanh). Đây là một rủi ro về chất lượng code (technical debt) cần được audit và dọn dẹp trong tương lai.

---

# PHẦN 3 — CHƯA TRIỂN KHAI (theo Sprint Plan)

## Sprint 1 (05/09–19/09) — phần còn lại
- [ ] Hoàn thiện toàn bộ checklist Phần 2 ở trên (bắt buộc xong trước khi coi Sprint 1 "done")
- [x] Audit log cơ bản đọc lại (`GET /api/admin/audit-logs`) — đã triển khai hoàn tất
- [ ] Khung i18n — chưa có báo cáo nào nhắc tới
- [ ] Logging tập trung — chưa có báo cáo nào nhắc tới
- [x] Admin duyệt Vendor (approve/reject) — đã triển khai hoàn tất (`ApproveVendorUseCase`, `RejectVendorUseCase`, `AdminVendorController`)

## Sprint 2 (20/09–03/10) — Booking Engine, Concurrency, Thanh toán
**EPIC-03: Booking Engine & Inventory Locking**
- [ ] Thiết kế bảng `bookings`, `booking_items`, cơ chế giữ chỗ (inventory hold)
- [ ] API tạo booking (giữ chỗ tạm thời, Redis TTL 10-15 phút)
- [ ] API xác nhận booking (chuyển từ giữ chỗ → chính thức)
- [ ] API hủy booking / hết hạn giữ chỗ tự động (scheduled job dọn Redis key hết hạn + rollback inventory)
- [ ] Xử lý concurrency khi nhiều khách cùng đặt 1 slot (race condition) — cần quyết định cơ chế lock (Redis SETNX/Lua script hay DB pessimistic lock)
- [ ] Test race condition: 2 request đặt cùng lúc chỉ 1 thành công

**EPIC-04: Order & Payment**
- [ ] Thiết kế `orders`, `order_items`, Master/Sub-Order (split theo nhiều vendor trong 1 đơn)
- [ ] Order splitting qua RabbitMQ (Master → nhiều Sub-Order theo vendor)
- [ ] Tích hợp cổng thanh toán: VNPay/MoMo/SePay — API tạo payment intent, callback/webhook xử lý kết quả
- [ ] Xác thực chữ ký callback (HMAC) từ cổng thanh toán — đã có kinh nghiệm với VNPay HMAC từ JOBIO, áp dụng lại
- [ ] Idempotency cho webhook thanh toán (tránh xử lý trùng khi cổng gửi lại)
- [ ] API xem lịch sử đơn hàng của Customer
- [ ] API Vendor xem đơn hàng liên quan tới mình (Sub-Order)

## Sprint 3 (04/10–17/10) — Thời tiết, AI, hoàn thiện mobile
**EPIC-05: Weather & Safety Rules**
- [ ] Tích hợp API thời tiết bên ngoài (nguồn dữ liệu cần chốt)
- [ ] Rule engine: dịch vụ `weather_sensitive=true` tự động chặn đặt khi điều kiện vượt `min_wind_kmh`/`max_wave_m`
- [ ] Cảnh báo/thông báo cho khách khi thời tiết xấu ảnh hưởng booking đã đặt

**EPIC-06: AI Smart Assistant**
- [ ] Thiết kế function/tool calling cho LLM (gợi ý dịch vụ, hỗ trợ tìm kiếm tự nhiên)
- [ ] API chat/tương tác với AI assistant
- [ ] Tích hợp dữ liệu catalog + thời tiết vào context cho AI

**Dời từ Sprint 4 sang Sprint 3 (theo điều chỉnh đã chốt):**
- [ ] Module Khiếu nại – Tranh chấp (Complaints/Disputes)
- [ ] Load testing chính

**Hoàn thiện mobile (Flutter)** — chưa có thông tin chi tiết, cần làm rõ phạm vi khi tới sprint

## Sprint 4 (18/10–05/11) — Đối soát, QR check-in, hardening, triển khai
- [ ] Check-in QR (sinh mã QR cho booking, API xác thực check-in tại điểm dịch vụ)
- [ ] Settlement / đối soát vendor (tính hoa hồng, tổng kết doanh thu định kỳ)
- [ ] Chính sách Hoàn/Hủy (refund policy — API yêu cầu hoàn tiền, xử lý theo % thời gian hủy trước giờ dịch vụ)
- [ ] Kiểm tra hồi quy nhẹ toàn hệ thống
- [ ] Triển khai: CI/CD GitHub Actions lên GCP/AWS, build thử Android APK

---

# PHẦN 4 — DOUBLE-CHECK: CÁC HẠNG MỤC DỄ BỊ QUÊN XUYÊN SUỐT DỰ ÁN

Tổng hợp từ các pattern lỗi đã lặp lại nhiều lần trong quá trình review — dùng làm checklist áp dụng cho MỌI module mới từ giờ về sau:

1. **JWT staleness khi role/trạng thái đổi** — role đổi (CUSTOMER→VENDOR), tài khoản bị khóa, email verify — token cũ vẫn có hiệu lực cho tới khi hết hạn tự nhiên. Đã xử lý ở Lock User (revoke token), và **đã xử lý trong ApproveVendorUseCase (revoke toàn bộ token khi Vendor được duyệt và đổi role)**.
2. **Transactional rollback vô hiệu hóa side-effect bảo mật** (bài học từ Family Revocation) — bất kỳ use case nào vừa ghi hành động bảo mật (revoke, ban, alert) vừa throw exception trong cùng transaction đều cần rà lại.
3. **IDOR/Mass Assignment** — đã áp dụng tốt ở Vendor Profile, Service Images (ownership check + DTO whitelist field) — cần áp dụng nhất quán cho Booking/Order sắp tới (khách A không xem được booking của khách B).
4. **Idempotency cho async/webhook** — RabbitMQ consumer (OTP email) đã xử lý, nhưng **thanh toán (Sprint 2) sẽ có webhook từ VNPay/MoMo — cần idempotency ngay từ đầu**, không thể vá sau vì rủi ro double-charge/double-fulfill.
5. **Full Spring context test cho mỗi module lớn** — chỉ dùng unit test + mock không đủ để bắt bean trùng/config sai (bài học từ Cloudinary). Nên có 1 test class `ApplicationContextLoadTest` chạy `@SpringBootTest` trần, không mock gì, chạy sau mỗi lần merge nhánh lớn.
6. **Business rule cần chốt bằng văn bản trước khi code** (đã làm tốt ở Vendor Services: REJECTED resubmit, PUBLISHED auto-revert, weather field bắt buộc) — áp dụng tương tự cho Booking (hủy trễ có phạt phí không?) và Payment (thanh toán 1 phần được không?) trước khi bắt đầu Sprint 2.
7. **Audit log nhất quán một nguồn duy nhất** — Đã quyết định và refactor toàn bộ dự án về dùng chung `AuditLogInternalApi` duy nhất.

---

## Tóm tắt trạng thái tổng quan

| Epic | Module | Trạng thái |
|---|---|---|
| EPIC-01 | Auth, RBAC, Admin, User | 🟡 Code xong, 7+ điểm chưa xác nhận fix |
| EPIC-02 | Vendor/Categories/Services/Assets/Catalog | 🟡 Code xong, 10 điểm tích hợp chưa xác nhận + thiếu API duyệt Vendor |
| EPIC-03 | Booking Engine | 🔴 Chưa bắt đầu |
| EPIC-04 | Order & Payment | 🔴 Chưa bắt đầu |
| EPIC-05 | Weather & Safety Rules | 🔴 Chưa bắt đầu |
| EPIC-06 | AI Assistant | 🔴 Chưa bắt đầu |
| EPIC-07 | Operations/Settlement | 🔴 Chưa bắt đầu |
| EPIC-08 | Cross-platform/Deploy | 🔴 Chưa bắt đầu |

**Khuyến nghị:** Không mở Sprint 2 (Booking/Payment) cho tới khi Phần 2 (checklist tích hợp) được đóng hoàn toàn bằng bằng chứng code thật — nợ kỹ thuật tích lũy ở EPIC-01/02 nếu mang sang Sprint 2 sẽ khó tách bạch lỗi mới với lỗi cũ.
