# Handoff Report: Service Lifecycle, Category, Publish Rules & Test Infrastructure

## 1. Observation
- **Category Model & JPA Entity**:
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java`: Khai báo 6 trường riêng: `name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive` kế thừa `BaseDomainModel` (`id`, `createdAt`, `updatedAt`). Chưa có trường `requiresSafetyCert`.
  - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java`: Dòng 13 `@Table(name = "categorys")` (tên bảng là `categorys`), kế thừa `BaseJpaEntity`.
- **Database DDL / Migration**:
  - `backend/src/main/resources/application.yml` (dòng 15-16):
    ```yaml
    jpa:
      hibernate:
        ddl-auto: update
    ```
    Không có Liquibase, Flyway, hay seeder scripts trong repo. Hibernate tự động sinh DDL cập nhật schema khi entity thay đổi.
- **Service Status & Safety Attributes**:
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceStatus.java`: Các giá trị `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `PAUSED`.
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Service.java`: Dòng 29 `private Boolean weatherSensitive;`, dòng 14 `private UUID categoryId;`, dòng 27 `private ServiceStatus status;`.
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/DocStatus.java`: Các giá trị `PENDING`, `APPROVED`, `REJECTED`.
  - Hiện tại `modules/service` chỉ có `domain/models` và `infrastructure/persistence` (entities, repositories). Chưa có use case hay controller nào được triển khai cho Service.
- **Test Runner & Testing Infrastructure**:
  - `backend/pom.xml`: Spring Boot 4.1.1, Java 21, `spring-boot-starter-test`, JUnit 5 Jupiter, Mockito 5.x.
  - Lệnh test thực tế:
    `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=LoginUseCaseTest`
  - Chạy trong sandbox mặc định gặp lỗi ByteBuddy:
    ```
    Caused by: java.lang.IllegalStateException: Could not self-attach to current VM using external process
    ```
  - Chạy với `BypassSandbox: true` thực thi thành công hoàn toàn:
    ```
    [INFO] Running com.danasea.backend.security.authentication.application.usecase.LoginUseCaseTest
    [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.848 s
    [INFO] BUILD SUCCESS
    ```
  - Pattern kiểm thử UseCase hiện hữu: Pure Unit Test (như `LoginUseCaseTest.java`), khởi tạo đối tượng trực tiếp bằng `new UseCase(mockPort, ...)` mà không cần tải Spring ApplicationContext, thời gian chạy dưới 1 giây.

## 2. Logic Chain
1. *Từ Observation về CategoryJpaEntity và application.yml*: Bảng `categorys` được quản lý bởi Hibernate với `ddl-auto: update`. Do đó việc thêm `private Boolean requiresSafetyCert = false;` vào `Category.java` và `CategoryJpaEntity.java` (kèm `@Column(name = "requires_safety_cert")`) sẽ được Hibernate tự sinh `ALTER TABLE categorys ADD COLUMN requires_safety_cert boolean DEFAULT false;` mà không cần viết migration thủ công.
2. *Từ Observation về Service, Category, DocStatus*:
   - Dịch vụ có rủi ro cao hoặc nhạy cảm thời tiết khi: `service.getWeatherSensitive() == true || category.getRequiresSafetyCert() == true`.
   - Để thỏa mãn R3 trong ORIGINAL_REQUEST.md: Khi dịch vụ thuộc diện này, chỉ được phép chuyển trạng thái sang `PUBLISHED` nếu tồn tại ít nhất một `ServiceSafetyDocument` có `status == DocStatus.APPROVED`.
   - Nếu điều kiện không thỏa mãn, use case phải chặn và ném `SafetyDocumentRequiredException`.
3. *Từ Observation về LoginUseCaseTest và cấu trúc thư mục test*:
   - Clean Architecture phân tách rõ ranh giới use case độc lập với framework.
   - Các use case `UploadServiceImageUseCase`, `ReorderServiceImagesUseCase`, `UploadSafetyDocumentUseCase`, `ApproveSafetyDocumentUseCase` đều cần được kiểm thử bằng Pure Unit Test (JUnit 5 + Mockito) đặt tại `src/test/java/com/danasea/backend/modules/service/application/usecase/`.
   - Cloudinary và Database được mock thông qua các interface port/repository (`FileStoragePort`, `JpaServiceRepository`, `JpaServiceImageRepository`, `JpaServiceSafetyDocumentRepository`, `JpaCategoryRepository`).
4. *Từ Observation về ByteBuddy sandbox*:
   - Máy chủ dev có cài đặt OpenJDK 21 (Temurin-21 tại `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
   - Khi chạy test cần chỉ định `JAVA_HOME` và cấu hình `BypassSandbox` (hoặc cấu hình Mockito subclass mock maker) để tránh lỗi self-attach của ByteBuddy.

## 3. Caveats
- Thư mục `backend/src/test/resources` hiện chưa tồn tại; nếu cần cấu hình file `mockito-extensions/org.mockito.plugins.MockMaker` để fix triệt để lỗi dynamic agent trên Java 21 khi chạy trong sandbox, implementer có thể tạo thư mục này.
- Hiện chưa có cơ chế database seeder tự động cho các category; cần có script khởi tạo hoặc SQL seed để gán `requires_safety_cert = true` cho các môn thể thao mạo hiểm (scuba diving, jet ski, parasailing,...).
- Chưa có use cases cũ trong module `service`, toàn bộ use cases của Service Images và Safety Documents sẽ là module mới được viết theo chuẩn Clean Architecture kế thừa pattern của `security` và `account`.

## 4. Conclusion
- Đã xác định đầy đủ và chính xác cấu trúc thực thể `Category`, bảng `categorys`, và cách thức thêm cờ `requires_safety_cert`.
- Đã thiết kế hoàn chỉnh luồng Publish Guard cho Service dựa trên `weather_sensitive` và `requires_safety_cert` cùng điều kiện có safety document `APPROVED`.
- Đã kiểm chứng môi trường test runner (`./mvnw test` với Java 21 Temurin) và phong cách viết test Pure Unit Test bằng JUnit 5 + Mockito.
- Đã đặc tả toàn diện 4 Acceptance Criteria test suites (`UploadServiceImageUseCaseTest`, `ReorderServiceImagesUseCaseTest`, `UploadSafetyDocumentUseCaseTest`, `ApproveSafetyDocumentUseCaseTest`) với đầy đủ các scenario tích cực, tiêu cực và phân quyền.
- Chi tiết báo cáo khảo sát đã được lưu tại:
  `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_3/analysis.md`

## 5. Verification Method
- **Lệnh chạy test mẫu kiểm chứng test infrastructure**:
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=LoginUseCaseTest
  ```
  *(Kết quả mong đợi: `BUILD SUCCESS`, 5 tests run, 0 failures).*
- **Các file cần kiểm tra trong codebase**:
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Service.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceJpaEntity.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceSafetyDocument.java`
  - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceImage.java`
  - `backend/src/test/java/com/danasea/backend/security/authentication/application/usecase/LoginUseCaseTest.java`
- **Điều kiện phủ nhận (Invalidation Conditions)**:
  - Nếu dự án chuyển sang dùng Liquibase/Flyway thay vì `ddl-auto: update`, việc tự động cập nhật schema sẽ không còn hiệu lực và bắt buộc phải viết changelog XML/SQL.
  - Nếu chuyển sang môi trường Java không có quyền dynamic agent attach, cần bổ sung `mock-maker-subclass` trong `src/test/resources/mockito-extensions`.
