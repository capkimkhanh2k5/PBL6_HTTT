# Báo Cáo Khai Thác Đặc Tả & Kiến Trúc Kiểm Thử (Spec Miner Survey Report)

## Features Discovered
| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | API / Profile | POST /api/vendor/profile | CUSTOMER đăng ký thành Vendor, tạo Vendor profile mới với trạng thái PENDING, cập nhật role của User sang VENDOR | RegisterVendorProfileRequest (businessName, taxCode, address, bankAccountNumber, bankName, bankAccountHolder) + Principal | VendorProfileResponse (HTTP 201 Created) | 400 (Invalid input), 403 (User locked), 409 (Vendor already exists), 401 (Unauthorized) | ORIGINAL_REQUEST.md, Clean_Architecture_Rules.md |
| 2 | API / Profile | GET /api/vendor/profile | Lấy thông tin profile Vendor của người dùng hiện tại dựa trên SecurityContext | Principal (lấy userId từ authenticated user) | VendorProfileResponse (HTTP 200 OK) | 401 (Unauthorized), 403 (ADMIN truy cập), 404 (CUSTOMER chưa có profile vendor) | ORIGINAL_REQUEST.md, SecurityConfig.java |
| 3 | API / Profile | PATCH /api/vendor/profile | Cập nhật thông tin profile của Vendor; ngăn chặn triệt để mass assignment các trường nhạy cảm | UpdateVendorProfileRequest (businessName, taxCode, address, bankAccountNumber, bankName, bankAccountHolder) + Principal | VendorProfileResponse (HTTP 200 OK) | 400 (Validation error), 401 (Unauthorized), 403 (ADMIN), 404 (Không tìm thấy vendor theo userId) | ORIGINAL_REQUEST.md |
| 4 | API / Document | POST /api/vendor/documents | Tải tài liệu xác thực (BUSINESS_LICENSE, SAFETY_CERT) lên Cloudinary, lưu bản ghi vendor_documents trạng thái PENDING | MultipartFile file, String doc_type (DocType enum) + Principal | VendorDocumentResponse (HTTP 201 Created) | 400 (Invalid doc_type hoặc file trống), 401 (Unauthorized), 403 (ADMIN), 404/409 (Chưa đăng ký vendor) | ORIGINAL_REQUEST.md, DocType.java, DocStatus.java |
| 5 | API / Document | GET /api/vendor/documents | Vendor xem danh sách các tài liệu đã tải lên cùng trạng thái thẩm định | Principal | List<VendorDocumentResponse> (HTTP 200 OK) | 401 (Unauthorized), 403 (ADMIN), 404 (Vendor không tồn tại) | ORIGINAL_REQUEST.md, JpaVendorDocumentRepository.java |
| 6 | Testing / UseCase | RegisterVendorProfileUseCaseTest | Kiểm thử đăng ký Vendor thành công (status PENDING, role update), xử lý trùng lặp, chặn tài khoản bị khóa | Mock VendorRepository, AccountInternalApi, input command | Void / Vendor profile result | Ném VendorAlreadyExistsException (409), UserLockedException (403) | RegisterUseCaseTest.java, ORIGINAL_REQUEST.md |
| 7 | Testing / UseCase | UpdateVendorProfileUseCaseTest | Kiểm thử cập nhật thông tin hợp lệ, ngăn chặn mass assignment, chống thao túng profile vendor khác | Mock VendorRepository, input command, userId | Vendor profile result | Ném VendorNotFoundException khi userId không khớp profile | ORIGINAL_REQUEST.md |
| 8 | Testing / UseCase | UploadVendorDocumentUseCaseTest | Kiểm thử upload tài liệu thành công (status PENDING, reviewed_by/at null), enum doc_type không hợp lệ, upload khi chưa đăng ký vendor | Mock VendorRepository, VendorDocumentRepository, CloudinaryService | VendorDocument result | Ném InvalidDocTypeException (400), VendorNotFoundException (404/409) | ORIGINAL_REQUEST.md |
| 9 | Testing / Controller | VendorProfileControllerTest | Kiểm thử MockMvc cho Controller: CUSTOMER chưa có profile nhận 404, ADMIN truy cập route VENDOR nhận 403 | MockMvc, @WithMockUser (CUSTOMER, ADMIN, VENDOR), mock use cases | HTTP status, JSON payload | 404 NOT FOUND, 403 FORBIDDEN | AuthenticationControllerTest.java, SecurityConfig.java, ORIGINAL_REQUEST.md |
| 10 | Security / Auth | Role-based Authorization | Phân quyền truy cập theo Role (VENDOR, CUSTOMER, ADMIN) sử dụng Spring Security Method Security (@PreAuthorize) | Authentication subject authorities (ROLE_VENDOR, ROLE_CUSTOMER, ROLE_ADMIN) | Access granted / Access denied (403) | Ném AccessDeniedException -> 403 JSON {"code":"ACCESS_DENIED","message":"Access denied"} | SecurityConfig.java, AuthorizationController.java, AuthorizationAdapter.java |
| 11 | Exception Handling | VendorExceptionHandler | Bắt và chuẩn hóa toàn bộ ngoại lệ của module Vendor thành ErrorResponse(code, message) | Custom exceptions (VendorAlreadyExists, VendorNotFound, UserLocked, InvalidDocType, MethodArgumentNotValid) | ResponseEntity<ErrorResponse> | HTTP status tương ứng: 400, 403, 404, 409 | AuthenticationExceptionHandler.java, ErrorResponse.java |

## Edge Cases
| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | RegisterVendorProfileUseCase | User có `isLocked = true` gọi đăng ký vendor | Chặn đăng ký, ném ngoại lệ tài khoản bị khóa (`UserLockedException`), không gọi lưu vendor |
| 2 | RegisterVendorProfileUseCase | User đã có bản ghi Vendor trong database gọi đăng ký lần 2 | Ném `VendorAlreadyExistsException`, trả về mã lỗi `VENDOR_ALREADY_EXISTS` (HTTP 409 Conflict) |
| 3 | RegisterVendorProfileUseCase | Đăng ký thành công | Trạng thái bắt buộc phải là `VerificationStatus.PENDING`, `badgeTier = BadgeTier.NONE`, `ratingAvg = 0`, User được cập nhật role từ `CUSTOMER` sang `VENDOR` qua `AccountInternalApi.saveUser()` |
| 4 | UpdateVendorProfileUseCase | Request chứa `verification_status: "APPROVED"` hoặc `rating_avg: 5.0` | Structurally ignored / excluded via DTO. Trạng thái và rating cũ được giữ nguyên tuyệt đối, không bị ghi đè |
| 5 | UpdateVendorProfileUseCase | Attacker cố tình truyền path param / body vendorId của vendor khác | Controller không nhận vendorId từ client; UseCase chỉ truy vấn theo `userId` lấy từ `SecurityContext`. Nếu `userId` không sở hữu vendor, trả về 404/403, ngăn chặn hoàn toàn cross-vendor ID manipulation |
| 6 | UpdateVendorProfileUseCase | Request chỉ chứa 1 số trường (ví dụ chỉ đổi `address`, các trường khác null) | Cập nhật trường có giá trị, giữ nguyên các trường còn lại |
| 7 | UploadVendorDocumentUseCase | Tải lên document khi User chưa từng đăng ký Vendor Profile | Ném `VendorNotFoundException` (hoặc `VendorNotRegisteredException`), không gọi upload lên Cloudinary, không lưu DB, trả về 404/409 |
| 8 | UploadVendorDocumentUseCase | Giá trị `doc_type` không nằm trong enum (ví dụ: "PASSPORT", "INVALID_DOC") | Ném `InvalidDocTypeException` hoặc binding error, trả về HTTP 400 Bad Request với code `INVALID_DOC_TYPE` hoặc `INVALID_INPUT` |
| 9 | UploadVendorDocumentUseCase | Upload file hợp lệ | File được gửi lên Cloudinary, bản ghi lưu với `status = DocStatus.PENDING`, `reviewedBy = null`, `reviewedAt = null` |
| 10 | VendorProfileController (GET) | Người dùng có role `CUSTOMER` chưa có vendor profile gọi `GET /api/vendor/profile` | Controller/UseCase phát hiện không tìm thấy profile, ném `VendorNotFoundException`, trả về HTTP 404 Not Found |
| 11 | VendorProfileController (Any Vendor Route) | Người dùng có role `ADMIN` gọi bất kỳ endpoint nào của VENDOR (`GET /api/vendor/profile`, `PATCH /api/vendor/profile`, `POST /api/vendor/documents`) | Spring Security chặn bằng `@PreAuthorize("hasRole('VENDOR')")` (hoặc `@PreAuthorize("hasAnyRole('VENDOR', 'CUSTOMER')")`), trả về HTTP 403 Forbidden với body `{"code":"ACCESS_DENIED","message":"Access denied"}` |
| 12 | VendorProfileController (POST Registration) | Request thiếu trường bắt buộc (ví dụ `taxCode` rỗng hoặc `businessName` rỗng) | `@Valid` kích hoạt validation error, handler trả về HTTP 400 Bad Request kèm chi tiết lỗi |

---

## 1. Observation
1. **Kiến trúc ứng dụng & Quy tắc Clean Architecture**:
   - Tệp kiến trúc chuẩn mực: `backend/docs/Clean_Architecture_Rules.md`.
   - Mục tiêu module hóa: Mỗi module đặt tại `backend/src/main/java/com/danasea/backend/modules/<module_name>` gồm 4 tầng:
     - `domain/` (models, exceptions, enums).
     - `application/` (usecases, commands, results, ports).
     - `infrastructure/` (persistence entities, repositories, mappers, external adapters).
     - `presentation/` (REST controllers, request/response DTOs, exception handlers).
   - Giao tiếp liên module (Module Dependency Rule): Sử dụng public internal API, ví dụ `AccountInternalApi` tại `backend/src/main/java/com/danasea/backend/modules/account/application/api/AccountInternalApi.java` cung cấp:
     ```java
     Optional<User> findUserById(UUID id);
     Optional<User> findUserByEmail(String email);
     User saveUser(User user);
     ```
2. **Cấu hình Spring Boot & Thư viện Test**:
   - Tệp `backend/pom.xml`:
     - Spring Boot Starter Parent: version `4.1.1` (Java 21).
     - Test dependencies có sẵn:
       - `spring-boot-starter-test` (JUnit 5 Jupiter 5.11+, Mockito 5.14+, AssertJ).
       - `spring-boot-starter-webmvc-test` (cung cấp MockMvc, `@WebMvcTest`).
       - `spring-boot-starter-security-test` (cung cấp `@WithMockUser`, `SecurityMockMvcRequestPostProcessors`).
       - Spring Boot 4.1.1 hỗ trợ annotation mới `@MockitoBean` (thay thế `@MockBean`).
3. **Các Entity & Model Vendor hiện có trong Repo**:
   - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/Vendor.java`:
     Chứa các trường: `userId`, `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, `bankAccountHolder`, `verificationStatus`, `verifiedBy`, `verifiedAt`, `ratingAvg`, `ratingCount`, `badgeTier`.
   - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/VendorDocument.java`:
     Chứa các trường: `vendorId`, `docType`, `fileUrl`, `status`, `reviewedBy`, `reviewedAt`.
   - Enums:
     - `VerificationStatus.java`: `PENDING`, `APPROVED`, `REJECTED`.
     - `DocStatus.java`: `PENDING`, `APPROVED`, `REJECTED`.
     - `DocType.java`: `BUSINESS_LICENSE`, `SAFETY_CERT`.
     - `BadgeTier.java`: `NONE`, `VERIFIED`, `TOP_RATED`.
   - JPA Entities:
     - `VendorJpaEntity.java` (`@Table(name = "vendors")`).
     - `VendorDocumentJpaEntity.java` (`@Table(name = "vendor_documents")`).
   - Repositories hiện có:
     - `JpaVendorRepository.java` (kế thừa `JpaRepository<VendorJpaEntity, UUID>`).
     - `JpaVendorDocumentRepository.java` (kế thừa `JpaRepository<VendorDocumentJpaEntity, UUID>`).
4. **Cơ chế Bảo mật & SecurityContext**:
   - `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`:
     - Bật `@EnableWebSecurity` và `@EnableMethodSecurity`.
     - Cấu hình stateless session, CSRF disable.
     - Access Denied Handler trả về HTTP 403: `{"code":"ACCESS_DENIED","message":"Access denied"}`.
     - Ngoại trừ các route public (auth, health, swagger), tất cả các route khác đều yêu cầu xác thực (`anyRequest().authenticated()`).
   - `backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/security/JwtAuthenticationFilter.java`:
     - Chuyển `subject.roles()` thành `ROLE_` authorities (ví dụ: `ROLE_CUSTOMER`, `ROLE_VENDOR`, `ROLE_ADMIN`).
     - Đặt Principal là `subject.email()`.
5. **Khai thác Ngoại lệ & Xử lý lỗi hiện có**:
   - Tệp `backend/src/main/java/com/danasea/backend/shared/presentation/ErrorResponse.java`:
     ```java
     public record ErrorResponse(String code, String message) {}
     ```
   - Tệp `AuthenticationExceptionHandler.java`:
     - Map `MethodArgumentNotValidException` -> 400 Bad Request, code `INVALID_INPUT`.
     - Map `EmailAlreadyUsedException` -> 409 Conflict, code `EMAIL_ALREADY_USED`.
   - Hiện tại CHƯA có `VendorAlreadyExistsException`, `VendorNotFoundException`, `UserLockedException`, `InvalidDocTypeException`.
6. **Môi trường Sandbox Execution**:
   - Lệnh chạy `./mvnw test` trong sandbox gặp lỗi do inline ByteBuddy mock maker của Mockito cố gắng tự attach tiến trình JVM (`ByteBuddyAgent.installExternal`).
   - Khắc phục trong sandbox: sử dụng `mock-maker-subclass` (qua `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`) hoặc chạy với quyền bypass sandbox.

---

## 2. Logic Chain
1. **Từ ORIGINAL_REQUEST.md & DISPATCH.md đến Yêu cầu Test Use Case**:
   - **R1 & R2** yêu cầu:
     - `RegisterVendorProfileUseCaseTest`: Kiểm thử đăng ký thành công (status PENDING, role update sang VENDOR), ngăn chặn trùng lặp (`VendorAlreadyExistsException`), và ngăn chặn tài khoản bị khóa.
     - Quan sát từ `User.java`: `User` có trường `isLocked` (Boolean). Khi `isLocked == true`, Use Case phải từ chối và ném `UserLockedException`.
     - Quan sát từ `AccountInternalApi`: Khi đăng ký thành công, `user.setRole(Role.VENDOR)` và lưu lại qua `accountInternalApi.saveUser(user)`.
   - `UpdateVendorProfileUseCaseTest`:
     - Yêu cầu kiểm thử ngăn chặn Mass Assignment: Vendor chỉ được phép sửa `business_name`, `tax_code`, `address`, `bank_account_*`. Không được phép sửa `verification_status`, `rating_avg`, `badge_tier`.
     - Về mặt thiết kế: DTO `UpdateVendorProfileRequest` và Command chỉ chứa các trường được phép sửa. Trong UseCase, các thuộc tính nhạy cảm như `verificationStatus`, `ratingAvg`, `badgeTier` không bao giờ được gán từ input.
     - Yêu cầu kiểm thử ngăn chặn Cross-Vendor Manipulation: API không được dùng path variable `{vendorId}` hay request param. Controller lấy `userId` từ `Principal` / `SecurityContextHolder`, sau đó UseCase tìm Vendor theo `userId` (`vendorRepository.findByUserId(userId)`). Nếu không tồn tại, ném `VendorNotFoundException` (404/403). Một vendor không thể truyền ID của vendor khác để chỉnh sửa.
   - `UploadVendorDocumentUseCaseTest`:
     - Yêu cầu kiểm thử upload thành công: status ban đầu phải là `DocStatus.PENDING`, `reviewedBy` và `reviewedAt` phải là `null`.
     - Yêu cầu kiểm thử invalid `doc_type`: Giá trị không nằm trong enum `DocType` (`BUSINESS_LICENSE`, `SAFETY_CERT`) phải trả về lỗi 400 (`InvalidDocTypeException`).
     - Yêu cầu kiểm thử upload khi chưa đăng ký vendor: Nếu `userId` chưa có profile vendor trong DB, ném `VendorNotFoundException` (hoặc `VendorNotRegisteredException`), trả về 404 hoặc 409.
2. **Từ R3 & SecurityConfig đến Yêu cầu Controller Test**:
   - `VendorProfileControllerTest` (MockMvc):
     - Kiểm thử 1: CUSTOMER chưa có profile vendor khi gọi `GET /api/vendor/profile` -> Nhận mã 404 NOT FOUND (do UseCase ném `VendorNotFoundException` và `@RestControllerAdvice` bắt trả về 404).
     - Kiểm thử 2: ADMIN gọi bất kỳ VENDOR route nào (`GET /api/vendor/profile`, `PATCH /api/vendor/profile`, `POST /api/vendor/documents`) -> Nhận mã 403 FORBIDDEN (do `@PreAuthorize("hasRole('VENDOR')")` hoặc `@PreAuthorize("hasAnyRole('VENDOR', 'CUSTOMER')")` từ chối người dùng có `ROLE_ADMIN`).
     - Sử dụng `@WebMvcTest(controllers = VendorProfileController.class)` kèm `@Import(SecurityConfig.class)` và `@WithMockUser(roles = "ADMIN")` / `@WithMockUser(roles = "CUSTOMER")` hoặc Standalone MockMvc.
3. **Từ Clean Architecture đến Phân tầng Module**:
   - Tạo mới các thành phần bên dưới `com.danasea.backend.modules.vendor`:
     - `domain/exception/`: `VendorAlreadyExistsException`, `VendorNotFoundException`, `UserLockedException`, `InvalidDocTypeException`.
     - `application/port/`: `VendorRepositoryPort` (hoặc trực tiếp qua JpaVendorRepository theo convention dự án), `CloudinaryPort` (interface upload file).
     - `application/usecase/`:
       - `RegisterVendorProfileUseCase`
       - `GetVendorProfileUseCase`
       - `UpdateVendorProfileUseCase`
       - `UploadVendorDocumentUseCase`
       - `GetVendorDocumentsUseCase`
     - `presentation/controller/`: `VendorProfileController`, `VendorDocumentController` (hoặc gộp trong `VendorProfileController`).
     - `presentation/dto/`:
       - `RegisterVendorProfileRequest`
       - `UpdateVendorProfileRequest` (strict, không chứa trường nhạy cảm)
       - `VendorProfileResponse`
       - `VendorDocumentResponse`
     - `presentation/advice/`: `VendorExceptionHandler` (`@RestControllerAdvice`).

---

## 3. Caveats
1. **Chế độ Mockito trong môi trường Sandbox macOS**:
   - Môi trường chạy lệnh có sandbox hạn chế quyền Dynamic Agent Attachment của JVM. Khi chạy `mvn test` với Mockito mặc định (inline mock maker), JVM có thể báo lỗi `Could not self-attach to current VM`.
   - Biện pháp giải quyết: Cung cấp `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` chứa nội dung `mock-maker-subclass`, hoặc chạy test với cờ cho phép quyền agent.
2. **Cloudinary Integration**:
   - Dự án hiện chưa cài thư viện `cloudinary-http44` hoặc `cloudinary-core` trong `pom.xml`.
   - Trong Clean Architecture, UseCase chỉ tương tác qua interface abstraction `CloudinaryPort` (hoặc `FileStorageService`). Việc mock port này trong unit test `UploadVendorDocumentUseCaseTest` hoàn toàn độc lập và không phụ thuộc vào thư viện ngoài hay API key thật.
3. **Cơ chế xác thực người dùng trong Controller**:
   - `JwtAuthenticationFilter` hiện lưu email vào `Authentication.principal` (chuỗi String email). Do đó, Controller có thể nhận `Principal principal` (hoặc `Authentication authentication`), rồi thông qua `AccountInternalApi.findUserByEmail(principal.getName())` để lấy `userId`, hoặc tạo helper method để lấy `userId`.

---

## 4. Conclusion
1. **Đầy đủ 4 bộ kiểm thử theo đặc tả**:
   - `RegisterVendorProfileUseCaseTest`: Unit test với Mockito và JUnit 5; bao phủ luồng tạo mới, status PENDING, đổi role sang VENDOR, bắt lỗi `VendorAlreadyExistsException` (409) và chặn `User.isLocked = true` (403).
   - `UpdateVendorProfileUseCaseTest`: Unit test kiểm tra cập nhật đúng các trường cho phép, đảm bảo tuyệt đối không bị mass assignment ghi đè status/rating/badge, và chống cross-vendor manipulation bằng cách lấy vendor theo `userId` từ context.
   - `UploadVendorDocumentUseCaseTest`: Unit test kiểm tra upload lên Cloudinary, status PENDING, `reviewedBy`/`reviewedAt` null, bắt lỗi `InvalidDocTypeException` (400) và bắt lỗi upload trước khi đăng ký vendor (404/409).
   - `VendorProfileControllerTest`: MockMvc test bao phủ HTTP 404 cho CUSTOMER chưa có profile trên GET, HTTP 403 cho ADMIN truy cập VENDOR routes, HTTP 201 cho đăng ký hợp lệ, và HTTP 400 cho validation lỗi.
2. **Hệ thống Ngoại lệ & HTTP Status Code**:
   - `VendorAlreadyExistsException` -> HTTP 409 CONFLICT (`code: "VENDOR_ALREADY_EXISTS"`).
   - `VendorNotFoundException` -> HTTP 404 NOT FOUND (`code: "VENDOR_NOT_FOUND"`).
   - `UserLockedException` -> HTTP 403 FORBIDDEN (`code: "USER_LOCKED"`).
   - `InvalidDocTypeException` -> HTTP 400 BAD REQUEST (`code: "INVALID_DOC_TYPE"`).
   - `MethodArgumentNotValidException` -> HTTP 400 BAD REQUEST (`code: "INVALID_INPUT"`).
   - `AccessDeniedException` -> HTTP 403 FORBIDDEN (`code: "ACCESS_DENIED"`).
3. **DTOs & Endpoint Signatures**:
   - `POST /api/vendor/profile`: Nhận `RegisterVendorProfileRequest` (`@Valid`), trả về `VendorProfileResponse` (HTTP 201).
   - `GET /api/vendor/profile`: Trả về `VendorProfileResponse` (HTTP 200).
   - `PATCH /api/vendor/profile`: Nhận `UpdateVendorProfileRequest` (loại trừ hoàn toàn status/rating/badge), trả về `VendorProfileResponse` (HTTP 200).
   - `POST /api/vendor/documents`: Nhận `MultipartFile file`, `@RequestParam("doc_type") String docType`, trả về `VendorDocumentResponse` (HTTP 201).
   - `GET /api/vendor/documents`: Trả về `List<VendorDocumentResponse>` (HTTP 200).

---

## 5. Verification Method
1. **Kiểm tra cú pháp & Biên dịch**:
   - Chạy lệnh biên dịch:
     ```bash
     export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
     export PATH=$JAVA_HOME/bin:$PATH
     ./mvnw compile test-compile
     ```
2. **Thực thi các bộ kiểm thử Use Case & Controller**:
   - Chạy riêng từng test class sau khi hoàn thành cài đặt:
     ```bash
     ./mvnw test -Dtest=RegisterVendorProfileUseCaseTest
     ./mvnw test -Dtest=UpdateVendorProfileUseCaseTest
     ./mvnw test -Dtest=UploadVendorDocumentUseCaseTest
     ./mvnw test -Dtest=VendorProfileControllerTest
     ```
3. **Điều kiện vô hiệu hóa (Invalidation Conditions)**:
   - Nếu `RegisterVendorProfileUseCase` không gọi cập nhật role của User sang `Role.VENDOR` -> Test thất bại.
   - Nếu `UpdateVendorProfileRequest` chứa trường `verification_status` hoặc `rating_avg` -> Vi phạm tiêu chuẩn chấp thuận (Acceptance Criteria).
   - Nếu `VendorProfileController` nhận `vendorId` qua `@PathVariable` hoặc `@RequestParam` ở endpoint PATCH thay vì lấy từ SecurityContext -> Vi phạm tiêu chuẩn chấp thuận.
   - Nếu ADMIN truy cập route vendor mà không trả về 403 -> Test thất bại.
   - Nếu CUSTOMER chưa đăng ký vendor gọi GET profile mà không trả về 404 -> Test thất bại.
