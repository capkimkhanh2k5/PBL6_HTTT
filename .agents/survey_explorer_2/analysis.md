# Chi Tiết Khảo Sát: Spring Security & Cơ Sở Dữ Liệu (Categories Module)

**Agent**: `survey_explorer_2` (Security & Database Schema Explorer)  
**Ngày khảo sát**: 2026-09-10  
**Thư mục làm việc**: `.agents/survey_explorer_2/`  
**Dự án mục tiêu**: `backend/` (Spring Boot 4.x / Java 21)

---

## 1. Cấu Hình Spring Security & Cơ Chế Phân Quyền (RBAC)

### 1.1. Vị trí và Cấu hình `SecurityConfig`
- **File**: `backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`
- **Package khai báo**: `package com.danasea.backend.modules.systemconfig;`
- **Annotations**:
  - `@Configuration`
  - `@EnableWebSecurity`
  - `@EnableMethodSecurity` (Bật hỗ trợ bảo mật cấp phương thức: `@PreAuthorize`, `@Secured`, `@RolesAllowed`)

### 1.2. Phân tích `SecurityFilterChain` hiện tại
```java
@Bean
SecurityFilterChain applicationSecurityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter
) throws Exception {
    return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    ))
            .exceptionHandling(exception -> exception
                    .accessDeniedHandler((request, response, accessDenied) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(
                                "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access denied\"}"
                        );
                    }))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/auth/login",
                            "/api/auth/register",
                            "/api/auth/refresh",
                            "/api/auth/logout",
                            "/actuator/health",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**"
                    ).permitAll()
                    .anyRequest()
                    .authenticated())
            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            )
            .build();
}
```

### 1.3. Cơ chế Xác thực & Trích xuất Quyền (JWT & Authorities)
- **Filter**: `com.danasea.backend.security.authentication.infrastructure.security.JwtAuthenticationFilter`
- **Cách trích xuất vai trò (Role)**:
  ```java
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
  ```
- **Hệ thống Role hiện có** (`com.danasea.backend.modules.account.domain.models.Role`):
  - `CUSTOMER`
  - `VENDOR`
  - `ADMIN`
- **Quy tắc tiền tố**: Vì filter tự động gắn prefix `"ROLE_"`, nên role `ADMIN` sẽ trở thành authority `"ROLE_ADMIN"`.
- Do đó:
  - Trong SpEL expression: `@PreAuthorize("hasRole('ADMIN')")` sẽ khớp với `"ROLE_ADMIN"`.
  - Hoặc `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.
  - Minh chứng thực tế trong codebase tại `com.danasea.backend.security.authorization.presentation.AuthorizationController`:
    ```java
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "ADMIN access granted";
    }
    ```

---

## 2. Phương Án Cấu Hình Security Cho Module Categories

Dựa trên yêu cầu:
- `GET /api/categories` — Public, cây phân cấp cha/con, chỉ lấy `is_active=true`.
- `GET /api/admin/categories` — Admin xem tất cả.
- `POST /api/admin/categories` — Admin tạo mới.
- `PATCH /api/admin/categories/{id}` — Admin sửa.
- `PATCH /api/admin/categories/{id}/deactivate` — Admin soft delete.

### 2.1. Cấu hình Public Endpoint (`GET /api/categories`)
Hiện tại `SecurityConfig` có `.anyRequest().authenticated()`. Nếu không khai báo whitelist, các request không có JWT Bearer token gửi tới `GET /api/categories` sẽ bị chặn (401/403).

**Giải pháp đề xuất**:
Cập nhật `SecurityConfig.java`:
```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers(
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/api/auth/logout",
                "/actuator/health",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**"
        ).permitAll()
        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
        // Hoặc gom chung vào danh sách permitAll()
```
*Lưu ý an toàn*: Khuyến nghị dùng `HttpMethod.GET` cụ thể cho `/api/categories` để đảm bảo chỉ có thao tác đọc là công khai, ngăn ngừa rủi ro nếu có route phát sinh.

### 2.2. Cấu hình Admin Endpoints (`/api/admin/categories/**`)
Áp dụng chiến lược bảo vệ đa tầng (Defense-in-depth):

1. **Tầng 1 - URL-level Authorization trong `SecurityConfig.java`**:
   ```java
   .requestMatchers("/api/admin/**").hasRole("ADMIN")
   // hoặc .requestMatchers("/api/admin/categories/**").hasRole("ADMIN")
   ```
   Điều này đảm bảo toàn bộ đường dẫn `/api/admin/` tự động yêu cầu quyền `ROLE_ADMIN` ngay tại FilterChain, phản hồi 403 Forbidden nếu user không phải ADMIN.

2. **Tầng 2 - Method-level Authorization trong Controller**:
   Trên `AdminCategoryController`:
   ```java
   @RestController
   @RequestMapping("/api/admin/categories")
   @PreAuthorize("hasRole('ADMIN')")
   public class AdminCategoryController {
       // Tất cả methods POST, GET, PATCH tự động thừa hưởng quyền ADMIN
   }
   ```
   Đây là chuẩn mực bảo mật Spring Boot, đồng bộ với cách triển khai trong `AuthorizationController`.

---

## 3. Quản Lý Database Migrations & Hiện Trạng Schema

### 3.1. Công cụ Quản lý Migration
- **Flyway**: Không sử dụng (không có dependency `flyway-core` trong `pom.xml`, không có thư mục `db/migration`).
- **Liquibase**: Không sử dụng (không có dependency `liquibase-core`, không có `changelog`).
- **JPA / Hibernate `ddl-auto`**: Dự án đang sử dụng **`spring.jpa.hibernate.ddl-auto: update`** (được cấu hình tại `backend/src/main/resources/application.yml:16`).
- **Database**: PostgreSQL (`org.postgresql.Driver`), kết nối qua các biến môi trường `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- **SQL Scripts**: Không có bất kỳ file script `.sql` nào (`schema.sql` hay `data.sql`). Toàn bộ cấu trúc bảng do Hibernate JPA tự động sinh/cập nhật từ JPA Entities.

### 3.2. Bảng và Entity `Category` đã có sẵn
Trong codebase đã có sẵn một phần Entity và Model cho Category:
- **JPA Entity**: `com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity`
  - Kế thừa: `BaseJpaEntity` (`UUID id`, `OffsetDateTime createdAt`, `OffsetDateTime updatedAt`).
  - Nội dung hiện tại:
    ```java
    @Entity
    @Getter
    @Setter
    @Table(name = "categorys") // <-- CHÚ Ý: Hiện tại đang để tên "categorys"
    public class CategoryJpaEntity extends BaseJpaEntity {
        private String name;
        private String nameEn;
        private String slug;
        private UUID parentId;
        private String iconUrl;
        private Boolean isActive;
    }
    ```
- **Tài liệu thiết kế (`docs/DANASEA_Database_Design.docx`)**:
  - Tên bảng chuẩn: **`categories`** (chữ "ies", không phải "categorys").
  - Mô tả trường:
    - `id`: `UUID`, PK
    - `name`: `VARCHAR(100)`, NOT NULL
    - `name_en`: `VARCHAR(100)`, Bản dịch tiếng Anh (đa ngôn ngữ)
    - `slug`: `VARCHAR(120)`, UNIQUE
    - `parent_id`: `UUID`, FK -> `categories.id`, NULL nếu là danh mục gốc
    - `icon_url`: `TEXT`
    - `is_active`: `BOOLEAN`, DEFAULT `true`
  - **Khuyến nghị điều chỉnh**:
    Nên đổi `@Table(name = "categorys")` thành `@Table(name = "categories")` để khớp với tài liệu thiết kế hệ thống và bổ sung các ràng buộc `@Column(name = "...", nullable = ..., unique = ...)` đầy đủ.

- **Domain Model**: `com.danasea.backend.modules.service.domain.models.Category`
  - Kế thừa: `BaseDomainModel` (`id`, `createdAt`, `updatedAt`).
  - Đã có đầy đủ thuộc tính: `name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive`.

- **Repository**: `com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaCategoryRepository`
  - Hiện tại: `public interface JpaCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {}`

### 3.3. Bảng và Entity `Service` liên quan
- **JPA Entity**: `com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity`
  - Tên bảng: `@Table(name = "services")`.
  - Trường liên kết danh mục: `private UUID categoryId;` (Lưu ý: liên kết bằng ID, không dùng `@ManyToOne`, tuân thủ kiến trúc phân tách lỏng giữa các Entity).
  - Trạng thái dịch vụ:
    ```java
    @Enumerated(EnumType.STRING)
    private ServiceStatus status;
    ```
- **Enum `ServiceStatus`** (`com.danasea.backend.modules.service.domain.models.ServiceStatus`):
  ```java
  public enum ServiceStatus {
      DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, PAUSED
  }
  ```
- **Quy tắc nghiệp vụ Active Services khi Deactivate Category**:
  - Yêu cầu R1: Chặn và ném exception (ví dụ: `CategoryHasActiveServicesException`) nếu category đang có services active tham chiếu tới.
  - Định nghĩa "Active Service": Theo tài liệu thiết kế `DANASEA_Database_Design.docx`, dịch vụ đang hoạt động công khai trên sàn là dịch vụ có `status = ServiceStatus.PUBLISHED`.
  - Phương thức cần bổ sung vào `JpaServiceRepository`:
    ```java
    boolean existsByCategoryIdAndStatus(UUID categoryId, ServiceStatus status);
    ```
    Hoặc kiểm tra theo danh sách trạng thái nếu nghiệp vụ mở rộng:
    ```java
    boolean existsByCategoryIdAndStatusIn(UUID categoryId, Collection<ServiceStatus> statuses);
    ```

---

## 4. Kiến Trúc & Cấu Trúc Module Đề Xuất

Dự án tuân thủ tài liệu `backend/docs/Clean_Architecture_Rules.md`.
Hai phương án tổ chức code:

### Phương án A (Khuyến nghị): Mở rộng trong `modules/service`
Vì `CategoryJpaEntity`, `Category`, `JpaCategoryRepository` và `ServiceJpaEntity` đều đã được nhóm sẵn trong `com.danasea.backend.modules.service`:
- Thêm các Use Cases vào:
  - `com.danasea.backend.modules.service.application.usecase.category.CreateCategoryUseCase`
  - `com.danasea.backend.modules.service.application.usecase.category.GetCategoryTreeUseCase`
  - `com.danasea.backend.modules.service.application.usecase.category.UpdateCategoryUseCase`
  - `com.danasea.backend.modules.service.application.usecase.category.DeactivateCategoryUseCase`
- Thêm Controllers vào:
  - `com.danasea.backend.modules.service.presentation.CategoryController` (`GET /api/categories`)
  - `com.danasea.backend.modules.service.presentation.AdminCategoryController` (`/api/admin/categories/**`)
- Thêm Exceptions vào:
  - `com.danasea.backend.modules.service.domain.exception.CategoryHasActiveServicesException`
  - `com.danasea.backend.modules.service.domain.exception.SlugAlreadyExistsException`
  - `com.danasea.backend.modules.service.domain.exception.CategoryNotFoundException`
  - `com.danasea.backend.modules.service.domain.exception.CategoryHierarchyLoopException`

### Phương án B: Tách thành module riêng `modules/category`
- Tạo module mới: `com.danasea.backend.modules.category` với đầy đủ 4 tầng: `domain`, `application`, `infrastructure`, `presentation`.
- Di chuyển `Category`, `CategoryJpaEntity`, `JpaCategoryRepository` từ `modules/service` sang `modules/category`.
- Sử dụng Port/Interface để truy vấn `ServiceInternalApi` hoặc `JpaServiceRepository` nhằm kiểm tra active services.

---

## 5. Hướng Dẫn Môi Trường Build & Chạy Unit Tests

- **Java**: Cần Java 21 (Temurin 21 có tại `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`).
- **Lệnh test dự án**:
  ```bash
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
  ./mvnw test -Dtest=CreateCategoryUseCaseTest,GetCategoryTreeUseCaseTest,DeactivateCategoryUseCaseTest
  ```
- **Lưu ý về Sandbox**: Mockito chạy dynamic agent loading yêu cầu quyền OS (`BypassSandbox: true` khi chạy qua tool). Các unit test nghiệp vụ độc lập không cần Docker hay DB thật, chạy hoàn toàn bằng pure Mockito/JUnit 5.
