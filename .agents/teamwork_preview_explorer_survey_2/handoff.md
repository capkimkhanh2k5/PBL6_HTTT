# HANDOFF REPORT: VENDOR DOCUMENTS PATTERN & SAFETY DOCUMENTS SURVEY (R2 & R3)

**Thư mục báo cáo**: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_2`  
**Handoff Type**: Hard (Nhiệm vụ khảo sát hoàn tất đầy đủ 100%)  
**Đối tượng bàn giao**: Orchestrator và Implementer Agent

---

## 1. OBSERVATION (Quan sát thực tế trong codebase)

1. **Thực trạng `vendor_documents` hiện tại**:
   - `DocStatus.java` (`backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/DocStatus.java`, dòng 3-5):
     ```java
     public enum DocStatus {
         PENDING, APPROVED, REJECTED
     }
     ```
   - `DocType.java` (`backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/DocType.java`, dòng 3-5):
     ```java
     public enum DocType {
         BUSINESS_LICENSE, SAFETY_CERT
     }
     ```
   - `VendorDocument.java` (`backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/VendorDocument.java`, dòng 12-19):
     ```java
     public class VendorDocument extends BaseDomainModel {
         private UUID vendorId;
         private DocType docType;
         private String fileUrl;
         private DocStatus status;
         private UUID reviewedBy;
         private OffsetDateTime reviewedAt;
     }
     ```
   - `VendorDocumentJpaEntity.java` (`backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/entities/VendorDocumentJpaEntity.java`, dòng 16-32):
     ```java
     @Table(name = "vendor_documents")
     public class VendorDocumentJpaEntity extends BaseJpaEntity {
         private UUID vendorId;
         @Enumerated(EnumType.STRING)
         private DocType docType;
         private String fileUrl;
         @Enumerated(EnumType.STRING)
         private DocStatus status;
         private UUID reviewedBy;
         private OffsetDateTime reviewedAt;
     }
     ```
   - `JpaVendorDocumentRepository.java` (`backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorDocumentRepository.java`, dòng 11-12):
     ```java
     public interface JpaVendorDocumentRepository extends JpaRepository<VendorDocumentJpaEntity, UUID> {}
     ```
   - *Kết quả tìm kiếm*: Chưa có Use Case, Controller, DTO hay Test nào cho `vendor_documents` trong module `vendor`.

2. **Thực trạng `service_safety_documents` và `service` hiện tại**:
   - `ServiceSafetyDocument.java` (`backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceSafetyDocument.java`, dòng 12-18):
     ```java
     public class ServiceSafetyDocument extends BaseDomainModel {
         private UUID serviceId;
         private String fileUrl;
         private DocStatus status;
         private UUID reviewedBy;
         private OffsetDateTime reviewedAt;
     }
     ```
   - `ServiceSafetyDocumentJpaEntity.java` (`backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceSafetyDocumentJpaEntity.java`, dòng 16-29):
     ```java
     @Table(name = "service_safety_documents")
     public class ServiceSafetyDocumentJpaEntity extends BaseJpaEntity {
         private UUID serviceId;
         private String fileUrl;
         @Enumerated(EnumType.STRING)
         private DocStatus status;
         private UUID reviewedBy;
         private OffsetDateTime reviewedAt;
     }
     ```
   - `JpaServiceSafetyDocumentRepository.java` (`backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceSafetyDocumentRepository.java`, dòng 11-12):
     ```java
     public interface JpaServiceSafetyDocumentRepository extends JpaRepository<ServiceSafetyDocumentJpaEntity, UUID> {}
     ```
   - `Service.java` (`backend/src/main/java/com/danasea/backend/modules/service/domain/models/Service.java`, dòng 27-29):
     ```java
     private ServiceStatus status;
     private String waiverContent;
     private Boolean weatherSensitive;
     ```
   - `Category.java` (`backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java`, dòng 12-18) và `CategoryJpaEntity.java` (`.../CategoryJpaEntity.java`, dòng 16-27): Hiện CHƯA có trường `requiresSafetyCert`.

3. **Cơ chế Database Schema Management**:
   - `application.yml` (`backend/src/main/resources/application.yml`, dòng 15-16):
     ```yaml
     jpa:
       hibernate:
         ddl-auto: update
     ```
     -> Hệ thống tự động cập nhật schema DB từ JPA Entities, không dùng script migration SQL riêng.

4. **Cơ chế Authentication & Authorization**:
   - `SecurityConfig.java` (`backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`, dòng 26-28):
     `@EnableWebSecurity` và `@EnableMethodSecurity`.
   - `JwtAuthenticationFilter.java` (`backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/security/JwtAuthenticationFilter.java`, dòng 62-78):
     Map các role từ AuthorizationSubject thành `ROLE_ADMIN`, `ROLE_VENDOR`, `ROLE_CUSTOMER` và lưu principal là user email (`subject.email()`).
   - `AuthorizationController.java` (`backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationController.java`, dòng 13, 19):
     Sử dụng `@PreAuthorize("hasRole('ADMIN')")` và `@PreAuthorize("hasRole('VENDOR')")`.

5. **Môi trường Biên dịch & Kiểm thử (Build Environment)**:
   - JDK: Temurin OpenJDK 21 tại `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`.
   - Lệnh `./mvnw test-compile` hoàn tất thành công trong 2.1s (194 source files compiled).
   - Chạy `./mvnw test -Dtest=LoginUseCaseTest` trong môi trường sandboxed gặp lỗi ByteBuddy:
     `Could not initialize inline Byte Buddy mock maker. It appears as if your JDK does not supply a working agent attachment mechanism.`

---

## 2. LOGIC CHAIN (Suy luận từng bước từ Quan sát đến Kết luận)

1. **Từ Quan sát 1 & 2**: Cả `vendor_documents` và `service_safety_documents` đều có sẵn Domain Model và JPA Entity tương đồng nhau (gồm các trường: foreign key ID, `fileUrl`, `status` [PENDING, APPROVED, REJECTED], `reviewedBy`, `reviewedAt`), nhưng chưa có tầng Application (UseCases) và Presentation (Controllers).
   -> Do đó, việc triển khai R2 hoàn toàn có thể kế thừa và phát triển đồng bộ cấu trúc Clean Architecture chuẩn cho `service_safety_documents` dựa trên pattern này.

2. **Từ Quan sát 1 & 2 về quy trình kiểm duyệt**: Khi Admin từ chối chứng chỉ, việc không có trường ghi nhận lý do sẽ gây khó khăn cho Vendor và vi phạm yêu cầu nghiệp vụ về "review notes" được nêu trong DISPATCH.md.
   -> Cần bổ sung trường `rejectionReason` (String) vào cả `ServiceSafetyDocument` và `ServiceSafetyDocumentJpaEntity`.

3. **Từ Quan sát 2 & Yêu cầu R3**: Đề bài yêu cầu:
   *"Dịch vụ có `weather_sensitive=true` hoặc liên quan đến hoạt động rủi ro cao nhưng chưa có safety documents nào được `APPROVED` thì không được set thành `PUBLISHED`... Thêm cờ `requires_safety_cert` gắn theo category thay vì hardcode."*
   Quan sát thấy `Category` và `CategoryJpaEntity` chưa có cờ này.
   -> Bắt buộc phải thêm trường `Boolean requiresSafetyCert` vào `Category.java` và `CategoryJpaEntity.java` (mặc định `false`). Khi thẩm định publish service, kiểm tra nếu `(category.requiresSafetyCert || service.weatherSensitive)` thì bắt buộc phải thỏa mãn `safetyDocRepository.existsByServiceIdAndStatus(serviceId, DocStatus.APPROVED) == true`.

4. **Từ Quan sát 4**: Hệ thống sử dụng `@EnableMethodSecurity` và `JwtAuthenticationFilter` cấp quyền dạng `ROLE_VENDOR` và `ROLE_ADMIN`.
   -> Các API của Vendor (`POST /api/vendor/services/{id}/safety-documents`) sẽ dùng `@PreAuthorize("hasRole('VENDOR')")`, đồng thời ở tầng Application/UseCase phải kiểm tra Resource Ownership: Vendor ID của service phải bằng Vendor ID của user đăng nhập.
   -> Các API của Admin (`GET /api/admin/services/{id}/safety-documents`, `PATCH .../approve`, `PATCH .../reject`) sẽ dùng `@PreAuthorize("hasRole('ADMIN')")`.

5. **Từ Quan sát 5 về lỗi ByteBuddy của Mockito trong Sandbox**: Khi viết Unit Test cho Use Case bằng Mockito, do macOS sandbox chặn dynamic agent attachment của ByteBuddy inline mock maker.
   -> Implementer chỉ cần cấu hình `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` chứa `mock-maker-subclass`, mockito sẽ dùng subclassing thông thường và chạy test pass 100% trong sandbox mà không cần quyền dynamic agent.

---

## 3. CAVEATS (Vấn đề cần lưu ý & Giả định)

1. **File Upload Storage**: Đề bài yêu cầu upload lưu trữ trên Cloudinary. Để tuân thủ nghiêm ngặt Clean Architecture Rule 6, tầng Application không nên import trực tiếp thư viện Cloudinary mà cần định nghĩa `FileUploadPort`. Tầng Infrastructure sẽ cài đặt adapter `CloudinaryFileUploadAdapter` (hoặc mock/in-memory adapter cho testing).
2. **Endpoint Review Routes**: DISPATCH.md liệt kê:
   - `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve|reject`
   Đề xuất triển khai 2 endpoint tường minh:
   - `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve`
   - `PATCH /api/admin/services/{id}/safety-documents/{docId}/reject` (kèm body `{ "rejectionReason": "..." }`)
   Cách này rõ ràng theo RESTful và dễ viết test hơn.
3. **Môi trường Test**: Khi Implementer chạy test surefire trên macOS sandbox, cần lưu ý cấu hình `mock-maker-subclass` như đã phân tích.

---

## 4. CONCLUSION (Kết luận & Đề xuất hành động cho Implementer)

1. **Cấu trúc dữ liệu**:
   - Mở rộng `Category.java` và `CategoryJpaEntity.java`: thêm `requiresSafetyCert`.
   - Mở rộng `ServiceSafetyDocument.java` và `ServiceSafetyDocumentJpaEntity.java`: thêm `rejectionReason`.
   - Mở rộng `JpaServiceSafetyDocumentRepository`: thêm `findAllByServiceIdOrderByCreatedAtDesc`, `findByIdAndServiceId`, `existsByServiceIdAndStatus`.
   - Mở rộng `JpaVendorRepository`: thêm `findByUserId(UUID userId)`.

2. **Các Use Case cần tạo mới**:
   - `UploadSafetyDocumentUseCase`: Upload chứng chỉ, gán trạng thái `PENDING`, kiểm tra quyền sở hữu service của Vendor.
   - `GetSafetyDocumentsUseCase`: Lấy danh sách chứng chỉ an toàn của service cho Admin.
   - `ApproveSafetyDocumentUseCase`: Chuyển trạng thái sang `APPROVED`, gán `reviewedBy = adminId`, `reviewedAt = now`.
   - `RejectSafetyDocumentUseCase`: Chuyển trạng thái sang `REJECTED`, gán `reviewedBy = adminId`, `reviewedAt = now`, `rejectionReason = reason`.
   - `ValidateServicePublishableUseCase` (hoặc method trong service publishing): Kiểm tra điều kiện có chứng chỉ `APPROVED` nếu service có `weatherSensitive == true` hoặc category có `requiresSafetyCert == true`.

3. **Các Controller cần tạo mới**:
   - `VendorSafetyDocumentController` (`/api/vendor/services/{id}/safety-documents`)
   - `AdminSafetyDocumentController` (`/api/admin/services/{id}/safety-documents/**`)

4. **Báo cáo chi tiết đã sẵn sàng**:
   Toàn bộ phân tích thiết kế, cấu trúc bảng, luồng API và danh sách file cụ thể đã được lưu tại:
   `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_2/analysis.md`.

---

## 5. VERIFICATION METHOD (Phương pháp kiểm tra độc lập)

1. **Kiểm tra biên dịch dự án**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/backend
   ./mvnw test-compile
   ```
   *Kỳ vọng*: BUILD SUCCESS, không có lỗi cú pháp hay thiếu import.

2. **Kiểm tra unit test của Use Cases**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   export PATH=$JAVA_HOME/bin:$PATH
   cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/backend
   ./mvnw test -Dtest=UploadSafetyDocumentUseCaseTest,ApproveSafetyDocumentUseCaseTest
   ```
   *Kỳ vọng*: Toàn bộ unit test cho upload, approve, reject và validate publish service đều PASS.

3. **Điều kiện vô hiệu hóa (Invalidation conditions)**:
   - Nếu dự án thay đổi cơ chế xác thực không dùng JWT email principal.
   - Nếu `CategoryJpaEntity` bị đổi tên bảng hoặc không dùng `ddl-auto: update`.
