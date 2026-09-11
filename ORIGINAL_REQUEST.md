# Original User Request

## Initial Request — 2026-09-10T03:40:19Z

Triển khai module Categories (Admin quản lý, public đọc) cho dự án Backend (Java/Spring Boot) bao gồm các API quản lý và Unit Tests tương ứng. Tính năng này yêu cầu triển khai theo kiến trúc codebase hiện có của hệ thống.

Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_categories_module_api/backend
Integrity mode: development
Requested team: Full team

## Requirements

### R1. Triển khai các API Categories
- `GET /api/categories` — Public, trả về cây phân cấp cha/con, chỉ lấy các category có `is_active=true`. Không được đệ quy vô hạn khi build tree.
- `GET /api/admin/categories` — Admin xem tất cả (kể cả inactive).
- `POST /api/admin/categories` — Tạo category (name, name_en, slug, parent_id, icon_url).
- `PATCH /api/admin/categories/{id}` — Sửa category.
- `PATCH /api/admin/categories/{id}/deactivate` — Set `is_active=false` (soft delete). Yêu cầu chặn và throw exception (ví dụ: `CategoryHasActiveServicesException`) nếu category đang có services active tham chiếu tới.

### R2. Đảm bảo cấu trúc Database và Codebase
- Tạo entity `Category` với các field tương ứng (id, name, name_en, slug, parent_id, icon_url, is_active).
- Tuân thủ kiến trúc phân tầng hiện tại của dự án (Controller, Service, Repository, UseCase nếu có).

## Acceptance Criteria

### Unit Tests (Verification)
Mọi Unit Tests dưới đây phải được implement đầy đủ và Pass 100% khi chạy lệnh test của Maven/Gradle:
- [ ] `CreateCategoryUseCaseTest`: Tạo category gốc thành công (`parent_id=null`).
- [ ] `CreateCategoryUseCaseTest`: Tạo category con với `parent_id` hợp lệ thành công.
- [ ] `CreateCategoryUseCaseTest`: `parent_id` trỏ tới category không tồn tại trả về lỗi 404.
- [ ] `CreateCategoryUseCaseTest`: Bị trùng `slug` sẽ ném ra `SlugAlreadyExistsException`.
- [ ] `CreateCategoryUseCaseTest`: Test chống vòng lặp phân cấp: set `parent_id` của category A trỏ về chính category con của nó (A → B → A) → phải bị chặn.
- [ ] `GetCategoryTreeUseCaseTest`: API trả đúng cấu trúc cha-con nhiều tầng.
- [ ] `GetCategoryTreeUseCaseTest`: Category `is_active=false` không xuất hiện ở API public nhưng vẫn thấy ở API admin.
- [ ] `DeactivateCategoryUseCaseTest`: Throw exception `CategoryHasActiveServicesException` chặn deactivate khi category đang có services active tham chiếu.
