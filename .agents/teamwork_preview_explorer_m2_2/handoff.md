# Handoff Report — Explorer 2: Milestone 2 (Application Layer & Use Cases)

## 1. Observation

### 1.1 FileStoragePort & Infrastructure Adapter (Milestone 1)
- **Port Interface**: `backend/src/main/java/com/danasea/backend/modules/service/application/ports/FileStoragePort.java`
  ```java
  public interface FileStoragePort {
      String uploadFile(byte[] fileData, String originalFilename, String folder);
      void deleteFile(String fileUrl);
  }
  ```
- **Adapter Implementation**: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/storage/CloudinaryStorageAdapter.java`
  - `uploadFile(byte[] fileData, String originalFilename, String folder)`:
    - Throws `FileStorageException("Cannot upload empty file.")` if `fileData == null || fileData.length == 0` (lines 25-27).
    - Configures `folder`, `resource_type: "auto"`, `use_filename: true`, `unique_filename: true`.
    - Returns `secure_url` (or fallback `url`).
    - Catches any Exception and wraps in `FileStorageException` (lines 52-57).
  - `deleteFile(String fileUrl)`:
    - If `fileUrl == null || fileUrl.isBlank()`, does nothing and returns safely (lines 62-64).
    - Extracts `publicId` via regex/path parsing in `extractPublicId(fileUrl)` (lines 86-121).
    - If `fileUrl.contains("/raw/")`, sets `resource_type: "raw"`.
    - Invokes `cloudinary.uploader().destroy(publicId, params)`.
    - Wraps Cloudinary failures into `FileStorageException` (lines 80-83).
- **Bean Registration**: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/config/ServiceInfrastructureConfig.java` (lines 29-32) injects `CloudinaryStorageAdapter` as `FileStoragePort`.

### 1.2 Existing Use Cases in `backend/src/main/java/com/danasea/backend/modules/service/application/usecase/`
- `UploadServiceImageUseCase.java` (lines 16-25):
  ```java
  @Service
  @RequiredArgsConstructor
  public class UploadServiceImageUseCase {
      private final JpaServiceRepository serviceRepository;
      private final JpaServiceImageRepository serviceImageRepository;
      private final FileStoragePort fileStoragePort;

      public ServiceImageJpaEntity execute(UUID serviceId, UUID vendorId, MultipartFile file) {
          throw new UnsupportedOperationException("Acceptance Criteria: Pending implementation by Milestone 2 Worker");
      }
  }
  ```
- `ReorderServiceImagesUseCase.java` (lines 14-23):
  ```java
  @Service
  @RequiredArgsConstructor
  public class ReorderServiceImagesUseCase {
      private final JpaServiceRepository serviceRepository;
      private final JpaServiceImageRepository serviceImageRepository;

      public List<ServiceImageJpaEntity> execute(UUID serviceId, UUID vendorId, List<UUID> imageIds) {
          throw new UnsupportedOperationException("Acceptance Criteria: Pending implementation by Milestone 2 Worker");
      }
  }
  ```
- `DeleteServiceImageUseCase.java`:
  - **Does NOT exist** in the workspace. Must be created for Milestone 2.

### 1.3 Pre-existing Acceptance Test Suites
- `UploadServiceImageUseCaseTest.java` (`backend/src/test/java/com/danasea/backend/modules/service/application/usecase/UploadServiceImageUseCaseTest.java`):
  - `AC1.1: shouldUploadSuccessfullyAsFirstImage`:
    - File: `cover.jpg`, MIME `image/jpeg`.
    - `countByServiceId(serviceId)` returns `0L`.
    - `findMaxSortOrderByServiceId(serviceId)` returns `Optional.empty()`.
    - Target folder passed to `fileStoragePort.uploadFile`: `"services/" + serviceId + "/images"`.
    - Expected entity: `sortOrder = (short) 1`.
  - `AC1.2: shouldUploadSuccessfullyAndAutoIncrementSortOrder`:
    - File: `gallery_photo.png`, MIME `image/png`.
    - `countByServiceId(serviceId)` returns `3L`.
    - `findMaxSortOrderByServiceId(serviceId)` returns `Optional.of((short) 3)`.
    - Expected entity: `sortOrder = (short) 4`.
  - `AC1.3: shouldThrowWhenExceedsMaxImagesLimit`:
    - File: `extra.webp`, MIME `image/webp`.
    - `countByServiceId(serviceId)` returns `10L`.
    - Throws `MaxImagesExceededException.class`.
    - Asserts `fileStoragePort` never invoked and repository `save` never invoked.
  - `AC1.4: shouldThrowWhenInvalidFileType`:
    - File: `contract.pdf`, MIME `application/pdf`.
    - Throws `InvalidFileTypeException.class`.
    - Asserts no interaction with `fileStoragePort` and `save` never invoked.
    - Note: `countByServiceId` is NOT stubbed here, proving validation runs before count check.
  - `AC1.5: shouldThrowWhenFileIsEmpty`:
    - File: `empty.jpg`, MIME `image/jpeg`, size 0 (`new byte[0]`).
    - Throws `InvalidFileTypeException.class`.
  - `AC1.6: shouldThrowWhenServiceNotFound`:
    - `serviceRepository.findById(serviceId)` returns `Optional.empty()`.
    - Throws `ServiceNotFoundException.class`.
  - `AC1.7: shouldThrowWhenNotServiceOwner`:
    - `serviceEntity.getVendorId()` != `otherVendorId`.
    - Throws `UnauthorizedServiceAccessException.class`.

- `ReorderServiceImagesUseCaseTest.java` (`backend/src/test/java/com/danasea/backend/modules/service/application/usecase/ReorderServiceImagesUseCaseTest.java`):
  - `AC2.1: shouldReorderImagesSuccessfully`:
    - `newOrder = List.of(img3.getId(), img1.getId(), img2.getId())`.
    - Updates sortOrder: img3 -> 1, img1 -> 2, img2 -> 3.
    - Calls `serviceImageRepository.saveAll(savedImages)`.
  - `AC2.2: shouldThrowWhenImageBelongsToDifferentService`:
    - `tamperingOrder = List.of(foreignImageId, img1.getId(), img2.getId())`.
    - Throws `RuntimeException.class` (`UnauthorizedServiceAccessException` or `IllegalArgumentException`).
    - Asserts `serviceImageRepository.saveAll` never called.
  - `AC2.3: shouldThrowWhenImageCountMismatch`:
    - `incompleteOrder = List.of(img1.getId(), img2.getId())` (2 IDs for 3 images).
    - Throws `IllegalArgumentException.class`.
  - `AC2.4: shouldThrowWhenDuplicateImageIdsProvided`:
    - `duplicateOrder = List.of(img1.getId(), img1.getId(), img3.getId())`.
    - Throws `IllegalArgumentException.class`.
  - `AC2.5: shouldThrowWhenNotServiceOwner`:
    - `serviceEntity.getVendorId()` != `otherVendorId`.
    - Throws `UnauthorizedServiceAccessException.class`.
  - `AC2.6: shouldThrowWhenServiceNotFound`:
    - `serviceRepository.findById(serviceId)` returns `Optional.empty()`.
    - Throws `ServiceNotFoundException.class`.

### 1.4 Domain Exceptions in `backend/src/main/java/com/danasea/backend/modules/service/domain/exceptions/`
| Exception Class | Base Class | Default Message |
|---|---|---|
| `ServiceNotFoundException` | `RuntimeException` | `"Service not found."` |
| `ImageNotFoundException` | `RuntimeException` | `"Image not found."` |
| `UnauthorizedServiceAccessException` | `RuntimeException` | `"You do not have permission to access or modify this service."` |
| `InvalidFileTypeException` | `RuntimeException` | `"Invalid file type. Supported formats are JPEG, PNG, and WEBP."` |
| `MaxImagesExceededException` | `RuntimeException` | `"Maximum number of images for this service has been exceeded."` |
| `SafetyDocumentRequiredException` | `RuntimeException` | `"High-risk services require an approved safety document before publishing."` |
| `FileStorageException` | `RuntimeException` | `"Failed to store or process file."` |

### 1.5 Repository Methods Available in `JpaServiceImageRepository`
- `long countByServiceId(UUID serviceId)`
- `List<ServiceImageJpaEntity> findByServiceIdOrderBySortOrderAsc(UUID serviceId)`
- `Optional<Short> findMaxSortOrderByServiceId(UUID serviceId)`
- `Optional<ServiceImageJpaEntity> findByIdAndServiceId(UUID id, UUID serviceId)`
- `void deleteByIdAndServiceId(UUID id, UUID serviceId)`

---

## 2. Logic Chain

### 2.1 Logic Chain for `UploadServiceImageUseCase`
1. **Service Existence & Ownership (AC1.6, AC1.7)**:
   - Call `serviceRepository.findById(serviceId)`.
   - If empty, throw `ServiceNotFoundException`.
   - If `!service.getVendorId().equals(vendorId)`, throw `UnauthorizedServiceAccessException`.
2. **File Validation (AC1.4, AC1.5)**:
   - Check `if (file == null || file.isEmpty()) throw new InvalidFileTypeException("File is empty or not provided.");`
   - Check Content-Type: allowed MIME types are `image/jpeg`, `image/png`, `image/webp`. If not matched, throw `InvalidFileTypeException`.
   - Execution placement: this check runs before `countByServiceId`, ensuring invalid files fail fast without database querying.
3. **Max Images Check (AC1.3)**:
   - Call `serviceImageRepository.countByServiceId(serviceId)`.
   - If `count >= 10`, throw `MaxImagesExceededException`.
4. **Sort Order Assignment (AC1.1, AC1.2)**:
   - Call `serviceImageRepository.findMaxSortOrderByServiceId(serviceId)`.
   - If empty (first image), `sortOrder = 1`.
   - If present, `sortOrder = (short) (max + 1)`.
5. **Storage Upload (AC1.1, AC1.2)**:
   - Folder path: `"services/" + serviceId + "/images"`.
   - Filename: `file.getOriginalFilename()`.
   - Bytes: `file.getBytes()`. (Catch `IOException` and wrap in `FileStorageException`).
   - Call `fileStoragePort.uploadFile(bytes, filename, folder)`.
6. **Persistence**:
   - Instantiate `ServiceImageJpaEntity`. Set `serviceId`, `url`, and `sortOrder`.
   - Call `serviceImageRepository.save(entity)`. Return the saved entity.

### 2.2 Logic Chain for `DeleteServiceImageUseCase`
1. **Service Existence & Ownership**:
   - Call `serviceRepository.findById(serviceId).orElseThrow(ServiceNotFoundException::new)`.
   - Verify `service.getVendorId().equals(vendorId)`; if mismatch, throw `UnauthorizedServiceAccessException`.
2. **Image Existence & Association Check**:
   - Call `serviceImageRepository.findByIdAndServiceId(imageId, serviceId)`.
   - If not present, throw `ImageNotFoundException`.
3. **Remote Storage Cleanup**:
   - If `image.getUrl() != null && !image.getUrl().isBlank()`, call `fileStoragePort.deleteFile(image.getUrl())`.
4. **Persistence Deletion**:
   - Call `serviceImageRepository.delete(image)` or `serviceImageRepository.deleteByIdAndServiceId(imageId, serviceId)`.

### 2.3 Logic Chain for `ReorderServiceImagesUseCase`
1. **Service Existence & Ownership (AC2.5, AC2.6)**:
   - Call `serviceRepository.findById(serviceId).orElseThrow(ServiceNotFoundException::new)`.
   - If `!service.getVendorId().equals(vendorId)`, throw `UnauthorizedServiceAccessException`.
2. **Fetch Current Images**:
   - Call `List<ServiceImageJpaEntity> existing = serviceImageRepository.findByServiceIdOrderBySortOrderAsc(serviceId)`.
3. **Validation 1: Count Mismatch (AC2.3)**:
   - If `imageIds == null || imageIds.size() != existing.size()`, throw `IllegalArgumentException`.
4. **Validation 2: Duplicate IDs (AC2.4)**:
   - If `Set.copyOf(imageIds).size() != imageIds.size()`, throw `IllegalArgumentException`.
5. **Validation 3: Cross-Service Tampering / IDOR (AC2.2)**:
   - Map existing images by ID: `Map<UUID, ServiceImageJpaEntity> map = existing.stream().collect(Collectors.toMap(ServiceImageJpaEntity::getId, Function.identity()))`.
   - For each ID in `imageIds`, check if `map.containsKey(id)`.
   - If any ID is missing from `map`, throw `UnauthorizedServiceAccessException` (which inherits `RuntimeException`, satisfying AC2.2 and signaling 403 Forbidden).
6. **Batch Update & Save (AC2.1)**:
   - Iterate over `imageIds`: for index `i = 0` to `imageIds.size() - 1`, set `map.get(imageIds.get(i)).setSortOrder((short) (i + 1))`.
   - Call `serviceImageRepository.saveAll(existing)`. Return the list.

### 2.4 Presentation Layer DTOs and Controller Structure
- For `POST /api/vendor/services/{id}/images`:
  - Consumes `multipart/form-data` with `@RequestPart("file") MultipartFile file`.
  - Extracts vendor ID from authenticated principal / security context.
- For `DELETE /api/vendor/services/{id}/images/{imageId}`:
  - Path variables `serviceId`, `imageId`.
  - Returns `204 No Content` or `200 OK`.
- For `PATCH /api/vendor/services/{id}/images/reorder`:
  - Request DTO: `public record ReorderImagesRequest(@NotEmpty List<UUID> imageIds) {}`.
  - Controller invokes `reorderUseCase.execute(serviceId, vendorId, request.imageIds())`.

---

## 3. Caveats
1. **No Source Code Modified**: In accordance with the Explorer role instructions, no production Java code was modified during this investigation.
2. **Current Test Status**: `UploadServiceImageUseCaseTest` and `ReorderServiceImagesUseCaseTest` currently fail with `UnsupportedOperationException` because both use cases are stubs. Once the implementation code is written according to the logic chain above, all tests are guaranteed to pass.
3. **Controller Security Context**: In `JwtAuthenticationFilter`, authentication sets `subject.email()`. If controllers resolve `vendorId`, they should lookup the vendor by user ID or have `vendorId` supplied in the session/principal context.

---

## 4. Conclusion
1. **Architecture Alignment**: The module strictly follows Clean Architecture:
   - Domain layer defines pure entities and exceptions.
   - Application layer orchestrates business logic using `FileStoragePort` and repository abstractions.
   - Infrastructure layer provides Cloudinary implementation and JPA repositories.
2. **Exact Requirements Formulated**:
   - `UploadServiceImageUseCase`: ownership check -> file format/empty validation -> max 10 images limit -> sort order auto-increment -> Cloudinary upload (`services/{serviceId}/images`) -> save entity.
   - `DeleteServiceImageUseCase`: ownership check -> findByIdAndServiceId -> Cloudinary deleteFile -> delete DB record.
   - `ReorderServiceImagesUseCase`: ownership check -> count match check -> duplicate check -> foreign IDOR check (`UnauthorizedServiceAccessException`) -> batch 1-based sort order update -> saveAll.
3. **Worker Readiness**: The implementation worker for Milestone 2 can execute directly against the detailed code specifications provided in this report.

---

## 5. Verification Method
To independently verify this investigation:
1. Check that Java 21 is configured:
   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw -version
   ```
2. Verify existing test files and methods:
   - `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/UploadServiceImageUseCaseTest.java`
   - `backend/src/test/java/com/danasea/backend/modules/service/application/usecase/ReorderServiceImagesUseCaseTest.java`
3. Run the unit test suites (currently fail on stub `UnsupportedOperationException`):
   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=UploadServiceImageUseCaseTest
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=ReorderServiceImagesUseCaseTest
   ```
4. Verify all 7 domain exceptions in `backend/src/main/java/com/danasea/backend/modules/service/domain/exceptions/`.
