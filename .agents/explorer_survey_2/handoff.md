# Báo Cáo Khảo Sát Tầng API & Security (Handoff Report)

## 1. Observation

### 1.1 Cấu trúc Project & Clean Architecture
- **Tài liệu quy chuẩn kiến trúc**: `backend/docs/Clean_Architecture_Rules.md`.
  - Quy tắc phụ thuộc (Dependency Rule, dòng 13): `Presentation → Application → Domain ← Infrastructure`.
  - Quy tắc độc lập framework (Framework Independence, dòng 123-128): Use cases là POJO, không gắn annotation `@Service` của Spring.
  - Quy tắc giao tiếp giữa các module (Module Dependency Rule, dòng 195-212): Các module giao tiếp qua Public abstraction/contract (`Module A → Public contract → Module B`), không truy cập trực tiếp repo hay entity nội bộ của module khác.
  - Cấu hình beans: Use cases được instantiate trong các file cấu hình `@Configuration` riêng như `backend/src/main/java/com/danasea/backend/config/ApplicationBeans.java` (dòng 23-86) và `backend/src/main/java/com/danasea/backend/security/authorization/infrastructure/security/AuthorizationBeans.java` (dòng 13-32).

### 1.2 Cấu trúc API, Route Patterns, DTOs & Response
- **Controllers mẫu**:
  - `backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationController.java`:
    - Dòng 33-35: `@RestController`, `@RequestMapping("/api/auth")`, `@RequiredArgsConstructor`.
    - Dòng 46: `public AuthenticationResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response)`. Trả về record DTO trực tiếp (Spring Boot tự động wrap thành HTTP 200 JSON).
    - Dòng 77, 93, 103, 112: `ResponseEntity<RefreshResponse>`, `ResponseEntity<Void>` cho các endpoint trả về `201`, `202 Accepted`, hoặc `204 No Content`.
    - Hệ thống **không dùng** lớp wrapper tổng quát `ApiResponse<T>`. Thay vào đó, API trả trực tiếp DTO (record) hoặc `ResponseEntity<DTO>`.
  - `backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationController.java`:
    - Dòng 8-10: `@RestController`, `@RequestMapping("/api/authorization")`.
    - Dòng 13, 19, 25, 31: Dùng `@PreAuthorize("hasRole('ADMIN')")`, `@PreAuthorize("hasRole('VENDOR')")`, `@PreAuthorize("hasRole('USER')")`, `@PreAuthorize("hasAuthority('PRODUCT_READ')")`.
- **DTOs**:
  - Khai báo dưới dạng Java `record` đặt tại package `presentation.dto` của từng module (ví dụ: `com.danasea.backend.security.authentication.presentation.dto.*`).
  - Dùng bean validation chuẩn Jakarta (`@Valid`, `@NotBlank`, `@Size`, etc.).

### 1.3 Cơ Chế Authentication & Authorization
- **Security Configuration**:
  - `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`:
    - Dòng 25-28: `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`.
    - Dòng 41-44: `sessionCreationPolicy(SessionCreationPolicy.STATELESS)`.
    - Dòng 45-52: Cấu hình `accessDeniedHandler`:
      ```java
      .accessDeniedHandler((request, response, accessDenied) -> {
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
          response.getWriter().write(
              "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access denied\"}"
          );
      })
      ```
    - Dòng 53-65: Các endpoint public gồm `/api/auth/**`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`. Mọi request khác `.anyRequest().authenticated()`.
    - Dòng 66-69: `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`.
- **JWT Authentication Filter**:
  - `backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/security/JwtAuthenticationFilter.java`:
    - Dòng 38-45: Đọc header `Authorization: Bearer <token>`.
    - Dòng 50-56: Trích xuất email từ JWT (`tokenProvider.getEmail(token)`), tìm user qua `userAccountPort.findByEmail(email)`, kiểm tra `user.enabled()`, tìm subject qua `authorizationPort.findSubjectByEmail(user.email())`.
    - Dòng 61-82 (phương thức `authenticate`):
      ```java
      List<SimpleGrantedAuthority> authorities = subject.roles()
          .stream()
          .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
          .toList();

      authorities = java.util.stream.Stream.concat(
          authorities.stream(),
          subject.permissions().stream().map(SimpleGrantedAuthority::new)
      ).toList();

      var authentication = new UsernamePasswordAuthenticationToken(
          subject.email(),
          null,
          authorities
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);
      ```
  - `Role` Enum: `backend/src/main/java/com/danasea/backend/modules/account/domain/models/Role.java`:
    - Dòng 3-5: `CUSTOMER`, `VENDOR`, `ADMIN`.
    - Authority trong Spring Security: `ROLE_CUSTOMER`, `ROLE_VENDOR`, `ROLE_ADMIN`.

### 1.4 Cơ Chế Nhận Diện User / Vendor Hiện Tại
- **Principal & SecurityContext**:
  - `SecurityContextHolder.getContext().getAuthentication()` giữ `UsernamePasswordAuthenticationToken` với:
    - `principal`: là chuỗi `email` (`subject.email()`, kiểu `String`).
    - `credentials`: `null`.
    - `authorities`: các `SimpleGrantedAuthority` dạng `ROLE_<ROLE>` và permission strings.
  - Trong Controller: Có thể inject trực tiếp `Principal principal` hoặc `Authentication authentication`. Gọi `principal.getName()` sẽ trả về `email` của user.
    - Xem `AuthenticationController.java` dòng 103-107:
      ```java
      public ResponseEntity<Void> sendOtp(Principal principal) {
          if (principal == null || principal.getName() == null) {
              return ResponseEntity.status(401).build();
          }
          sendVerificationOtpUseCase.execute(principal.getName());
      ```
- **Không có Custom Resolver**:
  - Không có custom annotation như `@CurrentUser` hay `HandlerMethodArgumentResolver` trong codebase.
- **Truy vấn thông tin User & Vendor**:
  - `AccountInternalApi` (`backend/src/main/java/com/danasea/backend/modules/account/application/api/AccountInternalApi.java` dòng 12):
    - `Optional<User> findUserByEmail(String email);`
    - `Optional<User> findUserById(UUID id);`
  - `Vendor` domain model (`backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/Vendor.java` dòng 13-27):
    - Chứa `id` (UUID), `userId` (UUID), `verificationStatus` (`VerificationStatus`: `PENDING`, `APPROVED`, `REJECTED`).
  - `JpaVendorRepository` (`backend/src/main/java/com/danasea/backend/modules/vendor/infrastructure/persistence/repositories/JpaVendorRepository.java` dòng 11):
    - Đang kế thừa `JpaRepository<VendorJpaEntity, UUID>`, chưa có phương thức `findByUserId(UUID userId)`.

### 1.5 Cơ Chế Audit Log Hiện Có
- **Model**: `backend/src/main/java/com/danasea/backend/modules/account/domain/models/AuditLog.java`:
  - Dòng 13-19:
    ```java
    public class AuditLog extends BaseDomainModel {
        private UUID actorUserId;
        private String action;
        private String entityType;
        private UUID entityId;
        private String metadata;
    }
    ```
- **Entity**: `backend/src/main/java/com/danasea/backend/modules/account/infrastructure/persistence/entities/AuditLogJpaEntity.java` (table `audit_logs`).
- **Repository**: `backend/src/main/java/com/danasea/backend/modules/account/infrastructure/persistence/repositories/JpaAuditLogRepository.java`.
- **Trạng thái sử dụng hiện tại**:
  - `JpaAuditLogRepository` chưa được inject hay sử dụng ở bất kỳ service/usecase nào trong codebase.
  - Chưa có `AuditLogService` hay phương thức lưu audit log trong `AccountInternalApi`.

### 1.6 Xử Lý Lỗi & Exception Handling (400, 403, 404)
- **Chuẩn Error Response**:
  - `backend/src/main/java/com/danasea/backend/shared/presentation/ErrorResponse.java`:
    - Dòng 3-6: `public record ErrorResponse(String code, String message) {}`.
    - Format JSON: `{"code": "...", "message": "..."}`.
- **Xử lý 403 Forbidden**:
  - Filter chain: `SecurityConfig.java` (dòng 46-52) trả JSON `{"code":"ACCESS_DENIED","message":"Access denied"}` với HTTP status 403.
  - Presentation Advice: `backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationHandler.java`:
    - Dòng 14-22: Bắt `AccessDeniedException` (domain exception của security/authorization), trả HTTP 403 với `ErrorResponse("ACCESS_DENIED", exception.getMessage())`.
- **Xử lý 400 Bad Request**:
  - `backend/src/main/java/com/danasea/backend/security/authentication/presentation/AuthenticationExceptionHandler.java`:
    - Dòng 48-64: Bắt `MethodArgumentNotValidException` (lỗi validation `@Valid`), lấy lỗi field đầu tiên, trả HTTP 400 với `ErrorResponse("INVALID_INPUT", field + ": " + message)`.
    - Dòng 66-73: Bắt `OtpInvalidException`, `OtpExpiredException`, `OtpMaxAttemptsExceededException`, trả HTTP 400 với `ErrorResponse("INVALID_OTP", message)`.
- **Xử lý 404 Not Found**:
  - Chưa có class `ResourceNotFoundException` hay handler 404 nào trong toàn bộ project.

### 1.7 Hiện Trạng Module Services Hiện Có
- Module `com/danasea/backend/modules/service`:
  - Đã có domain models: `Category`, `Service`, `ServiceStatus`, `ServiceImage`, `ServiceSafetyDocument`, `ServiceSlot`, `RecentlyViewed`, `Wishlist`.
  - Đã có persistence entities & repositories: `ServiceJpaEntity`, `CategoryJpaEntity`, `ServiceImageJpaEntity`, v.v.
  - `ServiceStatus` (`ServiceStatus.java` dòng 3-5): `DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, PAUSED`.
  - Chưa có tầng `application` (usecases, ports) và chưa có tầng `presentation` (controllers, dtos, exception handlers).

### 1.8 Môi Trường Build & Test
- Java Version: Temurin-21.0.10 (`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
- Spring Boot version: `4.1.1` (parent pom).
- Khi chạy `./mvnw test` trong macOS Sandbox, ByteBuddy self-attach bị chặn (`Operation not permitted at jdk.attach/sun.tools.attach.VirtualMachineImpl.connect`). Khi chạy với `BypassSandbox: true`, toàn bộ 7 test trong `AuthenticationControllerTest` và 5 test trong `LoginUseCaseTest` đều PASS 100%.

---

## 2. Logic Chain

1. **Từ Quan Sát 1.1 & 1.7 (Clean Architecture & Module Structure)**:
   - Vì dự án áp dụng Clean Architecture nghiêm ngặt theo `Clean_Architecture_Rules.md`, việc triển khai Services Module phải tuân thủ cấu trúc 4 tầng chuẩn:
     - `com.danasea.backend.modules.service.domain`: Domain logic, enum, business rules validation (weather params, photo check, status transitions), domain exceptions.
     - `com.danasea.backend.modules.service.application`: Use Cases (`CreateServiceUseCase`, `UpdateServiceUseCase`, `SubmitServiceForReviewUseCase`, `ApproveServiceUseCase`, `RejectServiceUseCase`, `DeleteServiceUseCase`, v.v.) và Ports (`ServiceRepositoryPort`, `CategoryPort`, `VendorPort`, `AuditLogPort`).
     - `com.danasea.backend.modules.service.infrastructure`: Repositories adapter, entity mappers (`ServiceMapper`), và bean configuration class (`ServiceBeans.java`).
     - `com.danasea.backend.modules.service.presentation`: `VendorServiceController`, `AdminServiceController`, DTO records, và `ServiceExceptionHandler`.

2. **Từ Quan Sát 1.3 (Security & Phân Quyền)**:
   - `SecurityConfig` đã kích hoạt `@EnableMethodSecurity`.
   - `JwtAuthenticationFilter` cấp quyền dạng `ROLE_VENDOR`, `ROLE_ADMIN`, `ROLE_CUSTOMER`.
   - Do đó, để đáp ứng yêu cầu R4 (chặn role `CUSTOMER`, `ADMIN` vào `/api/vendor/services/**` và chặn `VENDOR` vào `/api/admin/services/**`), chỉ cần đánh dấu:
     - `@PreAuthorize("hasRole('VENDOR')")` cho `VendorServiceController` (hoặc các method tương ứng).
     - `@PreAuthorize("hasRole('ADMIN')")` cho `AdminServiceController`.
   - Bất kỳ request nào không đúng role sẽ bị Spring Security chặn và trả về HTTP 403 với body JSON `{"code":"ACCESS_DENIED","message":"Access denied"}`.

3. **Từ Quan Sát 1.4 (Nhận Diện User / Vendor Hiện Tại)**:
   - Vì principal trong `SecurityContext` chỉ là `email` (String), khi Vendor gửi request:
     - Controller inject `Principal principal` (hoặc `Authentication authentication`), lấy `email = principal.getName()`.
     - Use Case (thông qua Port kết nối sang Account và Vendor module) sẽ:
       1. Tìm `User` theo email thông qua `AccountInternalApi` để lấy `userId`.
       2. Tìm `Vendor` theo `userId` để lấy `vendorId` và `verificationStatus`.
     - Kiểm tra rule R3:
       - Nếu `vendor.getVerificationStatus() != VerificationStatus.APPROVED`, chặn không cho tạo service (báo lỗi HTTP 400 hoặc 403 với thông điệp rõ ràng).
       - Khi sửa/xem/xóa service, kiểm tra `service.getVendorId().equals(currentVendor.getId())`. Nếu không khớp, trả về HTTP 403 `ACCESS_DENIED` ("Không được phép sửa dịch vụ của Vendor khác").

4. **Từ Quan Sát 1.5 & Clean Architecture Rule 11 (Audit Logging)**:
   - Bảng `audit_logs` và entity `AuditLogJpaEntity` đã có sẵn trong database và `account` module.
   - Clean Architecture cấm `service` module phụ thuộc trực tiếp vào repository nội bộ của `account` (`JpaAuditLogRepository`).
   - Do đó, cách sạch nhất là:
     - Mở rộng `AccountInternalApi` (hoặc tạo port `AuditLogPort` trong service module với adapter ghi vào `JpaAuditLogRepository` / gọi `AccountInternalApi`):
       - Phương thức: `void recordAuditLog(UUID actorUserId, String action, String entityType, UUID entityId, String metadata);`
     - Trong `ApproveServiceUseCase`: ghi action `"SERVICE_APPROVED"`, entityType `"SERVICE"`, entityId là ID của service.
     - Trong `RejectServiceUseCase`: ghi action `"SERVICE_REJECTED"`, entityType `"SERVICE"`, entityId là ID của service, metadata chứa `{ "reason": rejectionReason }`.

5. **Từ Quan Sát 1.6 (Error Handling & HTTP Statuses)**:
   - Chuẩn response lỗi trong hệ thống là record `ErrorResponse(String code, String message)`.
   - Cần triển khai `ServiceExceptionHandler` (`@RestControllerAdvice`) trong `service.presentation` xử lý:
     - 400 Bad Request: Lỗi nghiệp vụ (chuyển trạng thái không hợp lệ, thiếu ảnh khi submit, thiếu thông số gió/sóng khi `weatherSensitive=true`, xóa khi status != DRAFT).
     - 403 Forbidden: Vendor chưa verify APPROVED, hoặc vendor cố tình sửa dịch vụ của vendor khác (`AccessDeniedException` hoặc `NotServiceOwnerException`).
     - 404 Not Found: `ServiceNotFoundException`, `CategoryNotFoundException` (trường hợp category không tồn tại hoặc `isActive == false`).

---

## 3. Caveats

1. **Vendor Module Interface**: Hiện tại module `vendor` chưa có `VendorInternalApi` hay method `findByUserId` trong `JpaVendorRepository`. Việc tìm kiếm vendor theo `userId` cần bổ sung query method `Optional<VendorJpaEntity> findByUserId(UUID userId)` trong `JpaVendorRepository` và có thể tạo contract `VendorInternalApi` để tuân thủ Module Dependency Rule.
2. **AuditLog Metadata Format**: Cột `metadata` trong `AuditLogJpaEntity` là `String`. Nên chuẩn hóa lưu dưới dạng chuỗi JSON hợp lệ (ví dụ dùng ObjectMapper để serialize object lý do từ chối).
3. **Môi trường chạy Test trên macOS**: Mockito ByteBuddy inline mock maker bị chặn bởi macOS Sandbox (`Operation not permitted` khi connect qua `VirtualMachineImpl`). Cần chạy test với flag `BypassSandbox: true` hoặc cấu hình surefire phù hợp.

---

## 4. Conclusion

- **Cấu trúc API**: Phải xây dựng 2 controllers: `VendorServiceController` (`/api/vendor/services`) và `AdminServiceController` (`/api/admin/services`). DTOs dạng Java `record` với validation Jakarta. Dùng `ErrorResponse(code, message)` cho mọi phản hồi lỗi.
- **Phân quyền**: Áp dụng `@PreAuthorize("hasRole('VENDOR')")` cho Vendor API và `@PreAuthorize("hasRole('ADMIN')")` cho Admin API.
- **Nhận diện Vendor**: Sử dụng `principal.getName()` để lấy email, resolve ra `userId` qua `AccountInternalApi`, resolve ra `vendorId` qua `JpaVendorRepository` (hoặc `VendorInternalApi`), đồng thời kiểm tra `verificationStatus == APPROVED` và quyền sở hữu `service.vendorId == currentVendor.id`.
- **Audit Logging**: Lưu audit log vào bảng `audit_logs` khi Admin Approve (`SERVICE_APPROVED`) hoặc Reject (`SERVICE_REJECTED`) thông qua abstraction port tuân thủ Clean Architecture.
- **Error Mapping**:
  - `400`: Thiếu ảnh khi submit, sai thông số thời tiết, trạng thái không hợp lệ khi xóa (status != DRAFT). Mã: `INVALID_INPUT`, `INVALID_SERVICE_STATE`, `MISSING_REQUIRED_FIELDS`.
  - `403`: Role không hợp lệ, Vendor chưa được approve, không phải chủ sở hữu service. Mã: `ACCESS_DENIED`.
  - `404`: Không tìm thấy Service (`SERVICE_NOT_FOUND`) hoặc Category không tồn tại/bị inactive (`CATEGORY_NOT_FOUND`).

---

## 5. Verification Method

### 5.1 Kiểm Tra Độc Lập
1. **Kiểm tra biên dịch và test hiện có**:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
   cd /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_vendor_services_module/backend
   ./mvnw test -Dtest=AuthenticationControllerTest,LoginUseCaseTest
   ```
   *Kết quả kỳ vọng*: `BUILD SUCCESS`, 12 tests passed.

2. **Kiểm tra cấu hình Security & Role Authorization**:
   - Soát file `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`: Kiểm tra `@EnableMethodSecurity`, `accessDeniedHandler`, và matcher cho `/api/auth/**`.
   - Soát file `backend/src/main/java/com/danasea/backend/security/authentication/infrastructure/security/JwtAuthenticationFilter.java`: Kiểm tra tiền tố `ROLE_` được gắn vào authority.
   - Soát file `backend/src/main/java/com/danasea/backend/security/authorization/presentation/AuthorizationController.java`: Tham khảo ví dụ mẫu `@PreAuthorize("hasRole('VENDOR')")` và `@PreAuthorize("hasRole('ADMIN')")`.

3. **Kiểm tra AuditLog và User/Vendor Entity**:
   - Soát file `backend/src/main/java/com/danasea/backend/modules/account/domain/models/AuditLog.java`.
   - Soát file `backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/Vendor.java` và `VerificationStatus.java`.
   - Soát file `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Service.java` và `ServiceStatus.java`.

4. **Điều Kiện Bác Bỏ (Invalidation Conditions)**:
   - Nếu `SecurityConfig` không kích hoạt `@EnableMethodSecurity` thì `@PreAuthorize` sẽ không có hiệu lực (đã xác nhận: dòng 27 có `@EnableMethodSecurity`).
   - Nếu `JwtAuthenticationFilter` không thêm tiền tố `ROLE_` thì `hasRole('VENDOR')` sẽ thất bại (đã xác nhận: dòng 64 có `new SimpleGrantedAuthority("ROLE_" + role)`).
   - Nếu trả về format lỗi khác `ErrorResponse(code, message)` thì sẽ vi phạm tính nhất quán của frontend/API contract (đã xác nhận: dùng `com.danasea.backend.shared.presentation.ErrorResponse`).
