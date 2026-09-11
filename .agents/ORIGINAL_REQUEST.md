# Original User Request

## 2026-09-10T03:40:36Z

# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview
> Requested team: [none — teamwork routes from the description]

Triển khai module Service Images & Safety Documents cho dự án (PBL6_HTTT). Bao gồm các API để upload (lưu trữ trên Cloudinary), xóa, và đổi thứ tự ảnh của service, cùng với các API quản lý (upload, phê duyệt/từ chối) chứng chỉ an toàn cho các dịch vụ rủi ro cao. Đồng thời, viết các test case để đảm bảo business rules.

Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api
Integrity mode: development

## Requirements

### R1. API Quản lý Service Images
- Kế thừa và sử dụng các cấu hình ORM/Database và Cloudinary đã có sẵn trong project.
- Triển khai API `POST /api/vendor/services/{id}/images`: Upload ảnh qua Cloudinary, tự động set `sort_order`.
- Triển khai API `DELETE /api/vendor/services/{id}/images/{imageId}`.
- Triển khai API `PATCH /api/vendor/services/{id}/images/reorder`: Đổi `sort_order` hàng loạt.

### R2. API Quản lý Safety Documents
- Sử dụng mô hình tương tự module `vendor_documents` hiện tại để thiết kế.
- Triển khai API `POST /api/vendor/services/{id}/safety-documents`: Upload chứng chỉ an toàn (áp dụng cho dịch vụ rủi ro cao).
- Triển khai API `GET /api/admin/services/{id}/safety-documents`.
- Triển khai API `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve|reject`.

### R3. Business Rules & Cấu hình (Core Logic)
- Dịch vụ có `weather_sensitive=true` hoặc liên quan đến hoạt động rủi ro cao nhưng chưa có safety documents nào được `APPROVED` thì không được set thành `PUBLISHED` (ngay cả khi Admin approve service).
- Thêm cờ `requires_safety_cert` (hoặc cấu trúc tương tự) gắn theo category thay vì hardcode, để xác định dịch vụ nào bắt buộc phải có chứng chỉ an toàn.

## Acceptance Criteria

### Unit/Integration Tests (Programmatic Verification)
- [ ] Các bài test phải chạy tự động thành công (Pass) thông qua test runner của dự án.
- [ ] `UploadServiceImageUseCaseTest`: Bao gồm case upload thành công (sort_order tự tăng), case vượt quá số ảnh tối đa (bị chặn), và định dạng file không hợp lệ (trả về 400).
- [ ] `ReorderServiceImagesUseCaseTest`: Bao gồm case đổi thứ tự thành công, và case thao túng truyền imageId của service khác (trả về 403/404).
- [ ] `UploadSafetyDocumentUseCaseTest` & `ApproveSafetyDocumentUseCaseTest`: Tuân thủ đúng pattern của vendor_documents, cover được logic publish service dựa trên trạng thái của safety document và category cờ `requires_safety_cert`.

## 2026-09-10T04:10:00Z

# Teamwork Project Prompt — Draft (Resume Session)

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview
> Requested team: [none — teamwork routes from the description]

Tiếp tục triển khai module Service Images & Safety Documents cho dự án (PBL6_HTTT). Tiến trình trước đó đã hoàn thành Milestone 1 (M1) bao gồm cài đặt Cloudinary, config Mockito, FileStoragePort, các Exception cơ bản và cờ requires_safety_cert.
Yêu cầu đợt này: Đọc `PROJECT.md` và bỏ qua M1. Bắt đầu thực thi tiếp từ Milestone 2 (M2) đến Milestone 5 (M5).

Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api
Integrity mode: development

## Requirements

### R1. API Quản lý Service Images (Milestone 2)
- Kế thừa cấu trúc từ M1.
- Triển khai API `POST /api/vendor/services/{id}/images`: Upload ảnh qua Cloudinary (dùng CloudinaryStorageAdapter), tự động set `sort_order`.
- Triển khai API `DELETE /api/vendor/services/{id}/images/{imageId}`.
- Triển khai API `PATCH /api/vendor/services/{id}/images/reorder`: Đổi `sort_order` hàng loạt.

### R2. API Quản lý Safety Documents (Milestone 3)
- Sử dụng mô hình tương tự module `vendor_documents` hiện tại để thiết kế.
- Triển khai API `POST /api/vendor/services/{id}/safety-documents`: Upload chứng chỉ an toàn.
- Triển khai API `GET /api/admin/services/{id}/safety-documents`.
- Triển khai API `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve|reject`.

### R3. Business Rules & Cấu hình (Milestone 4)
- Triển khai Publish Guard: Dịch vụ có `weather_sensitive=true` hoặc category có `requires_safety_cert=true` nhưng chưa có safety documents nào được `APPROVED` thì không được set thành `PUBLISHED`.

## Acceptance Criteria (Milestone 5)

### Unit/Integration Tests (Programmatic Verification)
- [ ] Các bài test phải chạy tự động thành công (Pass) thông qua test runner của dự án.
- [ ] `UploadServiceImageUseCaseTest` & `ReorderServiceImagesUseCaseTest`: Chạy pass 100%.
- [ ] `UploadSafetyDocumentUseCaseTest` & `ApproveSafetyDocumentUseCaseTest`: Chạy pass 100%.

