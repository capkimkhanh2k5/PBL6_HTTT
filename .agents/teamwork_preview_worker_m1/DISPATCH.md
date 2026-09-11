# DISPATCH: Worker M1 (Cloudinary & Testing Foundation Infra)

## Working Directory
/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_worker_m1

## Mandatory Reading
Subagents MUST read before starting work:
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/ORIGINAL_REQUEST.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/PROJECT.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_orchestrator_m1/SCOPE.md`
- `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_1/analysis.md`

## Mandatory Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Write Ownership
File boundaries for this milestone:
- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `.env.example`
- `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- `backend/src/main/java/com/danasea/backend/modules/service/application/ports/FileStoragePort.java`
- `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/storage/CloudinaryStorageAdapter.java`
- `backend/src/main/java/com/danasea/backend/modules/service/domain/exceptions/*`
- Configuration bean for Cloudinary/FileStoragePort in `backend/src/main/java/com/danasea/backend/config/ApplicationBeans.java` (or a dedicated config class in `modules/service/infrastructure/config/ServiceInfrastructureConfig.java`)

## Implementation Details
1. **Cloudinary Dependency**:
   - Thêm `com.cloudinary:cloudinary-http44:1.39.0` vào `backend/pom.xml`.
2. **Cấu hình**:
   - Cập nhật `backend/src/main/resources/application.yml` với cấu hình cloudinary và spring multipart.
   - Cập nhật `.env.example` với `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
3. **Mockito MockMaker**:
   - Tạo thư mục và file `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` với nội dung `mock-maker-subclass` để chạy mockito mượt mà trên Java 21 sandbox.
4. **Port & Adapter (Clean Architecture)**:
   - `FileStoragePort`:
     - `String uploadFile(byte[] fileData, String originalFilename, String folder);`
     - `void deleteFile(String fileUrl);`
   - `CloudinaryStorageAdapter` implements `FileStoragePort`:
     - Dùng Cloudinary SDK để upload và delete.
     - Xử lý lỗi ném `FileStorageException`.
5. **Domain Exceptions**:
   - Tạo các class exception trong `com.danasea.backend.modules.service.domain.exceptions`:
     - `InvalidFileTypeException`
     - `FileStorageException`
     - `MaxImagesExceededException`
     - `ServiceNotFoundException`
     - `UnauthorizedServiceAccessException`
     - `ImageNotFoundException`
     - `SafetyDocumentRequiredException`
6. **Compile & Verification**:
   - Chạy lệnh build:
     `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test-compile`
   - Chạy test kiểm chứng không gãy existing tests:
     `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=LoginUseCaseTest`

## Output
Ghi báo cáo kết quả thực thi, build output và test output vào `handoff.md` trong thư mục làm việc của bạn, sau đó gửi message thông báo cho Orchestrator.

## 2026-09-10T03:50:49Z
<USER_REQUEST>
Bạn là Worker triển khai Milestone 1: Cloudinary & Testing Foundation Infrastructure.
Thư mục làm việc: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_worker_m1
Đọc kỹ DISPATCH.md trong thư mục làm việc của bạn và thực hiện các nhiệm vụ:
1. Thêm dependency Cloudinary vào backend/pom.xml.
2. Cấu hình application.yml và .env.example.
3. Tạo MockMaker (mock-maker-subclass) trong backend/src/test/resources/mockito-extensions/.
4. Tạo FileStoragePort và CloudinaryStorageAdapter.
5. Tạo domain exceptions cho module service.
6. Cấu hình bean trong ApplicationBeans hoặc ServiceInfrastructureConfig.
7. Chạy compile và test kiểm chứng không gãy existing tests.
Ghi báo cáo vào handoff.md và gửi send_message cho Orchestrator.
</USER_REQUEST>
