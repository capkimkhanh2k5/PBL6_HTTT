# BÁO CÁO KHẢO SÁT CHI TIẾT: VENDOR DOCUMENTS PATTERN & THIẾT KẾ SAFETY DOCUMENTS (R2 & R3)

**Thư mục làm việc**: `.agents/teamwork_preview_explorer_survey_2`  
**Ngày thực hiện**: 2026-09-10  
**Đối tượng khảo sát**: Module `vendor_documents`, Module `service`, Authentication/Authorization Framework và Business Rules R2/R3 cho dự án `PBL6_HTTT (DANASEA)`.

---

## 1. TỔNG QUAN HIỆN TRẠNG CODEBASE LIÊN QUAN ĐẾN DOCUMENTS

### 1.1. Hiện trạng Module `vendor_documents`
Khảo sát toàn bộ mã nguồn backend phát hiện mã liên quan đến `vendor_documents` hiện mới chỉ có ở tầng Domain và Infrastructure Persistence cơ bản:
- **Domain Enum**:
  - `DocStatus.java` (`com.danasea.backend.modules.vendor.domain.models.DocStatus`):
    ```java
    public enum DocStatus {
        PENDING, APPROVED, REJECTED
    }
    ```
  - `DocType.java` (`com.danasea.backend.modules.vendor.domain.models.DocType`):
    ```java
    public enum DocType {
        BUSINESS_LICENSE, SAFETY_CERT
    }
    ```
- **Domain Model**:
  - `VendorDocument.java` (`com.danasea.backend.modules.vendor.domain.models.VendorDocument`):
    - Kế thừa `BaseDomainModel` (`id`, `createdAt`, `updatedAt`).
    - Các trường:
      - `UUID vendorId`: Khóa ngoại logic tới Vendor.
      - `DocType docType`: Loại tài liệu (`BUSINESS_LICENSE` hoặc `SAFETY_CERT`).
      - `String fileUrl`: Đường dẫn file tài liệu.
      - `DocStatus status`: Trạng thái kiểm duyệt (`PENDING`, `APPROVED`, `REJECTED`).
      - `UUID reviewedBy`: ID của Admin thực hiện duyệt.
      - `OffsetDateTime reviewedAt`: Thời điểm thực hiện duyệt.
- **JPA Entity**:
  - `VendorDocumentJpaEntity.java` (`com.danasea.backend.modules.vendor.infrastructure.persistence.entities.VendorDocumentJpaEntity`):
    - Bảng: `@Table(name = "vendor_documents")`.
    - Kế thừa `BaseJpaEntity` (`id` UUID sinh tự động, `createdAt` và `updatedAt` tự sinh qua `@CreationTimestamp` và `@UpdateTimestamp`).
    - Các trường cột tương ứng: `vendorId`, `docType` (`@Enumerated(EnumType.STRING)`), `fileUrl`, `status` (`@Enumerated(EnumType.STRING)`), `reviewedBy`, `reviewedAt`.
- **Repository**:
  - `JpaVendorDocumentRepository.java` (`com.danasea.backend.modules.vendor.infrastructure.persistence.repositories.JpaVendorDocumentRepository`):
    - Kế thừa `JpaRepository<VendorDocumentJpaEntity, UUID>`. Hiện chưa có method tùy biến nào.
- **UseCase / DTO / Controller / Test**:
  - Chưa được triển khai trong module `vendor`.

### 1.2. Hiện trạng Module `service` liên quan đến `service_safety_documents`
Khảo sát module `service` cho thấy cấu trúc đã được định hình sẵn một phần đối xứng với `vendor_documents`:
- **Domain Enum**:
  - `DocStatus.java` (`com.danasea.backend.modules.service.domain.models.DocStatus`): Gồm 3 giá trị `PENDING`, `APPROVED`, `REJECTED`.
  - `ServiceStatus.java` (`com.danasea.backend.modules.service.domain.models.ServiceStatus`): Gồm `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `PAUSED`.
- **Domain Model**:
  - `ServiceSafetyDocument.java` (`com.danasea.backend.modules.service.domain.models.ServiceSafetyDocument`):
    - Kế thừa `BaseDomainModel`.
    - Các trường: `UUID serviceId`, `String fileUrl`, `DocStatus status`, `UUID reviewedBy`, `OffsetDateTime reviewedAt`.
- **JPA Entity**:
  - `ServiceSafetyDocumentJpaEntity.java` (`com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSafetyDocumentJpaEntity`):
    - Bảng: `@Table(name = "service_safety_documents")`.
    - Kế thừa `BaseJpaEntity`.
    - Các cột: `serviceId` (UUID), `fileUrl` (String), `status` (DocStatus STRING), `reviewedBy` (UUID), `reviewedAt` (OffsetDateTime).
- **Repository**:
  - `JpaServiceSafetyDocumentRepository.java`: Kế thừa `JpaRepository<ServiceSafetyDocumentJpaEntity, UUID>`, hiện rỗng.
- **Cơ chế Database Migration**:
  - Dự án cấu hình `spring.jpa.hibernate.ddl-auto: update` trong `application.yml`.
  - Không sử dụng file SQL migration riêng lẻ (Flyway/Liquibase). Hibernate tự động ánh xạ và cập nhật schema PostgreSQL trực tiếp từ JPA Entities khi ứng dụng khởi chạy.

### 1.3. Tiêu chuẩn Kiến trúc (Clean Architecture Blueprint)
Theo tài liệu kiến trúc dự án `backend/docs/Clean_Architecture_Rules.md` và mã nguồn chuẩn tại `modules/account` cùng `security/authentication`:
1. **Quy tắc phụ thuộc (Dependency Rule)**: Presentation -> Application -> Domain <- Infrastructure.
2. **Độc lập Framework (Framework Independence)**: Tầng Domain và Application không chứa annotation của Spring Framework (`@Service`, `@Component`). Các Use Case được khai báo dưới dạng Spring `@Bean` trong tầng Infrastructure/Config (`ApplicationBeans.java` hoặc `ServiceSafetyDocumentBeans.java`).
3. **Abstractions/Ports**: Giao tiếp ra ngoài (như lưu trữ file Cloudinary) phải thông qua Port interface (ví dụ `FileUploadPort`) được định nghĩa trong tầng Application và implement ở tầng Infrastructure.

---

## 2. PHÂN TÍCH VÒNG ĐỜI TRẠNG THÁI CỦA DOCUMENT (DOCUMENT STATUS LIFECYCLE)

### 2.1. Các trạng thái trong `DocStatus`
| Trạng thái | Ý nghĩa nghiệp vụ | Ai tạo/chuyển đổi |
| :--- | :--- | :--- |
| **`PENDING`** | Tài liệu vừa được tải lên bởi Vendor, đang chờ Admin kiểm duyệt. | Vendor (khi gọi API upload document) |
| **`APPROVED`** | Chứng chỉ hợp lệ, đã được Admin phê duyệt. Đủ điều kiện để Service được Publish. | Admin (khi gọi API approve) |
| **`REJECTED`** | Chứng chỉ không hợp lệ (mờ, hết hạn, sai thông tin), bị Admin từ chối. | Admin (khi gọi API reject kèm lý do) |

### 2.2. Ma trận chuyển đổi trạng thái (State Transition Matrix)
```
       [Vendor Upload File]
                 │
                 ▼
        ┌─────────────────┐
        │     PENDING     │
        └────────┬────────┘
                 │
     ┌───────────┴───────────┐
     │ (Admin Approve)       │ (Admin Reject)
     ▼                       ▼
┌──────────┐           ┌──────────┐
│ APPROVED │           │ REJECTED │
└──────────┘           └─────┬────┘
                             │ (Vendor re-uploads new file)
                             ▼
                    ┌─────────────────┐
                    │  PENDING (mới)  │
                    └─────────────────┘
```

- **Chuyển dịch 1: Khởi tạo (`null` -> `PENDING`)**
  - **Tác nhân**: Vendor.
  - **Hành động**: Gọi `POST /api/vendor/services/{id}/safety-documents`.
  - **Ghi nhận**: `status = PENDING`, `reviewedBy = null`, `reviewedAt = null`, `createdAt = now`.
- **Chuyển dịch 2: Phê duyệt (`PENDING` -> `APPROVED`)**
  - **Tác nhân**: Admin.
  - **Hành động**: Gọi `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve`.
  - **Ghi nhận**: `status = APPROVED`, `reviewedBy = currentAdminUserId`, `reviewedAt = OffsetDateTime.now()`, `rejectionReason = null`.
- **Chuyển dịch 3: Từ chối (`PENDING` -> `REJECTED`)**
  - **Tác nhân**: Admin.
  - **Hành động**: Gọi `PATCH /api/admin/services/{id}/safety-documents/{docId}/reject`.
  - **Ghi nhận**: `status = REJECTED`, `reviewedBy = currentAdminUserId`, `reviewedAt = OffsetDateTime.now()`, `rejectionReason = request.reason`.
- **Chuyển dịch 4: Tái nộp chứng chỉ mới**
  - Khi một chứng chỉ bị `REJECTED`, Vendor không ghi đè trực tiếp trạng thái của bản ghi cũ mà tải lên một tài liệu mới (tạo record `ServiceSafetyDocument` mới với `status = PENDING`). Bản ghi cũ được giữ lại phục vụ mục đích audit log và lịch sử kiểm duyệt.

### 2.3. Bổ sung trường Audit & Ghi chú kiểm duyệt
Hiện tại `ServiceSafetyDocumentJpaEntity` và `ServiceSafetyDocument` chỉ có:
- `reviewedBy` (UUID)
- `reviewedAt` (OffsetDateTime)
- `createdAt` / `updatedAt` (kế thừa từ `BaseJpaEntity`)

**Đề xuất cải tiến cấp thiết**:
Bổ sung trường `rejectionReason` (kiểu `String`) vào cả Domain Model và JPA Entity:
- Trong Domain Model: `private String rejectionReason;`
- Trong JPA Entity: `@Column(name = "rejection_reason") private String rejectionReason;`
- **Lợi ích**: Giúp Admin cung cấp lý do cụ thể khi từ chối chứng chỉ (ví dụ: "Ảnh bị mờ, không rõ ngày cấp", "Chứng chỉ lặn đã hết hạn"), đồng thời Vendor nhận được thông tin rõ ràng để khắc phục. Hibernate `ddl-auto: update` sẽ tự động tạo thêm cột này vào PostgreSQL mà không gây downtime hay lỗi dữ liệu cũ.

---

## 3. PHÂN TÍCH CƠ CHẾ XÁC THỰC & PHÂN QUYỀN (AUTHORIZATION & AUTHENTICATION)

### 3.1. Cơ chế xác thực người dùng hiện tại trong codebase
1. **Luồng JWT Authentication**:
   - `JwtAuthenticationFilter` (`com.danasea.backend.security.authentication.infrastructure.security.JwtAuthenticationFilter`) can thiệp trước mọi request:
     - Lấy header `Authorization: Bearer <token>`.
     - Giải mã email từ token qua `TokenProvider`.
     - Lấy thông tin người dùng qua `UserAccountPort.findByEmail(email)` và `AuthorizationPort.findSubjectByEmail(email)`.
     - Tạo `UsernamePasswordAuthenticationToken` với:
       - Principal: `subject.email()` (String email của user).
       - Authorities: Danh sách `SimpleGrantedAuthority` bao gồm `ROLE_<ROLE>` (ví dụ `ROLE_VENDOR`, `ROLE_ADMIN`, `ROLE_CUSTOMER`) và các permissions chi tiết.
     - Lưu vào `SecurityContextHolder.getContext().setAuthentication(authentication)`.
2. **Cấu hình Security**:
   - `SecurityConfig` (`com.danasea.backend.modules.systemconfig.SecurityConfig`) có:
     - `@EnableWebSecurity` và `@EnableMethodSecurity`.
     - `anyRequest().authenticated()`.
     - Chỉ permitAll các endpoint auth (`/api/auth/**`), actuator, swagger.

### 3.2. Phân quyền cho VENDOR (Vendor Authorization)
- **Cấp độ Route/Method**:
  - Áp dụng `@PreAuthorize("hasRole('VENDOR')")` tại controller hoặc phương thức API.
- **Cấp độ Tài nguyên (Resource Ownership Authorization - Rất quan trọng)**:
  - Khi Vendor gọi `POST /api/vendor/services/{id}/safety-documents`:
    1. Lấy thông tin tài khoản đăng nhập từ Security Context (email qua `principal.getName()`).
    2. Tìm `User` qua `accountInternalApi.findUserByEmail(email)`.
    3. Tìm `Vendor` qua `jpaVendorRepository.findByUserId(user.getId())`. Nếu không tìm thấy hồ sơ Vendor -> ném `AccessDeniedException` (403 Forbidden).
    4. Tìm `Service` theo `{id}` qua `jpaServiceRepository.findById(serviceId)`. Nếu không tồn tại -> ném `ServiceNotFoundException` (404 Not Found).
    5. **Kiểm tra quyền sở hữu**: Đối chiếu `service.getVendorId()` với `vendor.getId()`.
       - Nếu không trùng khớp -> ném `AccessDeniedException("You do not own this service")` (403 Forbidden).
       - Ngăn chặn triệt để lỗ hổng IDOR (Insecure Direct Object Reference).

### 3.3. Phân quyền cho ADMIN (Admin Authorization)
- **Cấp độ Route/Method**:
  - Áp dụng `@PreAuthorize("hasRole('ADMIN')")` cho các endpoint kiểm duyệt:
    - `GET /api/admin/services/{id}/safety-documents`
    - `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve`
    - `PATCH /api/admin/services/{id}/safety-documents/{docId}/reject`
- **Cấp độ Nghiệp vụ**:
  - Admin có quyền xem và duyệt chứng chỉ của mọi Service trong toàn hệ thống.
  - Khi duyệt/từ chối, lấy `user.getId()` của Admin từ email hiện tại để gán vào trường `reviewedBy`.

---

## 4. THIẾT KẾ CẤU TRÚC BẢNG & MODEL

### 4.1. So sánh `vendor_documents` vs `service_safety_documents`
| Tiêu chí | `vendor_documents` | `service_safety_documents` |
| :--- | :--- | :--- |
| **Cấp quản lý** | Cấp đối tác (Vendor-level) | Cấp dịch vụ cụ thể (Service-level) |
| **Khóa ngoại sở hữu** | `vendor_id` -> `vendors.id` | `service_id` -> `services.id` |
| **Mục đích** | Xác minh pháp lý đối tác (Giấy phép kinh doanh, MST) | Chứng chỉ chuyên môn cho dịch vụ rủi ro cao (Chứng chỉ lặn biển, lướt ván, dù lượn...) |
| **Loại tài liệu (`docType`)** | Có enum `DocType` (`BUSINESS_LICENSE`, `SAFETY_CERT`) | Mặc định là chứng chỉ an toàn của service (không cần `DocType` riêng hoặc cố định loại) |
| **Ảnh hưởng vòng đời** | Ảnh hưởng đến `vendor.verification_status` | Quyết định điều kiện Service được chuyển sang `PUBLISHED` (R3) |

### 4.2. Cấu trúc bảng `service_safety_documents` (PostgreSQL)
```sql
CREATE TABLE IF NOT EXISTS service_safety_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    file_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_service_safety_docs_service_id ON service_safety_documents(service_id);
CREATE INDEX idx_service_safety_docs_status ON service_safety_documents(status);
```

### 4.3. Mở rộng bảng `categories` (Hỗ trợ R3)
Theo yêu cầu R3: Cần thêm cờ `requires_safety_cert` gắn theo danh mục (Category) thay vì hardcode:
```sql
ALTER TABLE categorys ADD COLUMN IF NOT EXISTS requires_safety_cert BOOLEAN NOT NULL DEFAULT false;
```
*(Lưu ý: Tên bảng trong `CategoryJpaEntity` hiện tại là `@Table(name = "categorys")`)*.

### 4.4. Domain Model & JPA Entity cập nhật
1. **`Category.java`**:
   ```java
   public class Category extends BaseDomainModel {
       private String name;
       private String nameEn;
       private String slug;
       private UUID parentId;
       private String iconUrl;
       private Boolean isActive;
       private Boolean requiresSafetyCert; // Bổ sung cho R3
   }
   ```
2. **`CategoryJpaEntity.java`**:
   ```java
   @Column(name = "requires_safety_cert")
   private Boolean requiresSafetyCert = false;
   ```
3. **`ServiceSafetyDocument.java`**:
   ```java
   public class ServiceSafetyDocument extends BaseDomainModel {
       private UUID serviceId;
       private String fileUrl;
       private DocStatus status;
       private UUID reviewedBy;
       private OffsetDateTime reviewedAt;
       private String rejectionReason; // Bổ sung
   }
   ```
4. **`ServiceSafetyDocumentJpaEntity.java`**:
   ```java
   @Column(name = "rejection_reason")
   private String rejectionReason;
   ```
5. **`JpaServiceSafetyDocumentRepository.java`**:
   ```java
   @Repository
   public interface JpaServiceSafetyDocumentRepository extends JpaRepository<ServiceSafetyDocumentJpaEntity, UUID> {
       List<ServiceSafetyDocumentJpaEntity> findAllByServiceIdOrderByCreatedAtDesc(UUID serviceId);
       Optional<ServiceSafetyDocumentJpaEntity> findByIdAndServiceId(UUID id, UUID serviceId);
       boolean existsByServiceIdAndStatus(UUID serviceId, DocStatus status);
   }
   ```

---

## 5. THIẾT KẾ CÁC API CHI TIẾT (R2 & R3)

### 5.1. API 1: Vendor Upload Safety Document
- **Endpoint**: `POST /api/vendor/services/{id}/safety-documents`
- **Quyền truy cập**: `@PreAuthorize("hasRole('VENDOR')")`
- **Content-Type**: `multipart/form-data`
- **Request Parameters / Body**:
  - Path variable `{id}`: UUID của service.
  - Part `file`: `MultipartFile` (chứng chỉ file scan/pdf/ảnh).
- **Validation**:
  - File không được null/rỗng.
  - Kích thước tối đa cho phép (vd: <= 10MB).
  - Content-Type hợp lệ: `application/pdf`, `image/jpeg`, `image/png`, `image/webp`.
  - Service `{id}` phải tồn tại.
  - Vendor phải là chủ sở hữu của Service `{id}`.
- **Quy trình xử lý**:
  1. Xác thực Vendor và kiểm tra quyền sở hữu service.
  2. Upload file qua `FileUploadPort` (lưu trữ Cloudinary, lưu vào thư mục `danasea/safety-documents/{serviceId}`).
  3. Nhận URL trả về từ Cloudinary.
  4. Tạo bản ghi `ServiceSafetyDocument` với trạng thái `PENDING`.
  5. Lưu vào database qua Repository.
- **Response**: `201 Created`
  ```json
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "serviceId": "7b12c820-8025-4c09-a1b7-a0684f8ce30e",
    "fileUrl": "https://res.cloudinary.com/.../safety_doc_1.pdf",
    "status": "PENDING",
    "reviewedBy": null,
    "reviewedAt": null,
    "rejectionReason": null,
    "createdAt": "2026-09-10T10:45:00Z"
  }
  ```

### 5.2. API 2: Admin Lấy Danh Sách Safety Documents Của Service
- **Endpoint**: `GET /api/admin/services/{id}/safety-documents`
- **Quyền truy cập**: `@PreAuthorize("hasRole('ADMIN')")`
- **Path Variable**: `{id}` (UUID của service).
- **Quy trình xử lý**:
  1. Kiểm tra Service `{id}` có tồn tại không. Nếu không -> 404.
  2. Truy vấn tất cả document của `serviceId` từ DB, sắp xếp `createdAt DESC`.
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "serviceId": "7b12c820-8025-4c09-a1b7-a0684f8ce30e",
      "fileUrl": "https://res.cloudinary.com/.../safety_doc_1.pdf",
      "status": "PENDING",
      "reviewedBy": null,
      "reviewedAt": null,
      "rejectionReason": null,
      "createdAt": "2026-09-10T10:45:00Z"
    }
  ]
  ```

### 5.3. API 3: Admin Phê Duyệt Safety Document
- **Endpoint**: `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve`
- **Quyền truy cập**: `@PreAuthorize("hasRole('ADMIN')")`
- **Path Variables**: `{id}` (serviceId), `{docId}` (documentId).
- **Quy trình xử lý**:
  1. Tìm document theo `{docId}` và `{id}`. Nếu không tìm thấy -> 404.
  2. Lấy `adminUserId` từ Security Context.
  3. Cập nhật `status = DocStatus.APPROVED`.
  4. Cập nhật `reviewedBy = adminUserId`, `reviewedAt = OffsetDateTime.now()`, `rejectionReason = null`.
  5. Lưu vào database.
- **Response**: `200 OK`
  ```json
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "serviceId": "7b12c820-8025-4c09-a1b7-a0684f8ce30e",
    "fileUrl": "https://res.cloudinary.com/.../safety_doc_1.pdf",
    "status": "APPROVED",
    "reviewedBy": "a50c822e-13cb-4db4-bb39-38b438258e72",
    "reviewedAt": "2026-09-10T11:00:00Z",
    "rejectionReason": null,
    "createdAt": "2026-09-10T10:45:00Z"
  }
  ```

### 5.4. API 4: Admin Từ Chối Safety Document
- **Endpoint**: `PATCH /api/admin/services/{id}/safety-documents/{docId}/reject`
- **Quyền truy cập**: `@PreAuthorize("hasRole('ADMIN')")`
- **Path Variables**: `{id}` (serviceId), `{docId}` (documentId).
- **Request Body**:
  ```json
  {
    "rejectionReason": "Chứng chỉ đã hết hạn hiệu lực hoặc hình ảnh không rõ nét"
  }
  ```
- **Quy trình xử lý**:
  1. Tìm document theo `{docId}` và `{id}`. Nếu không tìm thấy -> 404.
  2. Lấy `adminUserId` từ Security Context.
  3. Cập nhật `status = DocStatus.REJECTED`.
  4. Cập nhật `reviewedBy = adminUserId`, `reviewedAt = OffsetDateTime.now()`, `rejectionReason = body.rejectionReason`.
  5. Lưu vào database.
- **Response**: `200 OK` (kèm thông tin trạng thái `REJECTED`).

---

## 6. THIẾT KẾ BUSINESS RULES R3: QUY TẮC PUBLISH SERVICE

### 6.1. Quy tắc nghiệm ngặt khi Publish Service
Theo yêu cầu R3:
> "Dịch vụ có `weather_sensitive=true` hoặc liên quan đến hoạt động rủi ro cao nhưng chưa có safety documents nào được `APPROVED` thì không được set thành `PUBLISHED` (ngay cả khi Admin approve service).  
> Thêm cờ `requires_safety_cert` (hoặc cấu trúc tương tự) gắn theo category thay vì hardcode, để xác định dịch vụ nào bắt buộc phải có chứng chỉ an toàn."

### 6.2. Thuật toán kiểm tra điều kiện Publish (Publish Verification Logic)
```java
public boolean canPublishService(UUID serviceId) {
    ServiceJpaEntity service = serviceRepository.findById(serviceId)
        .orElseThrow(() -> new ServiceNotFoundException("Service not found"));
    
    CategoryJpaEntity category = categoryRepository.findById(service.getCategoryId())
        .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

    boolean isHighRisk = Boolean.TRUE.equals(category.getRequiresSafetyCert());
    boolean isWeatherSensitive = Boolean.TRUE.equals(service.getWeatherSensitive());

    // Nếu là dịch vụ rủi ro cao HOẶC nhạy cảm thời tiết
    if (isHighRisk || isWeatherSensitive) {
        boolean hasApprovedSafetyCert = safetyDocRepository
            .existsByServiceIdAndStatus(serviceId, DocStatus.APPROVED);
        
        if (!hasApprovedSafetyCert) {
            throw new SafetyCertificateRequiredException(
                "Service requires an approved safety certificate before it can be published."
            );
        }
    }

    return true;
}
```

### 6.3. Trường hợp kiểm thử nghiệp vụ (Business Rule Cases)
1. **Case A**: Service thuộc Category có `requires_safety_cert = true` (ví dụ: Lặn ngắm san hô bằng bình khí, Dù lượn cano):
   - Đã có ít nhất 1 safety document `APPROVED` -> Cho phép Publish.
   - Chưa upload tài liệu nào -> Chặn Publish, báo lỗi `SAFETY_CERTIFICATE_REQUIRED`.
   - Có tài liệu nhưng đang `PENDING` -> Chặn Publish.
   - Có tài liệu nhưng bị `REJECTED` -> Chặn Publish.
2. **Case B**: Service có `weather_sensitive = true` (dù Category không yêu cầu chứng chỉ):
   - Bắt buộc phải có ít nhất 1 safety document `APPROVED` mới được chuyển sang `PUBLISHED`.
3. **Case C**: Service thông thường (`requires_safety_cert = false` VÀ `weather_sensitive = false`):
   - Được phép chuyển sang `PUBLISHED` bình thường mà không cần kiểm tra safety document.

---

## 7. DANH MỤC CÁC FILE CẦN TẠO MỚI VÀ SỬA ĐỔI CHO R2 & R3

### 7.1. Các file cần sửa đổi (Modifications)
1. `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java`:
   - Thêm trường `private Boolean requiresSafetyCert;`
2. `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java`:
   - Thêm trường `@Column(name = "requires_safety_cert") private Boolean requiresSafetyCert = false;`
3. `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceSafetyDocument.java`:
   - Thêm trường `private String rejectionReason;`
4. `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceSafetyDocumentJpaEntity.java`:
   - Thêm cột `@Column(name = "rejection_reason") private String rejectionReason;`
5. `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceSafetyDocumentRepository.java`:
   - Thêm các method truy vấn:
     - `List<ServiceSafetyDocumentJpaEntity> findAllByServiceIdOrderByCreatedAtDesc(UUID serviceId);`
     - `Optional<ServiceSafetyDocumentJpaEntity> findByIdAndServiceId(UUID id, UUID serviceId);`
     - `boolean existsByServiceIdAndStatus(UUID serviceId, DocStatus status);`
6. `backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorRepository.java`:
   - Thêm `Optional<VendorJpaEntity> findByUserId(UUID userId);`
7. `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceRepository.java`:
   - Thêm `Optional<ServiceJpaEntity> findByIdAndVendorId(UUID id, UUID vendorId);`

### 7.2. Các file cần tạo mới (New Files)
#### Tầng Domain (`modules/service/domain/`):
- `exception/ServiceNotFoundException.java`
- `exception/SafetyDocumentNotFoundException.java`
- `exception/SafetyCertificateRequiredException.java`
- `exception/InvalidFileException.java`

#### Tầng Application (`modules/service/application/`):
- **Ports (`application/port/`)**:
  - `FileUploadPort.java`: Giao diện trừu tượng hóa việc upload file lên kho lưu trữ.
- **DTOs (`application/dto/`)**:
  - `UploadSafetyDocumentCommand.java`: Record chứa `UUID serviceId`, `UUID currentUserId`, `byte[] fileBytes`, `String fileName`, `String contentType`.
  - `ReviewSafetyDocumentCommand.java`: Record chứa `UUID serviceId`, `UUID docId`, `UUID adminUserId`, `DocStatus status`, `String rejectionReason`.
  - `SafetyDocumentResult.java`: Record kết quả nghiệp vụ trả về từ Use Case.
- **Use Cases (`application/usecase/`)**:
  - `UploadSafetyDocumentUseCase.java`: Thực hiện kiểm tra quyền sở hữu service, upload file qua port và tạo safety document với `PENDING`.
  - `GetSafetyDocumentsUseCase.java`: Lấy danh sách tài liệu kiểm duyệt của service.
  - `ApproveSafetyDocumentUseCase.java`: Thực hiện logic phê duyệt document sang `APPROVED`.
  - `RejectSafetyDocumentUseCase.java`: Thực hiện logic từ chối document sang `REJECTED`.
  - `ValidateServicePublishableUseCase.java` (hoặc method trong `PublishServiceUseCase`): Kiểm tra các điều kiện an toàn R3 trước khi publish.

#### Tầng Infrastructure (`modules/service/infrastructure/`):
- **Storage Adapter (`infrastructure/storage/`)**:
  - `CloudinaryFileUploadAdapter.java`: Cài đặt `FileUploadPort` sử dụng Cloudinary SDK.
- **Mapper (`infrastructure/mapper/`)**:
  - `ServiceSafetyDocumentMapper.java`: Ánh xạ qua lại giữa Entity, Domain Model và Result DTO.
- **Configuration Beans (`infrastructure/config/`)**:
  - `ServiceSafetyDocumentBeans.java`: Cấu hình `@Bean` đăng ký các Use Case cho Spring Container theo chuẩn Clean Architecture Rule 7.

#### Tầng Presentation (`modules/service/presentation/`):
- **Controllers (`presentation/controller/`)**:
  - `VendorSafetyDocumentController.java`: Phục vụ endpoint `POST /api/vendor/services/{id}/safety-documents`.
  - `AdminSafetyDocumentController.java`: Phục vụ các endpoint `GET /api/admin/services/{id}/safety-documents`, `PATCH .../approve`, `PATCH .../reject`.
- **DTOs (`presentation/dto/`)**:
  - `SafetyDocumentResponse.java`: Record trả về cho Client.
  - `RejectSafetyDocumentRequest.java`: Record nhận `rejectionReason`.
- **Exception Handler (`presentation/handler/`)**:
  - `ServiceSafetyExceptionHandler.java`: `@RestControllerAdvice` bắt các ngoại lệ nghiệp vụ của service (`ServiceNotFoundException` -> 404, `SafetyCertificateRequiredException` -> 400/422, `InvalidFileException` -> 400).

#### Tầng Kiểm thử (`src/test/java/com/danasea/backend/modules/service/`):
- `application/usecase/UploadSafetyDocumentUseCaseTest.java`
- `application/usecase/ApproveSafetyDocumentUseCaseTest.java`
- `application/usecase/RejectSafetyDocumentUseCaseTest.java`
- `application/usecase/ValidateServicePublishableUseCaseTest.java`
- `presentation/controller/VendorSafetyDocumentControllerTest.java`
- `presentation/controller/AdminSafetyDocumentControllerTest.java`

---

## 8. PHÁT HIỆN KỸ THUẬT & CAVEATS QUAN TRỌNG CHO BƯỚC TRIỂN KHAI (TEST RUNNER)

Trong quá trình khảo sát môi trường build và test bằng Maven trên hệ thống:
1. **JDK Version**: Temurin OpenJDK 21 (`/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
2. **Mockito ByteBuddy Warning trong Sandbox macOS**:
   - Khi chạy `./mvnw test` trong môi trường sandboxed, Mockito mặc định sử dụng `InlineDelegateByteBuddyMockMaker` yêu cầu cơ chế attach tiến trình ngoài (`ByteBuddyAgent.installExternal()`), việc này bị sandbox của macOS chặn dẫn đến lỗi:
     `Could not initialize inline Byte Buddy mock maker. It appears as if your JDK does not supply a working agent attachment mechanism.`
   - **Giải pháp xử lý triệt để cho Implementer**:
     - Tạo file `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` với nội dung `mock-maker-subclass`.
     - Điều này cấu hình Mockito sử dụng subclass-based proxy thay vì inline agent attachment, giúp toàn bộ unit test chạy cực nhanh, mượt mà và 100% độc lập với hệ thống dynamic agent sandbox!

---

## 9. KẾT LUẬN

Module `vendor_documents` hiện tại cung cấp khuôn mẫu chuẩn về Domain Entity (`VendorDocument`), Enum (`DocStatus`), và JPA Entity (`vendor_documents`). Dựa trên khuôn mẫu này và kiến trúc Clean Architecture của dự án, thiết kế cho `service_safety_documents` (R2) cùng các quy tắc ràng buộc phát hành dịch vụ (R3) đã được làm rõ hoàn toàn và sẵn sàng để đội ngũ triển khai (Implementer) thực hiện code và viết unit tests mà không gặp bất kỳ vướng mắc nào.
