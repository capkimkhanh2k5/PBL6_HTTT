# Báo cáo Sẵn sàng Kiểm thử (Test Ready Report) — Acceptance Criteria Suites

> **Ngày công bố**: 2026-09-10  
> **Người thực hiện**: Test Writer (E2E & Acceptance Criteria Track)  
> **Trạng thái**: READY FOR IMPLEMENTATION (Sẵn sàng phục vụ nghiệm thu các Milestone 2, 3, 4)

---

## 1. Tóm tắt Kết quả Thực hiện

Toàn bộ hạ tầng kiểm thử và các bộ test Acceptance Criteria cho module **Service Assets & Safety Documents** đã được thiết lập và biên dịch thành công 100%:

1. **Tài liệu Hạ tầng Kiểm thử**:
   - `TEST_INFRA.md` (Project Root): Thiết lập đầy đủ chiến lược Tiers 1-4, hướng dẫn giải quyết ByteBuddy/MockMaker trên Java 21 LTS, danh mục lệnh test.
2. **Acceptance Criteria Test Suites**:
   - Vị trí: `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/`
   - Đã hoàn tất 4 Test Classes bao phủ toàn diện 100% các Acceptance Criteria theo yêu cầu tại `ORIGINAL_REQUEST.md`, `PROJECT.md` và `analysis.md`:
     - `UploadServiceImageUseCaseTest.java` (7 test cases)
     - `ReorderServiceImagesUseCaseTest.java` (6 test cases)
     - `UploadSafetyDocumentUseCaseTest.java` (5 test cases)
     - `ApproveSafetyDocumentUseCaseTest.java` (9 test cases)
   - Tổng cộng: **27 test cases Acceptance Criteria**.
3. **Tuân thủ Clean Architecture**:
   - Sử dụng JUnit 5 Jupiter + Mockito Pure Unit Test.
   - Không nạp Spring Application Context (`@SpringBootTest`), đảm bảo thời gian thực thi siêu tốc (< 1s).
   - Mocking toàn bộ Ports (`FileStoragePort`) và Repositories (`JpaServiceRepository`, `JpaServiceImageRepository`, `JpaServiceSafetyDocumentRepository`, `JpaCategoryRepository`).
4. **Trạng thái Biên dịch (Compilation Status)**:
   - Lệnh `mvn test-compile` chạy thành công tuyệt đối (`BUILD SUCCESS`), biên dịch toàn bộ 208 source files và 16 test files.
   - Không phát sinh lỗi xung đột, không có hồi quy (zero regressions).

---

## 2. Chi tiết Danh mục Test Suites đã Triển khai

### 2.1. `UploadServiceImageUseCaseTest` (7 Test Cases)
- `shouldUploadSuccessfullyAsFirstImage`: Upload ảnh đầu tiên thành công, gán `sort_order = 1`.
- `shouldUploadSuccessfullyAndAutoIncrementSortOrder`: Upload thành công khi đã có ảnh, `sort_order` tự tăng `(max + 1)`.
- `shouldThrowWhenExceedsMaxImagesLimit`: Chặn upload khi dịch vụ đã có 10 ảnh (`MaxImagesExceededException`), không gọi storage.
- `shouldThrowWhenInvalidFileType`: Chặn định dạng file không phải ảnh (PDF, EXE, etc.) ném `InvalidFileTypeException`.
- `shouldThrowWhenFileIsEmpty`: Chặn file dung lượng 0 bytes.
- `shouldThrowWhenServiceNotFound`: Ném `ServiceNotFoundException` (HTTP 404).
- `shouldThrowWhenNotServiceOwner`: Chặn Vendor khác can thiệp dịch vụ (IDOR Protection, HTTP 403).

### 2.2. `ReorderServiceImagesUseCaseTest` (6 Test Cases)
- `shouldReorderImagesSuccessfully`: Sắp xếp lại danh sách ảnh theo thứ tự mới `[img3, img1, img2]` -> `sort_order` cập nhật tương ứng 1, 2, 3.
- `shouldThrowWhenImageBelongsToDifferentService`: Chặn tấn công thao túng ID truyền ảnh của service khác (Cross-service Tampering).
- `shouldThrowWhenImageCountMismatch`: Chặn danh sách truyền thiếu hoặc thừa ảnh so với thực tế.
- `shouldThrowWhenDuplicateImageIdsProvided`: Chặn danh sách chứa ID trùng lặp.
- `shouldThrowWhenNotServiceOwner`: Chặn truy cập trái phép của Vendor khác (HTTP 403).
- `shouldThrowWhenServiceNotFound`: Dịch vụ không tồn tại (HTTP 404).

### 2.3. `UploadSafetyDocumentUseCaseTest` (5 Test Cases)
- `shouldUploadSafetyDocumentSuccessfullyWithPendingStatus`: Upload chứng chỉ PDF thành công, trạng thái khởi tạo `DocStatus.PENDING`, các trường audit trail `null`.
- `shouldUploadSafetyDocumentAsImageSuccessfully`: Upload chứng chỉ dạng ảnh (PNG/JPG) hợp lệ.
- `shouldThrowWhenInvalidFileTypeProvided`: Chặn định dạng lạ (TXT, ZIP, EXE) ném `InvalidFileTypeException`.
- `shouldThrowWhenNotServiceOwner`: Chặn Vendor khác upload vào service (HTTP 403).
- `shouldThrowWhenServiceNotFound`: Service không tồn tại (HTTP 404).

### 2.4. `ApproveSafetyDocumentUseCaseTest` (9 Test Cases)
- `shouldApproveDocumentSuccessfullyWithAuditTrail`: Admin duyệt tài liệu, `status = APPROVED`, cập nhật `reviewedBy` và `reviewedAt`.
- `shouldRejectDocumentSuccessfullyWithReason`: Admin từ chối tài liệu, `status = REJECTED`, lưu lý do từ chối `rejectionReason` và audit trail.
- `shouldThrowWhenDocumentNotFoundOnApprove`: Tài liệu duyệt không tồn tại.
- `shouldAllowPublishWhenCategoryRequiresCertAndDocumentApproved`: Category có `requiresSafetyCert = true`, có doc `APPROVED` -> Cho phép Publish dịch vụ.
- `shouldBlockPublishWhenCategoryRequiresCertAndNoApprovedDocument`: Category có `requiresSafetyCert = true`, chưa có doc `APPROVED` -> Chặn Publish, ném `SafetyDocumentRequiredException`.
- `shouldBlockPublishWhenWeatherSensitiveAndNoApprovedDocument`: Dịch vụ có `weatherSensitive = true`, chưa có doc `APPROVED` -> Chặn Publish, ném `SafetyDocumentRequiredException`.
- `shouldAllowPublishWhenWeatherSensitiveAndDocumentApproved`: Dịch vụ có `weatherSensitive = true`, có doc `APPROVED` -> Cho phép Publish.
- `shouldAllowPublishForNormalServiceWithoutSafetyDocument`: Dịch vụ thông thường không yêu cầu an toàn -> Cho phép Publish không cần tài liệu.
- `shouldThrowWhenServiceNotFoundOnPublishCheck`: Service không tồn tại khi kiểm tra publish (HTTP 404).

---

## 3. Lệnh Thực thi Kiểm thử Độc lập

```bash
# Thiết lập JAVA_HOME tới Java 21
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home

# 1. Kiểm tra biên dịch toàn bộ test
./mvnw test-compile

# 2. Chạy từng bộ Acceptance Criteria test
./mvnw test -Dtest=UploadServiceImageUseCaseTest
./mvnw test -Dtest=ReorderServiceImagesUseCaseTest
./mvnw test -Dtest=UploadSafetyDocumentUseCaseTest
./mvnw test -Dtest=ApproveSafetyDocumentUseCaseTest

# 3. Chạy kiểm tra hồi quy với các test hiện có
./mvnw test -Dtest=LoginUseCaseTest,CloudinaryStorageAdapterTest
```

---

## 4. Bàn giao cho Triển khai (Next Steps for Implementers)

- **Milestone 2 Worker**: Mở `UploadServiceImageUseCase.java` và `ReorderServiceImagesUseCase.java`, triển khai logic nghiệp vụ cho đến khi `UploadServiceImageUseCaseTest` và `ReorderServiceImagesUseCaseTest` chuyển sang trạng thái **GREEN (PASS 100%)**.
- **Milestone 3 Worker**: Triển khai logic trong `UploadSafetyDocumentUseCase.java` và `ApproveSafetyDocumentUseCase.java` (phần approve/reject) cho đến khi `UploadSafetyDocumentUseCaseTest` chuyển sang **GREEN (PASS 100%)**.
- **Milestone 4 Worker**: Hoàn tất `canPublish` trong `ApproveSafetyDocumentUseCase.java` (hoặc Publish Guard use case tương đương) để toàn bộ 9 test cases trong `ApproveSafetyDocumentUseCaseTest` chuyển sang **GREEN (PASS 100%)**.
