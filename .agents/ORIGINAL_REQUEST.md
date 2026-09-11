# Original User Request

## 2026-09-10T03:40:29Z

Triển khai module Services cho Vendor (CRUD) và Admin (Duyệt/Từ chối dịch vụ) trong hệ thống đặt lịch. Bao gồm việc phát triển các REST API endpoints và bộ test cases đầy đủ để đảm bảo luồng nghiệp vụ.

Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module
Integrity mode: development

## Requirements

### R1. Triển khai Vendor API (CRUD Services)
- `POST /api/vendor/services` (Tạo mới, status=DRAFT)
- `GET /api/vendor/services` (Danh sách mọi status)
- `GET /api/vendor/services/{id}` (Chi tiết, chỉ owner)
- `PATCH /api/vendor/services/{id}` (Sửa thông tin)
- `POST /api/vendor/services/{id}/submit` (Gửi duyệt: DRAFT -> PENDING_REVIEW)
- `PATCH /api/vendor/services/{id}/pause` (Tạm ngưng: PUBLISHED -> PAUSED)
- `PATCH /api/vendor/services/{id}/resume` (Mở lại: PAUSED -> PUBLISHED)
- `DELETE /api/vendor/services/{id}` (Xóa, chỉ khi status=DRAFT)

### R2. Triển khai Admin API (Duyệt Services)
- `GET /api/admin/services?status=PENDING_REVIEW`
- `PATCH /api/admin/services/{id}/approve` (Duyệt: PENDING_REVIEW -> PUBLISHED, ghi audit log SERVICE_APPROVED)
- `PATCH /api/admin/services/{id}/reject` (Từ chối: -> REJECTED, kèm reason, ghi audit log)

### R3. Business Rules Validation
- **Quyền tạo dịch vụ:** Chỉ Vendor có `verification_status=APPROVED` mới được quyền tạo dịch vụ mới. Trả về lỗi nếu Vendor đang `PENDING`.
- **Validation Dữ Liệu:** 
  - `category_id` không tồn tại/inactive trả về 404/400.
  - Nếu `weather_sensitive=true`, bắt buộc phải truyền `min_wind_kmh` và `max_wave_m`.
- **Submit duyệt:** 
  - Thiếu ảnh (`service_images` rỗng) sẽ bị chặn.
  - Submit khi đang `REJECTED`: Cho phép sửa thông tin và gửi lại (chuyển trạng thái từ `REJECTED` sang `PENDING_REVIEW`).
- **Sửa thông tin:**
  - `DRAFT`: Cho phép sửa tự do.
  - `PUBLISHED`: Bất kỳ sửa đổi nào cũng sẽ chuyển dịch vụ về trạng thái `PENDING_REVIEW` để duyệt lại (trong thời gian này sẽ không nhận được booking mới).
  - Không được phép sửa dịch vụ của Vendor khác (trả về 403).
- **Xóa dịch vụ:**
  - Chỉ cho xóa khi ở trạng thái `DRAFT`.
  - Chặn và báo lỗi rõ ràng nếu cố xóa khi đang `PUBLISHED` hoặc `PAUSED`.
  
### R4. Security & Access Control
- Đầu cuối của Vendor (`/api/vendor/services/**`) chặn các role `CUSTOMER`, `ADMIN` (trả về 403).
- Đầu cuối của Admin (`/api/admin/services/**`) chặn role `VENDOR` (trả về 403).

## Acceptance Criteria

### API Functionality & Validation
- [ ] Tất cả các endpoint thực hiện đúng hành vi, luồng nghiệp vụ và trả về mã HTTP chuẩn.
- [ ] Các Business Rules ở R3 được validate chặt chẽ (đặc biệt các rule chặn Vendor chưa verify, bắt buộc có ảnh, rules thời tiết).
- [ ] Quyền truy cập được phân cấp chuẩn theo R4.

### Test Coverage
- [ ] Implement `CreateServiceUseCaseTest`, `SubmitServiceForReviewUseCaseTest`, `UpdateServiceUseCaseTest`, `ApproveServiceUseCaseTest`, `RejectServiceUseCaseTest`, `DeleteServiceUseCaseTest`.
- [ ] Implement `ServiceControllerTest` (MockMvc) để test phân quyền API.
- [ ] Tất cả test case mô tả trong yêu cầu phải PASS.

## 2026-09-10T04:04:46Z

[RESUME PREVIOUS RUN]: Tiến trình trước đó đã tạo `PROJECT.md`, `TEST_READY.md` và hoàn tất khảo sát (Milestone 1) nhưng bị gián đoạn do lỗi Resource Exhausted. Hãy đọc các file đã có trong thư mục hiện tại (bao gồm `PROJECT.md`, `.agents/`) và tiếp tục công việc từ Milestone 1 Implementation/Milestone 2.

Triển khai module Services cho Vendor (CRUD) và Admin (Duyệt/Từ chối dịch vụ) trong hệ thống đặt lịch. Bao gồm việc phát triển các REST API endpoints và bộ test cases đầy đủ để đảm bảo luồng nghiệp vụ.

Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module
Integrity mode: development

## Requirements

### R1. Triển khai Vendor API (CRUD Services)
- `POST /api/vendor/services` (Tạo mới, status=DRAFT)
- `GET /api/vendor/services` (Danh sách mọi status)
- `GET /api/vendor/services/{id}` (Chi tiết, chỉ owner)
- `PATCH /api/vendor/services/{id}` (Sửa thông tin)
- `POST /api/vendor/services/{id}/submit` (Gửi duyệt: DRAFT -> PENDING_REVIEW)
- `PATCH /api/vendor/services/{id}/pause` (Tạm ngưng: PUBLISHED -> PAUSED)
- `PATCH /api/vendor/services/{id}/resume` (Mở lại: PAUSED -> PUBLISHED)
- `DELETE /api/vendor/services/{id}` (Xóa, chỉ khi status=DRAFT)

### R2. Triển khai Admin API (Duyệt Services)
- `GET /api/admin/services?status=PENDING_REVIEW`
- `PATCH /api/admin/services/{id}/approve` (Duyệt: PENDING_REVIEW -> PUBLISHED, ghi audit log SERVICE_APPROVED)
- `PATCH /api/admin/services/{id}/reject` (Từ chối: -> REJECTED, kèm reason, ghi audit log)

### R3. Business Rules Validation
- **Quyền tạo dịch vụ:** Chỉ Vendor có `verification_status=APPROVED` mới được quyền tạo dịch vụ mới. Trả về lỗi nếu Vendor đang `PENDING`.
- **Validation Dữ Liệu:** 
  - `category_id` không tồn tại/inactive trả về 404/400.
  - Nếu `weather_sensitive=true`, bắt buộc phải truyền `min_wind_kmh` và `max_wave_m`.
- **Submit duyệt:** 
  - Thiếu ảnh (`service_images` rỗng) sẽ bị chặn.
  - Submit khi đang `REJECTED`: Cho phép sửa thông tin và gửi lại (chuyển trạng thái từ `REJECTED` sang `PENDING_REVIEW`).
- **Sửa thông tin:**
  - `DRAFT`: Cho phép sửa tự do.
  - `PUBLISHED`: Bất kỳ sửa đổi nào cũng sẽ chuyển dịch vụ về trạng thái `PENDING_REVIEW` để duyệt lại (trong thời gian này sẽ không nhận được booking mới).
  - Không được phép sửa dịch vụ của Vendor khác (trả về 403).
- **Xóa dịch vụ:**
  - Chỉ cho xóa khi ở trạng thái `DRAFT`.
  - Chặn và báo lỗi rõ ràng nếu cố xóa khi đang `PUBLISHED` hoặc `PAUSED`.
  
### R4. Security & Access Control
- Đầu cuối của Vendor (`/api/vendor/services/**`) chặn các role `CUSTOMER`, `ADMIN` (trả về 403).
- Đầu cuối của Admin (`/api/admin/services/**`) chặn role `VENDOR` (trả về 403).

## Acceptance Criteria

### API Functionality & Validation
- [ ] Tất cả các endpoint thực hiện đúng hành vi, luồng nghiệp vụ và trả về mã HTTP chuẩn.
- [ ] Các Business Rules ở R3 được validate chặt chẽ (đặc biệt các rule chặn Vendor chưa verify, bắt buộc có ảnh, rules thời tiết).
- [ ] Quyền truy cập được phân cấp chuẩn theo R4.

### Test Coverage
- [ ] Implement `CreateServiceUseCaseTest`, `SubmitServiceForReviewUseCaseTest`, `UpdateServiceUseCaseTest`, `ApproveServiceUseCaseTest`, `RejectServiceUseCaseTest`, `DeleteServiceUseCaseTest`.
- [ ] Implement `ServiceControllerTest` (MockMvc) để test phân quyền API.
- [ ] Tất cả test case mô tả trong yêu cầu phải PASS.
