# Báo Cáo Điều Tra Tích Hợp Bảo Mật & Lưu Trữ (Security & Storage Integration Report)

## 1. Observation (Quan sát thực tế)

### 1.1. Cấu hình SecurityContext & Cơ chế Xác thực hiện tại
- **File**: `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`
  - Dòng 25-28: Lớp được chú thích `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`.
  - Dòng 41-44: Session là `SessionCreationPolicy.STATELESS`.
  - Dòng 45-52: Cấu hình `accessDeniedHandler`:
    ```java
    .exceptionHandling(exception -> exception
            .accessDeniedHandler((request, response, accessDenied) -> {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access denied\"}"
                );
            }))
    ```
  - Dòng 53-65: Các endpoint public là `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`, `/api/auth/logout`, `/actuator/health`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`. Tất cả các request còn lại yêu cầu `.authenticated()`.
  - Dòng 66-69: Thêm `jwtAuthenticationFilter` trước `UsernamePasswordAuthenticationFilter.class`.
  - **Quan sát đặc biệt**: Chưa cấu hình `.authenticationEntryPoint(...)` để xử lý khi người dùng chưa đăng nhập (401).

- **File**: `backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/security/JwtAuthenticationFilter.java`
  - Dòng 47-56: 
    ```java
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
        tokenProvider.getEmail(token)
            .flatMap(userAccountPort::findByEmail)
            .filter(user -> user.enabled())
            .map(user -> authorizationPort.findSubjectByEmail(user.email()))
            .filter(subject -> !subject.roles().isEmpty())
            .ifPresent(this::authenticate);
    }
    ```
  - Dòng 61-82:
    ```java
    private void authenticate(AuthorizationSubject subject) {
        List<SimpleGrantedAuthority> authorities = subject.roles()
            .stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        authorities = java.util.stream.Stream.concat(
            authorities.stream(),
            subject.permissions()
                .stream()
                .map(SimpleGrantedAuthority::new)
        ).toList();

        var authentication = new UsernamePasswordAuthenticationToken(
            subject.email(),
            null,
            authorities
        );

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }
    ```
  - **Quan sát mấu chốt**:
    1. Principal lưu trong `UsernamePasswordAuthenticationToken` hiện tại là `subject.email()` (kiểu `String`), không phải `userId` hay `User` entity.
    2. Các quyền hạn (authorities) được gắn tiền tố `ROLE_` từ `subject.roles()`, ví dụ: `"ROLE_CUSTOMER"`, `"ROLE_VENDOR"`, `"ROLE_ADMIN"`.
    3. Bộ lọc kiểm tra trạng thái quyền trực tiếp từ `authorizationPort.findSubjectByEmail(user.email())` ở mỗi request có token.

- **File**: `backend/src/main/java/com/danasea/backend/security/authorization/infrastructure/persistence/AuthorizationAdapter.java`
  - Dòng 50-73:
    ```java
    private AuthorizationSubject toSubject(User user) {
        String role = user.getRole() != null ? user.getRole().name() : "CUSTOMER";
        ...
        return new AuthorizationSubject(
                user.getId(),
                user.getEmail(),
                Set.of(role),
                permissions);
    }
    ```
  - `user.getRole()` trả về enum `Role` (`CUSTOMER`, `VENDOR`, `ADMIN`).

- **File**: `backend/src/main/java/com/danasea/backend/security/authorization/domain/model/AuthorizationSubject.java`
  - Định nghĩa: `public record AuthorizationSubject(UUID userId, String email, Set<String> roles, Set<String> permissions)`.

- **File**: `backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java`
  - Dòng 103, 114: Controller lấy user đăng nhập qua tham số `Principal principal` và gọi `principal.getName()`, giá trị trả về chính là email.

### 1.2. Quản lý Role & Chuyển đổi Vai trò (CUSTOMER -> VENDOR)
- **File**: `backend/src/main/java/com/danasea/backend/modules/account/domain/models/Role.java`
  - Enum gồm: `CUSTOMER`, `VENDOR`, `ADMIN`.
- **File**: `backend/src/main/java/com/danasea/backend/modules/account/domain/models/User.java`
  - Thuộc tính liên quan:
    - `private Role role;`
    - `private Boolean isLocked;`
    - Kế thừa từ `BaseDomainModel`: `private UUID id;`.
- **File**: `backend/src/main/java/com/danasea/backend/modules/account/application/api/AccountInternalApi.java`
  - Cung cấp API nội bộ dùng cho giao tiếp liên module (Clean Architecture cross-module contract):
    ```java
    Optional<User> findUserByEmail(String email);
    Optional<User> findUserById(UUID id);
    boolean existsByEmail(String email);
    User saveUser(User user);
    ```
- **Cơ chế cập nhật Role**:
  - Khi user đăng ký làm Vendor:
    1. Lấy `User` qua `accountInternalApi.findUserById(userId)` (hoặc qua email).
    2. Kiểm tra `user.getIsLocked()`: nếu `true`, chặn (throw `UserLockedException`).
    3. Cập nhật role: `user.setRole(Role.VENDOR);`.
    4. Lưu lại: `accountInternalApi.saveUser(user);`.
  - Vì `JwtAuthenticationFilter` nạp quyền hạn trực tiếp từ database qua `authorizationPort` và `AccountInternalApi` ở mỗi request tiếp theo, role mới (`ROLE_VENDOR`) sẽ có hiệu lực ngay lập tức.

### 1.3. Cấu hình Cloudinary & Lưu trữ Tệp tin
- **File**: `backend/pom.xml`
  - Chưa khai báo dependency nào cho Cloudinary (chỉ có JWT, Actuator, JPA, Security, Validation, WebMvc, Redis, AMQP, Mail, Bucket4j, Testcontainers).
- **File**: `backend/src/main/resources/application.yml`
  - Không có bất kỳ cấu hình nào liên quan đến Cloudinary (`cloudinary.cloud-name`, `api-key`, `api-secret`).
- **Toàn bộ codebase**:
  - Tìm kiếm từ khóa `cloudinary`: chỉ xuất hiện duy nhất trong `ORIGINAL_REQUEST.md`.
  - Chưa có bất kỳ bean, service hay adapter nào xử lý upload file hoặc tương tác với Cloudinary.
  - Entity và Domain Model (`VendorDocument.java`, `VendorDocumentJpaEntity.java`) đã có sẵn trường `fileUrl` kiểu `String`.

### 1.4. Xử lý Lỗi và Ngoại lệ Bảo mật (401, 403, 404)
- **File**: `backend/src/main/java/com/danasea/backend/shared/presentation/ErrorResponse.java`
  - Cấu trúc chuẩn của API Response lỗi:
    ```java
    public record ErrorResponse(String code, String message) {}
    ```
- **File**: `backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationHandler.java`
  - Đã có `@RestControllerAdvice` xử lý `AccessDeniedException` domain model, trả về status 403 và body `ErrorResponse("ACCESS_DENIED", exception.getMessage())`.
- **File**: `backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationExceptionHandler.java`
  - Đã xử lý `InvalidCredentialsException` (401), `EmailAlreadyUsedException` (409), `MethodArgumentNotValidException` (400), OTP exceptions (400, 429).
- **Trạng thái hiện tại đối với Module Vendor**:
  - Chưa có `VendorExceptionHandler` hay GlobalExceptionHandler cho các ngoại lệ cụ thể của Vendor:
    - 404: `VendorNotFoundException` (khi CUSTOMER gọi `GET /api/vendor/profile` mà chưa có hồ sơ).
    - 409: `VendorAlreadyExistsException` (khi đã là vendor hoặc đã có vendor profile mà gọi lại `POST /api/vendor/profile`).
    - 403: `UserLockedException` (khi tài khoản bị khóa gọi đăng ký vendor).
    - 400: `InvalidDocTypeException` (khi upload document với `doc_type` không thuộc enum `DocType`).

### 1.5. Kết quả Kiểm thử & Môi trường Chạy Build
- Lệnh chạy kiểm thử:
  ```bash
  JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=AuthenticationControllerTest
  ```
- Kết quả: `BUILD SUCCESS`, `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`.
- Lưu ý môi trường macOS: Cần chỉ định rõ biến môi trường `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` vì mặc định `/usr/bin/java` chưa link tự động tới JVM Home. Khi chạy Mockito Inline MockMaker trên macOS sandbox, cần cờ chạy phù hợp.

---

## 2. Logic Chain (Chuỗi lập luận)

1. **Từ việc quan sát `JwtAuthenticationFilter` (Mục 1.1)**:
   - Trong `authenticate()`, `UsernamePasswordAuthenticationToken` được khởi tạo với principal là `subject.email()` (`String`), còn authorities nhận `"ROLE_" + role` (`ROLE_CUSTOMER`, `ROLE_VENDOR`, `ROLE_ADMIN`).
   - *Suy ra*: Tại Presentation layer (Controller), Spring MVC có thể inject `Principal principal` (hoặc `Authentication auth`), từ đó `principal.getName()` trả về email của người dùng.
   - Để lấy `UUID userId` theo đúng nguyên tắc Clean Architecture mà không phụ thuộc hạ tầng: Controller gọi `accountInternalApi.findUserByEmail(principal.getName())` để lấy `user.getId()`, hoặc sử dụng một helper `SecurityUtils`/`CurrentUserPort`.
   - Sau đó Controller truyền trực tiếp `UUID userId` vào method `execute(...)` của các Use Case (`RegisterVendorProfileUseCase`, `UpdateVendorProfileUseCase`, `UploadVendorDocumentUseCase`, `GetVendorProfileUseCase`).
   - Cách này đảm bảo:
     - Use Case là POJO thuần túy (Rule 2 & 3 Clean Architecture), độc lập hoàn toàn với `SecurityContextHolder` và HTTP framework.
     - Dễ dàng viết Unit Test cho Use Case mà không cần mock tĩnh `SecurityContextHolder`.

2. **Từ yêu cầu chống thao túng chéo giữa các Vendor (Cross-vendor manipulation) và Mass Assignment (R1, R2)**:
   - Endpoint `PATCH /api/vendor/profile` không có biến đường dẫn (`vendorId`), toàn bộ định danh vendor được truy xuất dựa trên `userId` lấy từ `SecurityContext`.
   - DTO cập nhật (`UpdateVendorProfileRequest`) chỉ chứa các trường được phép sửa: `businessName`, `taxCode`, `address`, `bankAccountNumber`, `bankName`, `bankAccountHolder`.
   - DTO tuyệt đối không có `verificationStatus`, `ratingAvg`, `ratingCount`, `badgeTier`.
   - Use Case lấy Vendor bằng `vendorRepository.findByUserId(userId)`. Nếu không tồn tại -> ném `VendorNotFoundException` (trả về 404). Người dùng không thể nào cập nhật hồ sơ của vendor khác vì query luôn bị ràng buộc bởi `userId` của chính họ.

3. **Từ yêu cầu phân quyền Role (R1, R3)**:
   - `POST /api/vendor/profile`: Được truy cập bởi người dùng đang có vai trò `CUSTOMER` (`@PreAuthorize("hasRole('CUSTOMER')")`). Nếu `ADMIN` gọi endpoint này, Spring Security sẽ trả về 403 Forbidden.
   - `GET /api/vendor/profile`: R3 yêu cầu: "Tests 404/error for CUSTOMER without a vendor profile on GET, and 403 for ADMIN trying to access VENDOR routes."
     - Để thỏa mãn đồng thời:
       - Cấu hình `@PreAuthorize("hasAnyRole('VENDOR', 'CUSTOMER')")` trên endpoint `GET /api/vendor/profile`.
       - Khi `ADMIN` gọi: Không có role `VENDOR` hay `CUSTOMER` -> Spring Security chặn bằng 403 Forbidden.
       - Khi `CUSTOMER` gọi: Vượt qua lớp kiểm tra bảo mật của Spring Security, Controller gọi `GetVendorProfileUseCase`. Do Customer chưa đăng ký hồ sơ vendor, Use Case ném `VendorNotFoundException` -> ControllerAdvice chuyển thành 404 Not Found.
       - Khi `VENDOR` gọi: Trả về thông tin hồ sơ với status 200 OK.
   - `PATCH /api/vendor/profile`, `POST /api/vendor/documents`, `GET /api/vendor/documents`: Cấu hình `@PreAuthorize("hasRole('VENDOR')")`. Cả `CUSTOMER` lẫn `ADMIN` đều sẽ nhận 403 Forbidden.

4. **Từ việc chưa có Cloudinary / Storage trong codebase (Mục 1.3)**:
   - Áp dụng Rule 6 (Interface/Abstraction Rule) của Clean Architecture:
     - Trong `application/port`: Định nghĩa abstraction `DocumentStoragePort`:
       ```java
       public interface DocumentStoragePort {
           String upload(byte[] fileBytes, String originalFilename, String contentType);
       }
       ```
     - Trong `infrastructure/storage`: Cài đặt `CloudinaryDocumentStorageAdapter implements DocumentStoragePort`.
     - Trong file `pom.xml`: Thêm dependency `com.cloudinary:cloudinary-http44:1.38.0`.
     - Trong `application.yml`: Thêm config properties `cloudinary.cloud-name`, `api-key`, `api-secret`.
   - *Lợi ích*:
     - `UploadVendorDocumentUseCaseTest` có thể mock `DocumentStoragePort` dễ dàng, kiểm tra 100% logic mà không cần kết nối mạng hay API key Cloudinary thật.

5. **Từ cấu trúc ErrorResponse hiện có (Mục 1.4)**:
   - Toàn bộ các module trong hệ thống (`authentication`, `authorization`) đều dùng chung record `ErrorResponse(String code, String message)`.
   - Module `vendor` cần tạo `VendorExceptionHandler` (`@RestControllerAdvice`) để ánh xạ các domain exception sang `ErrorResponse` tương ứng với đúng mã HTTP Status (400, 403, 404, 409).

---

## 3. Caveats (Các điểm cần lưu ý & Giả định)

1. **AuthenticationEntryPoint cho lỗi 401**:
   - `SecurityConfig.java` hiện tại chưa cấu hình custom `authenticationEntryPoint`. Mặc định Spring Security sẽ trả về mã 401 nhưng format có thể là trang lỗi mặc định nếu không cấu hình rõ ràng. Khuyến nghị bổ sung `authenticationEntryPoint` vào `SecurityConfig` để format JSON thống nhất dạng `{"code":"UNAUTHORIZED","message":"..."}`.
2. **DTO Request DocType**:
   - `DocType` là enum gồm `BUSINESS_LICENSE` và `SAFETY_CERT`. Khi client gửi chuỗi không khớp (ví dụ `INVALID_DOC`), Spring MVC có thể ném `MethodArgumentTypeMismatchException` hoặc `HttpMessageNotReadableException`. UseCase cũng cần kiểm tra validate hoặc nhận String/Enum để đảm bảo trả về đúng mã lỗi 400 Bad Request theo yêu cầu kiểm thử R2.
3. **Multipart file upload vs Clean Architecture**:
   - Khi upload document qua `POST /api/vendor/documents`, Presentation layer nhận `MultipartFile`. Để không làm bẩn Application layer với `org.springframework.web.multipart.MultipartFile`, Controller nên đọc `byte[] bytes = file.getBytes()` và truyền `bytes`, `filename`, `contentType` vào Use Case, hoặc dùng một DTO nội bộ.

---

## 4. Conclusion (Kết luận & Khuyến nghị Thực thi)

### 4.1. Chữ ký các Port, Bean & Entity cần triển khai

1. **Application Ports (`com.danasea.backend.modules.vendor.application.port`)**:
   - `VendorRepositoryPort`:
     ```java
     public interface VendorRepositoryPort {
         Optional<Vendor> findByUserId(UUID userId);
         Optional<Vendor> findById(UUID id);
         boolean existsByUserId(UUID userId);
         Vendor save(Vendor vendor);
     }
     ```
   - `VendorDocumentRepositoryPort`:
     ```java
     public interface VendorDocumentRepositoryPort {
         VendorDocument save(VendorDocument document);
         List<VendorDocument> findByVendorId(UUID vendorId);
     }
     ```
   - `DocumentStoragePort`:
     ```java
     public interface DocumentStoragePort {
         String upload(byte[] content, String originalFilename, String contentType);
     }
     ```

2. **Cập nhật JPA Repositories (`com.danasea.backend.modules.vendor.infrastructure.persistence.repositories`)**:
   - `JpaVendorRepository`:
     ```java
     Optional<VendorJpaEntity> findByUserId(UUID userId);
     boolean existsByUserId(UUID userId);
     ```
   - `JpaVendorDocumentRepository`:
     ```java
     List<VendorDocumentJpaEntity> findByVendorId(UUID vendorId);
     ```

3. **Cập nhật `pom.xml`**:
   - Thêm dependency Cloudinary:
     ```xml
     <dependency>
         <groupId>com.cloudinary</groupId>
         <artifactId>cloudinary-http44</artifactId>
         <version>1.38.0</version>
     </dependency>
     ```

4. **Khai báo Beans (`com.danasea.backend.modules.vendor.infrastructure.config.VendorBeans`)**:
   ```java
   @Configuration
   public class VendorBeans {
       @Bean
       public RegisterVendorProfileUseCase registerVendorProfileUseCase(
               VendorRepositoryPort vendorRepository,
               AccountInternalApi accountInternalApi
       ) {
           return new RegisterVendorProfileUseCase(vendorRepository, accountInternalApi);
       }

       @Bean
       public GetVendorProfileUseCase getVendorProfileUseCase(
               VendorRepositoryPort vendorRepository
       ) {
           return new GetVendorProfileUseCase(vendorRepository);
       }

       @Bean
       public UpdateVendorProfileUseCase updateVendorProfileUseCase(
               VendorRepositoryPort vendorRepository
       ) {
           return new UpdateVendorProfileUseCase(vendorRepository);
       }

       @Bean
       public UploadVendorDocumentUseCase uploadVendorDocumentUseCase(
               VendorRepositoryPort vendorRepository,
               VendorDocumentRepositoryPort documentRepository,
               DocumentStoragePort storagePort
       ) {
           return new UploadVendorDocumentUseCase(vendorRepository, documentRepository, storagePort);
       }

       @Bean
       public GetVendorDocumentsUseCase getVendorDocumentsUseCase(
               VendorRepositoryPort vendorRepository,
               VendorDocumentRepositoryPort documentRepository
       ) {
           return new GetVendorDocumentsUseCase(vendorRepository, documentRepository);
       }
   }
   ```

5. **Bộ xử lý ngoại lệ (`com.danasea.backend.modules.vendor.presentation.VendorExceptionHandler`)**:
   - `VendorNotFoundException` -> HTTP 404 (`"VENDOR_NOT_FOUND"`)
   - `VendorAlreadyExistsException` -> HTTP 409 (`"VENDOR_ALREADY_EXISTS"`)
   - `UserLockedException` -> HTTP 403 hoặc 400 (`"USER_LOCKED"`)
   - `InvalidDocTypeException` -> HTTP 400 (`"INVALID_DOC_TYPE"`)

---

## 5. Verification Method (Phương pháp Kiểm chứng Độc lập)

1. **Kiểm tra Biên dịch Toàn bộ Dự án**:
   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw clean test-compile
   ```
   - Điều kiện đạt: `BUILD SUCCESS` không có lỗi cú pháp hay thiếu import.

2. **Kiểm tra các Use Case Unit Tests**:
   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=RegisterVendorProfileUseCaseTest,UpdateVendorProfileUseCaseTest,UploadVendorDocumentUseCaseTest
   ```
   - Điều kiện đạt:
     - `RegisterVendorProfileUseCaseTest`: Xác nhận lưu trạng thái `PENDING`, role chuyển sang `VENDOR`, chặn trùng lặp (`VendorAlreadyExistsException`), chặn user bị khóa (`isLocked == true`).
     - `UpdateVendorProfileUseCaseTest`: Xác nhận cập nhật thông tin thành công, ngăn chặn mass assignment (bỏ qua `verificationStatus`/`ratingAvg`), ngăn chặn thao túng chéo giữa các vendor (dựa hoàn toàn vào `userId`).
     - `UploadVendorDocumentUseCaseTest`: Xác nhận upload thành công (status `PENDING`, `reviewedBy` và `reviewedAt` là `null`), chặn `doc_type` không hợp lệ (400), chặn upload khi chưa đăng ký vendor (404/409).

3. **Kiểm tra Controller Integration Tests**:
   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./mvnw test -Dtest=VendorProfileControllerTest
   ```
   - Điều kiện đạt:
     - CUSTOMER gọi `GET /api/vendor/profile` khi chưa có hồ sơ trả về mã 404.
     - ADMIN gọi các route của VENDOR trả về mã 403.
     - VENDOR gọi `GET /api/vendor/profile` trả về 200 OK.
