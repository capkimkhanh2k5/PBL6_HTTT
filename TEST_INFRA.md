# Hạ tầng Kiểm thử (Test Infrastructure) — Service Assets & Safety Documents Module

> **Dự án**: PBL6_HTTT (Danasea Backend)  
> **Phiên bản Runtime**: Java 21 LTS (Temurin-21) / Spring Boot 4.1.1  
> **Kiến trúc**: Modular Clean Architecture  
> **Mục tiêu**: Thiết lập chiến lược kiểm thử đa tầng (Tiers 1 - 4) và các tiêu chuẩn Acceptance Criteria tự động cho module Quản lý Ảnh Dịch vụ (Service Images) và Tài liệu An toàn (Safety Documents).

---

## 1. Nguyên tắc Cốt lõi (Testing Principles)

Kiến trúc kiểm thử của dự án tuân thủ mô hình **Testing Pyramid** và quy chuẩn **Clean Architecture**:
1. **Cô lập Tầng (Layer Isolation)**:
   - Tầng Application (Use Cases) được kiểm thử thông qua **Pure Unit Test** bằng JUnit 5 và Mockito.
   - Không nạp Spring Application Context (`@SpringBootTest`) cho Use Case Unit Tests để tối ưu hóa thời gian chạy (< 1 giây cho toàn bộ usecase suite).
   - Tầng Infrastructure / Repository sử dụng JPA slice tests (`@DataJpaTest`) hoặc Testcontainers cho integration checks.
   - Tầng Presentation sử dụng MockMvc ở chế độ Standalone (`MockMvcBuilders.standaloneSetup`) nhằm xác minh routing, DTO validation và HTTP status code.
2. **Nguyên tắc AAA (Arrange - Act - Assert)**:
   - Mọi test case đều phân tách rõ ràng 3 giai đoạn: chuẩn bị dữ liệu & mock, thực thi hành vi nghiệp vụ, kiểm tra kết quả & xác thực tương tác (verifications).
3. **Độc lập và Tự chủ (Independence & Idempotency)**:
   - Mỗi phương thức kiểm thử khởi tạo trạng thái độc lập trong `@BeforeEach`, không chia sẻ state giữa các test case, không phụ thuộc vào thứ tự thực thi.
4. **Kiểm thử Hướng Hành vi & Trường hợp Biên (Behavior-Driven & Edge Cases)**:
   - Tập trung vào behavior và business invariants thay vì kiểm thử chi tiết triển khai nội bộ.
   - Bắt buộc kiểm thử đầy đủ các điều kiện biên: vượt quá dung lượng/số lượng (max images = 10), sai định dạng file (invalid MIME type), vi phạm quyền sở hữu (IDOR / multi-tenancy vendor), vi phạm điều kiện xuất bản dịch vụ rủi ro cao (Publish Guard).

---

## 2. Phân tầng Kiểm thử (Testing Tiers Matrix)

| Phân tầng | Phạm vi kiểm thử | Công nghệ & Công cụ | Tốc độ | Mục tiêu chất lượng |
|---|---|---|---|---|
| **Tier 1: Unit Tests** | Tầng Domain Invariants & Application UseCases (`UploadServiceImage`, `ReorderServiceImages`, `UploadSafetyDocument`, `ApproveSafetyDocument`, `Publish Guard`) | JUnit 5 Jupiter, Mockito (`@Mock`, `mock()`) | **Siêu tốc (< 1s)** | Kiểm thử 100% logic nghiệp vụ, phân quyền, validation rules và xử lý ngoại lệ. |
| **Tier 2: Component / Integration Tests** | Tầng Persistence, Spring Data JPA Repositories (`JpaServiceImageRepository`, `JpaServiceSafetyDocumentRepository`, `JpaCategoryRepository`) | Spring Data JPA, `@DataJpaTest`, In-Memory / Test Database | **Nhanh (1 - 3s)** | Kiểm tra câu truy vấn custom SQL/JPQL (`existsByServiceIdAndStatus`, `findMaxSortOrderByServiceId`, `findByServiceIdOrderBySortOrderAsc`), ràng buộc khóa ngoại, cascades. |
| **Tier 3: Presentation / API Tests** | Tầng Presentation REST Controllers (`VendorServiceImageController`, `VendorSafetyDocumentController`, `AdminSafetyDocumentController`) | Spring Test MockMvc Standalone, Jackson ObjectMapper | **Nhanh (< 2s)** | Kiểm tra mapping URL, multipart request parsing, HTTP Status Codes (200, 201, 400, 403, 404, 409), cấu trúc chuẩn `ErrorResponse(code, message)`. |
| **Tier 4: E2E Business Flow Tests** | Toàn bộ quy trình nghiệp vụ xuyên suốt: Upload ảnh -> Đổi thứ tự -> Upload Safety Doc -> Admin Duyệt -> Kiểm tra điều kiện Publish Service | JUnit 5, Integration Flow Runners, Simulated Subsystems | **Trung bình (3 - 5s)** | Đảm bảo tính toàn vẹn vòng đời dịch vụ từ DRAFT -> PENDING_REVIEW -> APPROVED -> PUBLISHED. |

---

## 3. Cấu hình Môi trường & ByteBuddy Fix trên Java 21

### 3.1. Java 21 ByteBuddy Sandbox Issue & Giải pháp
Trên môi trường Java 21, Mockito mặc định sử dụng cơ chế `inline-mock-maker`. Khi chạy bên trong sandbox hoặc môi trường hạn chế quyền `self-attach`, JVM có thể phát sinh cảnh báo hoặc ngoại lệ:
```
IllegalStateException: Could not self-attach to current VM using external process
```

**Giải pháp kỹ thuật đã áp dụng**:
1. Đặt file cấu hình MockMaker tại:
   `backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
   Nội dung:
   ```properties
   mock-maker-subclass
   ```
   Cấu hình này chỉ định Mockito sử dụng cơ chế Subclass Mock Maker truyền thống, hoàn toàn tương thích và ổn định tuyệt đối trên Java 21 mà không cần JVM self-attach agent.
2. Chạy test runner với cấu hình biến môi trường `JAVA_HOME` chuẩn xác trỏ về JDK 21:
   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test
   ```

---

## 4. Hướng dẫn Thực thi Kiểm thử (Execution Commands)

### 4.1. Chạy Toàn bộ Bộ Kiểm thử Unit & AC
```bash
# Chạy tất cả test usecase trong module service
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest="com.danasea.backend.modules.service.application.usecase.*Test"
```

### 4.2. Chạy Từng Test Suite Cụ thể (Acceptance Criteria Suites)
```bash
# 1. AC1: Upload Service Image
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=UploadServiceImageUseCaseTest

# 2. AC2: Reorder Service Images
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=ReorderServiceImagesUseCaseTest

# 3. AC3: Upload Safety Document
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=UploadSafetyDocumentUseCaseTest

# 4. AC4: Approve Safety Document & Publish Guard
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=ApproveSafetyDocumentUseCaseTest
```

### 4.3. Chạy Kiểm tra Biên dịch Test (Test Compilation Verification)
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test-compile
```

---

## 5. Đặc tả Ma trận Kiểm thử Acceptance Criteria (Test Specifications)

### 5.1. `UploadServiceImageUseCaseTest` (Quản lý Upload Ảnh Dịch vụ)
- **Hợp đồng Nghiệp vụ**:
  - `POST /api/vendor/services/{id}/images`
  - Giới hạn tối đa: 10 ảnh / dịch vụ (`MAX_SERVICE_IMAGES = 10`).
  - Định dạng hợp lệ: JPEG, PNG, WEBP.
  - Tự động gán `sort_order`: nếu chưa có ảnh -> 1; nếu đã có ảnh -> `max(sort_order) + 1`.
  - Phân quyền: Chỉ vendor sở hữu dịch vụ (`service.vendorId == request.vendorId`) mới có quyền tải ảnh.
- **Kịch bản Kiểm thử**:
  1. `shouldUploadSuccessfullyAndAutoIncrementSortOrder`: Upload thành công, `sort_order` tự tăng từ 2 lên 3.
  2. `shouldSetSortOrderToOneWhenFirstImage`: Upload ảnh đầu tiên, `sort_order` gán bằng 1.
  3. `shouldThrowWhenExceedingMaxImagesLimit`: Đã có 10 ảnh, ném `MaxServiceImagesExceededException` (HTTP 400).
  4. `shouldThrowWhenInvalidFileFormat`: File PDF hoặc EXE, ném `InvalidFileFormatException` (HTTP 400).
  5. `shouldThrowWhenFileIsEmpty`: File rỗng, ném `InvalidFileFormatException` hoặc `IllegalArgumentException`.
  6. `shouldThrowWhenServiceNotFound`: Không tìm thấy service, ném `ServiceNotFoundException` (HTTP 404).
  7. `shouldThrowWhenNotServiceOwner`: Vendor B gọi vào service của Vendor A, ném `UnauthorizedServiceAccessException` (HTTP 403).

### 5.2. `ReorderServiceImagesUseCaseTest` (Sắp xếp Thứ tự Ảnh Hàng loạt)
- **Hợp đồng Nghiệp vụ**:
  - `PATCH /api/vendor/services/{id}/images/reorder`
  - Nhận danh sách các `imageId` theo thứ tự mới mong muốn.
  - Cập nhật trường `sort_order` của các ảnh tương ứng theo thứ tự mảng (1, 2, 3...).
  - Phòng chống tấn công thao túng ID (IDOR / Cross-service Tampering): Nếu danh sách chứa `imageId` thuộc về service khác hoặc không thuộc sở hữu của service này, hệ thống phải chặn lập tức.
- **Kịch bản Kiểm thử**:
  1. `shouldReorderSuccessfullyWhenValidOrderProvided`: Đổi thứ tự [imgC, imgA, imgB] thành công, cập nhật `sort_order` lần lượt 1, 2, 3.
  2. `shouldThrowWhenImageBelongsToAnotherService`: Danh sách chứa imageId của service khác, ném `UnauthorizedServiceAccessException` (HTTP 403/404).
  3. `shouldThrowWhenImageListIncomplete`: Danh sách truyền thiếu ảnh hoặc dư ảnh so với số ảnh thực tế của service, ném `InvalidImageReorderException` (HTTP 400).
  4. `shouldThrowWhenDuplicateImageIdsProvided`: Danh sách có ID bị trùng lặp, ném `InvalidImageReorderException` (HTTP 400).
  5. `shouldThrowWhenNotServiceOwner`: Vendor khác thao túng dịch vụ, ném `UnauthorizedServiceAccessException` (HTTP 403).
  6. `shouldThrowWhenServiceNotFound`: Dịch vụ không tồn tại, ném `ServiceNotFoundException` (HTTP 404).

### 5.3. `UploadSafetyDocumentUseCaseTest` (Upload Chứng chỉ An toàn)
- **Hợp đồng Nghiệp vụ**:
  - `POST /api/vendor/services/{id}/safety-documents`
  - Upload tài liệu an toàn (PDF, JPG, PNG) lên lưu trữ Cloudinary.
  - Trạng thái khởi tạo bắt buộc: `DocStatus.PENDING`.
  - Các trường audit: `reviewedBy = null`, `reviewedAt = null`, `rejectionReason = null`.
- **Kịch bản Kiểm thử**:
  1. `shouldUploadSafetyDocumentSuccessfullyWithPendingStatus`: Upload thành công, kiểm tra `status == DocStatus.PENDING`, `reviewedBy == null`.
  2. `shouldThrowWhenInvalidFileExtension`: File txt/doc không được hỗ trợ, ném `InvalidFileFormatException` (HTTP 400).
  3. `shouldThrowWhenNotServiceOwner`: Vendor khác upload vào service, ném `UnauthorizedServiceAccessException` (HTTP 403).
  4. `shouldThrowWhenServiceNotFound`: Service ID không tồn tại, ném `ServiceNotFoundException` (HTTP 404).

### 5.4. `ApproveSafetyDocumentUseCaseTest` (Phê duyệt Tài liệu & Publish Guard)
- **Hợp đồng Nghiệp vụ**:
  - `PATCH /api/admin/services/{id}/safety-documents/{docId}/approve`
  - `PATCH /api/admin/services/{id}/safety-documents/{docId}/reject`
  - Ghi nhận Audit Trail: `reviewedBy = adminId`, `reviewedAt = OffsetDateTime.now()`.
  - **Publish Guard (R3)**:
    - Nếu `service.weatherSensitive == true` HOẶC `category.requiresSafetyCert == true`:
      Bắt buộc phải có ít nhất một tài liệu an toàn ở trạng thái `DocStatus.APPROVED`. Nếu không thỏa mãn, ném `SafetyDocumentRequiredException` (HTTP 400/409) và chặn chuyển trạng thái sang `PUBLISHED`.
    - Nếu dịch vụ bình thường (`weatherSensitive == false` VÀ `category.requiresSafetyCert == false`): Cho phép publish mà không cần tài liệu an toàn.
- **Kịch bản Kiểm thử**:
  1. `shouldApproveSafetyDocumentSuccessfullyWithAudit`: Admin duyệt thành công, `status = APPROVED`, `reviewedBy` và `reviewedAt` được cập nhật chính xác.
  2. `shouldRejectSafetyDocumentSuccessfullyWithReason`: Admin từ chối kèm lý do, `status = REJECTED`, `rejectionReason` được lưu vết.
  3. `shouldAllowPublishWhenCategoryRequiresCertAndDocumentApproved`: Category có `requiresSafetyCert = true`, có doc `APPROVED` -> Cho phép publish thành công sang `PUBLISHED`.
  4. `shouldBlockPublishWhenCategoryRequiresCertAndNoApprovedDocument`: Category có `requiresSafetyCert = true`, chỉ có doc `PENDING`/`REJECTED` -> Ném `SafetyDocumentRequiredException`.
  5. `shouldBlockPublishWhenWeatherSensitiveAndNoApprovedDocument`: Dịch vụ có `weatherSensitive = true`, chưa có doc `APPROVED` -> Ném `SafetyDocumentRequiredException`.
  6. `shouldAllowPublishForNormalServiceWithoutSafetyDocument`: Dịch vụ thông thường không yêu cầu chứng chỉ -> Publish thành công.
  7. `shouldThrowWhenDocumentNotFound`: Duyệt tài liệu không tồn tại -> Ném `ResourceNotFoundException` (HTTP 404).

---

## 6. Tiêu chí Sẵn sàng Kiểm thử (Test Readiness Checklist)
- [x] Hạ tầng MockMaker trên Java 21 đã sẵn sàng (`backend/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`).
- [x] Bộ tài liệu hạ tầng kiểm thử `TEST_INFRA.md` đã hoàn tất.
- [x] Các test suite Acceptance Criteria bao phủ toàn bộ yêu cầu R1, R2, R3.
- [x] Không tải Spring Context trong Unit Test để duy trì tốc độ thực thi tối ưu.
- [x] Báo cáo sẵn sàng `TEST_READY.md` được công bố sau khi hoàn tất thiết kế và viết code test.
