# Scope: Milestone 1 — Cloudinary & Testing Foundation Infrastructure

## Architecture
- Module: `modules/service/application/ports` & `infrastructure/storage`
- Dependencies: none

## Scope
1. **Dependency & Build**:
   - Thêm `com.cloudinary:cloudinary-http44:1.39.0` vào `backend/pom.xml`.
2. **Configuration**:
   - Cấu hình thuộc tính `cloudinary` và `spring.servlet.multipart` trong `backend/src/main/resources/application.yml`.
   - Cập nhật `.env.example` với các biến môi trường Cloudinary (`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`).
3. **Test Infrastructure Fix (Sandbox Support)**:
   - Tạo thư mục và file `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` với nội dung `mock-maker-subclass` để đảm bảo Mockito chạy trơn tru trong sandbox Java 21 mà không bị lỗi ByteBuddy attach.
4. **Ports & Adapters (Clean Architecture)**:
   - Tạo port `backend/src/main/java/com/danasea/backend/modules/service/application/ports/FileStoragePort.java`.
   - Tạo adapter `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/storage/CloudinaryStorageAdapter.java` implements `FileStoragePort`.
   - Đăng ký Bean trong `ApplicationBeans.java` (hoặc configuration class tương thích).
5. **Domain Exceptions**:
   - Tạo các exception cơ sở cho module service: `InvalidFileTypeException.java`, `FileStorageException.java`, `MaxImagesExceededException.java`, `ServiceNotFoundException.java`, `UnauthorizedServiceAccessException.java`.

## Verification
- Chạy `./mvnw clean compile` thành công.
- Unit test adapter hoặc mock port thành công.
