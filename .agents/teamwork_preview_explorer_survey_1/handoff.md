# BÁO CÁO KHẢO SÁT KIẾN TRÚC & MÔ HÌNH DỮ LIỆU (EXPLORER 1 HANDOFF)

## 1. Observation (Quan sát thực tế trong mã nguồn)

Dưới đây là các dữ liệu thực tế được kiểm chứng trực tiếp từ codebase:

### 1.1. Build System, Phiên bản & Dependencies
- **Build System**: Apache Maven với wrapper `./mvnw` và `./mvnw.cmd`.
- **File cấu hình**: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/backend/pom.xml`.
  - Spring Boot Starter Parent: version `4.1.1` (dòng 8).
  - Java version: `21` (dòng 32: `<java.version>21</java.version>`).
  - PostgreSQL Driver: `42.7.13` (dòng 34, 75-78).
  - Dockerfile (`backend/Dockerfile`): `FROM maven:3.9.16-eclipse-temurin-21-alpine` và runtime `eclipse-temurin:21-jre-jammy`.
- **Dependencies chính**:
  - `spring-boot-starter-webmvc` (dòng 70-72).
  - `spring-boot-starter-data-jpa` (dòng 58-60).
  - `spring-boot-starter-security` (dòng 62-64).
  - `spring-boot-starter-validation` (dòng 66-68).
  - `spring-boot-starter-data-redis` (dòng 149-151) & `bucket4j-redis` (dòng 169-172).
  - `spring-boot-starter-amqp` (RabbitMQ, dòng 153-155).
  - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` version `0.13.0` (dòng 122-139).
  - `springdoc-openapi-starter-webmvc-ui` version `3.1.1` (dòng 142-145).
  - Test: `spring-boot-starter-test`, `testcontainers-junit-jupiter` (2.0.5), `testcontainers-redis` (2.2.4).
- **Công cụ di chuyển database (Database Migration Tool)**:
  - KHÔNG có Flyway (`flyway-core`) hay Liquibase trong `pom.xml`.
  - Thư mục `backend/src/main/resources` chỉ chứa `application.properties` và `application.yml`, không có thư mục `db/migration` hay changelog.
  - Trong `backend/src/main/resources/application.yml` (dòng 14-17):
    ```yaml
    jpa:
      hibernate:
        ddl-auto: update
      open-in-view: false
    ```
    Hệ thống đang sử dụng cơ chế tự động đồng bộ schema của Hibernate (`ddl-auto: update`).
  - Database Dialect: PostgreSQL (`driver-class-name: org.postgresql.Driver`), trong `docker-compose.yml` sử dụng image `postgres:17-alpine` (dòng 3).

### 1.2. Cấu trúc Package & Kiến trúc tổng thể
- Cấu trúc dự án tuân thủ nghiêm ngặt **Modular Clean Architecture** theo tài liệu `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/backend/docs/Clean_Architecture_Rules.md`:
  - `com.danasea.backend`:
    - `config/`: `ApplicationBeans`, `OpenApiConfig`, `RabbitMQConfig`, `SecurityConfig`.
    - `security/`: `authentication`, `authorization`.
    - `shared/`: `core/domain/models/BaseDomainModel.java`, `core/infrastructure/persistence/entities/BaseJpaEntity.java`, `presentation/ErrorResponse.java`.
    - `modules/`: `account`, `ai`, `communication`, `operation`, `order`, `service`, `systemconfig`, `vendor`, `weather`.
- Module trọng tâm cần triển khai: `com.danasea.backend.modules.service`.

### 1.3. Các Domain Entity, Model & Repository hiện có trong `modules/service`
Tất cả các domain model kế thừa từ `BaseDomainModel` (`id: UUID`, `createdAt: OffsetDateTime`, `updatedAt: OffsetDateTime`).
Tất cả các JPA entity kế thừa từ `BaseJpaEntity` (`@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;`, `@CreationTimestamp createdAt`, `@UpdateTimestamp updatedAt`).

1. **`Service` & `ServiceJpaEntity`**:
   - Model: `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Service.java`
   - Entity: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceJpaEntity.java` (`@Table(name = "services")`)
   - Thuộc tính:
     - `UUID vendorId`
     - `UUID categoryId`
     - `String name`, `nameEn`, `slug`
     - `String description`, `descriptionEn`
     - `BigDecimal price`
     - `Integer durationMinutes`, `capacityPerSlot`
     - `String locationName`, `address`
     - `BigDecimal latitude`, `longitude`
     - `ServiceStatus status` (`@Enumerated(EnumType.STRING)`)
     - `String waiverContent`, `Boolean weatherSensitive`
     - `BigDecimal minWindKmh`, `maxWaveM`
     - `BigDecimal avgRating`, `Integer ratingCount`
     - `Integer viewCount`
2. **`Category` & `CategoryJpaEntity`**:
   - Model: `modules/service/domain/models/Category.java`
   - Entity: `modules/service/infrastructure/persistence/entities/CategoryJpaEntity.java` (`@Table(name = "categorys")`)
   - Thuộc tính: `name`, `nameEn`, `slug`, `UUID parentId`, `iconUrl`, `Boolean isActive`.
3. **`Wishlist` & `WishlistJpaEntity`**:
   - Model: `modules/service/domain/models/Wishlist.java`
   - Entity: `modules/service/infrastructure/persistence/entities/WishlistJpaEntity.java` (`@Table(name = "wishlists")`)
   - Thuộc tính: `UUID userId`, `UUID serviceId`.
4. **`RecentlyViewed` & `RecentlyViewedJpaEntity`**:
   - Model: `modules/service/domain/models/RecentlyViewed.java`
   - Entity: `modules/service/infrastructure/persistence/entities/RecentlyViewedJpaEntity.java` (`@Table(name = "recently_vieweds")`)
   - Thuộc tính: `UUID userId`, `String sessionId`, `UUID serviceId`, `OffsetDateTime viewedAt`.
5. **Các model/entity liên quan khác**:
   - `ServiceImage` / `ServiceImageJpaEntity` (`@Table(name = "service_images")`): `serviceId`, `url`, `sortOrder`.
   - `ServiceSlot` / `ServiceSlotJpaEntity` (`@Table(name = "service_slots")`): `serviceId`, `date`, `startTime`, `endTime`, `capacity`, `bookedCount`, `status: SlotStatus`.
   - `ServiceSafetyDocument` / `ServiceSafetyDocumentJpaEntity` (`@Table(name = "service_safety_documents")`).
6. **Các Repositories hiện có**:
   - `JpaServiceRepository extends JpaRepository<ServiceJpaEntity, UUID>`
   - `JpaCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID>`
   - `JpaWishlistRepository extends JpaRepository<WishlistJpaEntity, UUID>`
   - `JpaRecentlyViewedRepository extends JpaRepository<RecentlyViewedJpaEntity, UUID>`
   - `JpaServiceImageRepository`, `JpaServiceSlotRepository`, `JpaServiceSafetyDocumentRepository`.
   Tất cả hiện là các interface rỗng chưa có custom query.

### 1.4. Trạng thái Enum (Status Enums)
- File: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceStatus.java`:
  ```java
  package com.danasea.backend.modules.service.domain.models;

  public enum ServiceStatus {
      DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, PAUSED
  }
  ```
  -> **XÁC NHẬN: Giá trị `PUBLISHED` đã được định nghĩa sẵn trong `ServiceStatus`!**
- `SlotStatus`: `AVAILABLE, BOOKED, CANCELLED, BLOCKED`.
- `DocStatus`: `PENDING, APPROVED, REJECTED`.

### 1.5. Cấu hình Bảo mật (SecurityConfig & Authentication)
- File: `/Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_public_catalog_module/backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`:
  - Hiện tại chỉ mở public cho: `/api/auth/**`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`.
  - Mọi request khác đều yêu cầu `.authenticated()`.
  - Cần cập nhật `SecurityConfig` để cho phép `GET /api/services/**` và `GET /api/recently-viewed` là `permitAll()`.
  - Các endpoints Wishlist (`/api/wishlists/**`) yêu cầu đăng nhập và sẽ được bảo vệ bởi `.anyRequest().authenticated()`.
- Trong `JwtAuthenticationFilter`: Principal được set là email (`subject.email()`). Email này có thể map sang `User` và `userId: UUID` qua `AccountInternalApi.findUserByEmail(email)`.

---

## 2. Logic Chain (Lập luận & Phân tích kỹ thuật)

### 2.1. Về cơ chế tăng `view_count` và xử lý Concurrency an toàn (Atomic Increment)
- **Vấn đề quan sát**:
  - Yêu cầu R1 quy định: `GET /api/services/{id}` phải tăng `view_count`.
  - Acceptance Criteria quy định: Phải xử lý concurrent view count increments an toàn, có test chứng minh `race conditions on view_count`.
  - Nếu đọc entity lên bộ nhớ rồi gọi `service.setViewCount(service.getViewCount() + 1)` rồi `repository.save(service)`, khi nhiều request đồng thời truy cập cùng 1 service sẽ xảy ra lỗi **Lost Update** (ví dụ 100 requests đồng thời chỉ làm tăng count lên 1 hoặc 2), hoặc Optimistic Lock Exception nếu có `@Version`.
- **Giải pháp tối ưu**:
  - Thực hiện câu lệnh **Atomic UPDATE trực tiếp ở mức Database Engine (SQL/JPQL)**:
    ```java
    @Modifying
    @Query("UPDATE ServiceJpaEntity s SET s.viewCount = COALESCE(s.viewCount, 0) + 1 WHERE s.id = :id AND s.status = :status")
    int incrementViewCount(@Param("id") UUID id, @Param("status") ServiceStatus status);
    ```
  - **Lợi ích**:
    1. Cơ chế Row-level Lock của PostgreSQL sẽ tự động serialize các câu lệnh `UPDATE ... SET view_count = view_count + 1` cho cùng một bản ghi, đảm bảo tính nguyên tử (Atomicity) 100%, không bị mất lượt đếm nào.
    2. Không phát sinh chi phí đọc/ghi toàn bộ thực thể.
    3. Trả về số dòng bị ảnh hưởng (`int`), nếu trả về `0` nghĩa là service không tồn tại hoặc không ở trạng thái `PUBLISHED` -> lập tức quăng `ServiceNotFoundException` (HTTP 404).

### 2.2. Về cơ chế Tìm kiếm & Lọc Dịch vụ (Public Catalog Search)
- **Vấn đề quan sát**:
  - Endpoint `GET /api/services` yêu cầu lọc theo `category`, `keyword`, `location (lat/lng/radius)`, `price range (minPrice, maxPrice)`.
  - Chỉ được trả về service có `status = PUBLISHED`.
  - Image PostgreSQL trong Docker Compose là `postgres:17-alpine` chuẩn (không có extension PostGIS).
  - Các trường `latitude`, `longitude`, `price` trong `ServiceJpaEntity` là kiểu `BigDecimal`.
- **Giải pháp kỹ thuật**:
  - Sử dụng **Spring Data JPA Specification** (`JpaSpecificationExecutor<ServiceJpaEntity>`) kết hợp `Pageable` để xây dựng dynamic query an toàn, mềm dẻo.
  - Về tính khoảng cách địa lý (lat/lng/radius km):
    - Áp dụng công thức Haversine hoặc Bounding Box (hộp giới hạn vĩ độ/kinh độ) trực tiếp trong SQL / CriteriaBuilder / Specification:
      - Vĩ độ biến thiên: `deltaLat = radiusKm / 111.0`
      - Kinh độ biến thiên: `deltaLng = radiusKm / (111.0 * cos(radians(centerLat)))`
      - Lọc thô bằng Bounding Box `latitude BETWEEN (lat - deltaLat) AND (lat + deltaLat)` và `longitude BETWEEN (lng - deltaLng) AND (lng + deltaLng)`.
      - Lọc chính xác bằng công thức khoảng cách Haversine chuẩn:
        `6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(s.latitude))) <= :radiusKm`
  - Đảm bảo predicate luôn gắn kèm điều kiện bắt buộc: `builder.equal(root.get("status"), ServiceStatus.PUBLISHED)`.

### 2.3. Về cơ chế Lịch sử Xem gần đây (Recently Viewed History & Upsert)
- **Vấn đề quan sát**:
  - Yêu cầu R1 & R3: Xử lý cả người dùng đã đăng nhập (`user_id`) và khách vãng lai (`session_id`).
  - Cập nhật thời gian xem (`viewedAt`) mà không được tạo trùng lặp bản ghi (no duplicate records).
- **Giải pháp kỹ thuật**:
  - Định nghĩa Unique Constraint logic trên bảng `recently_vieweds`:
    - Với user đã đăng nhập: Khóa logic là `(userId, serviceId)`.
    - Với guest: Khóa logic là `(sessionId, serviceId)`.
  - Khi ghi nhận lượt xem (`recordRecentlyViewed`):
    - Nếu `userId != null`: Tìm bản ghi theo `(userId, serviceId)`. Nếu có, cập nhật `viewedAt = OffsetDateTime.now()`. Nếu chưa có, tạo mới `RecentlyViewedJpaEntity` với `userId`, `sessionId = null`, `serviceId`, `viewedAt`.
    - Nếu `userId == null` và `sessionId != null`: Tìm bản ghi theo `(sessionId, serviceId)`. Nếu có, cập nhật `viewedAt = OffsetDateTime.now()`. Nếu chưa có, tạo mới với `userId = null`, `sessionId`, `serviceId`, `viewedAt`.
  - Khi lấy danh sách (`GET /api/recently-viewed`):
    - Đọc theo `userId` (nếu có token) hoặc `sessionId` (nếu gửi header `X-Session-Id` hoặc query param), sắp xếp `viewedAt DESC`.
    - Join/map sang DTO kèm thông tin tóm tắt của Service để client hiển thị card.

### 2.4. Về Danh sách Yêu thích (Wishlist & Idempotency)
- **Vấn đề quan sát**:
  - Yêu cầu R2: `POST /api/wishlists/{serviceId}`, `DELETE /api/wishlists/{serviceId}`, `GET /api/wishlists`.
  - Acceptance Criteria: Xử lý thêm trùng (duplicate additions) và xóa không tồn tại (non-existent service removals) một cách êm ái (gracefully/idempotent).
- **Giải pháp kỹ thuật**:
  - `POST /api/wishlists/{serviceId}`:
    1. Kiểm tra service có tồn tại và `PUBLISHED` hay không (nếu không -> 404).
    2. Kiểm tra `existsByUserIdAndServiceId(userId, serviceId)`. Nếu đã tồn tại -> trả về thành công (idempotent, 200 OK / 201 Created). Nếu chưa có -> lưu mới.
  - `DELETE /api/wishlists/{serviceId}`:
    1. Thực hiện xóa theo `userId` và `serviceId`. Nếu không tồn tại -> vẫn trả về 200 OK hoặc 204 No Content mà không báo lỗi (idempotent).
  - `GET /api/wishlists`:
    - Lấy danh sách service trong wishlist của user kèm thông tin Service để hiển thị.

---

## 3. Caveats (Các điểm cần lưu ý & Giới hạn)

1. **Cấu hình Spring Security**:
   - Hiện tại `/api/services/**` đang bị chặn mặc định bởi `.anyRequest().authenticated()`. Bắt buộc phải thêm `permitAll()` cho các public catalog endpoints trong `SecurityConfig.java`.
2. **PostgreSQL Extension**:
   - Môi trường chạy PostgreSQL chuẩn không cài đặt PostGIS. Do đó, việc tìm kiếm theo bán kính cần dùng công thức tính toán toán học (Haversine/Bounding Box) thay vì các hàm `ST_DWithin` của PostGIS.
3. **Cơ chế Migration**:
   - Dự án chưa tích hợp Flyway/Liquibase mà đang dùng `ddl-auto: update`. Mọi chỉ mục (indexes) và ràng buộc (unique constraints) cần được khai báo rõ ràng trong annotations `@Table(uniqueConstraints = ..., indexes = ...)` của JPA Entity để Hibernate sinh chuẩn xác vào database.
4. **Mappers trong `modules/service`**:
   - Hiện tại `modules/service` chưa có thư mục `mapper/` (trong khi `modules/account` đã có mẫu `UserMapper`). Cần tạo `ServiceMapper`, `CategoryMapper`, `WishlistMapper`, `RecentlyViewedMapper` theo chuẩn Spring Component.
5. **Môi trường Sandbox**:
   - Lệnh `mvn` không tìm thấy Java Runtime trực tiếp trong môi trường sandbox mặc định. Tuy nhiên, các unit tests (`*UseCaseTest`, `CatalogControllerTest`) hoàn toàn độc lập, sử dụng Mockito và Spring WebMvc test tiêu chuẩn, không phụ thuộc vào container ngoài.

---

## 4. Conclusion (Kết luận & Đề xuất Thiết kế Chi tiết)

### 4.1. Kiến trúc Đề xuất cho Module Service (Modular Clean Architecture)
Cấu trúc các package và lớp cần triển khai cho module `modules/service`:

```
backend/src/main/java/com/danasea/backend/modules/service/
├── domain/
│   ├── models/
│   │   ├── Service.java (đã có)
│   │   ├── Category.java (đã có)
│   │   ├── Wishlist.java (đã có)
│   │   ├── RecentlyViewed.java (đã có)
│   │   └── ServiceStatus.java (đã có)
│   ├── exception/
│   │   └── ServiceNotFoundException.java (mới: kế thừa RuntimeException)
│   └── port/
│       ├── ServiceRepositoryPort.java (mới: các phương thức tìm kiếm, detail, atomic increment)
│       ├── WishlistRepositoryPort.java (mới: add, remove, get)
│       └── RecentlyViewedRepositoryPort.java (mới: record view, get views)
├── application/
│   ├── usecase/
│   │   ├── SearchServicesUseCase.java (mới)
│   │   ├── GetServiceDetailUseCase.java (mới)
│   │   ├── RecordRecentlyViewedUseCase.java (mới)
│   │   ├── WishlistUseCase.java (mới)
│   │   └── GetRecentlyViewedUseCase.java (mới)
│   └── dto/
│       ├── SearchServicesCriteria.java (mới)
│       ├── ServiceSummaryResult.java (mới)
│       └── ServiceDetailResult.java (mới)
├── infrastructure/
│   ├── persistence/
│   │   ├── entities/ (ServiceJpaEntity, WishlistJpaEntity, RecentlyViewedJpaEntity... đã có)
│   │   ├── repositories/
│   │   │   ├── JpaServiceRepository.java (bổ sung atomic query & JpaSpecificationExecutor)
│   │   │   ├── JpaCategoryRepository.java
│   │   │   ├── JpaWishlistRepository.java (bổ sung existsBy, deleteBy, findBy)
│   │   │   └── JpaRecentlyViewedRepository.java (bổ sung findByUserId, findBySessionId)
│   │   └── adapter/
│   │       ├── ServiceRepositoryAdapter.java (mới: implements ServiceRepositoryPort)
│   │       ├── WishlistRepositoryAdapter.java (mới: implements WishlistRepositoryPort)
│   │       └── RecentlyViewedRepositoryAdapter.java (mới: implements RecentlyViewedRepositoryPort)
│   └── mapper/
│       ├── ServiceMapper.java (mới)
│       ├── WishlistMapper.java (mới)
│       └── RecentlyViewedMapper.java (mới)
└── presentation/
    ├── CatalogController.java (mới: GET /api/services, GET /api/services/{id})
    ├── WishlistController.java (mới: POST, DELETE, GET /api/wishlists)
    ├── RecentlyViewedController.java (mới: GET /api/recently-viewed)
    ├── CatalogExceptionHandler.java (mới: bắt ServiceNotFoundException -> 404)
    └── dto/
        ├── ServiceSearchResponse.java
        ├── ServiceDetailResponse.java
        ├── WishlistResponse.java
        └── RecentlyViewedResponse.java
```

### 4.2. Danh sách Test Suites cần xây dựng (R4 Compliance)
1. `SearchServicesUseCaseTest`: Test tìm kiếm với các tiêu chí lọc (category, keyword, price range, vị trí bán kính, luôn lọc status = PUBLISHED, phân trang).
2. `GetServiceDetailUseCaseTest`: Test lấy chi tiết dịch vụ, xác nhận tăng viewCount an toàn, kiểm tra ném 404 ServiceNotFoundException khi service là DRAFT / REJECTED / không tồn tại, xác nhận kích hoạt record recently viewed.
3. `RecordRecentlyViewedUseCaseTest`: Test upsert cho user_id và session_id, kiểm tra cập nhật timestamp `viewedAt`, đảm bảo không tạo duplicate bản ghi.
4. `WishlistUseCaseTest`: Test thêm vào wishlist idempotent (thêm lại cùng service không lỗi), xóa idempotent (xóa service không có không lỗi), lấy danh sách wishlist của user.
5. `CatalogControllerTest`: MockMvc tests xác nhận các endpoint `GET /api/services` và `GET /api/services/{id}` là public; các endpoint `/api/wishlists/**` yêu cầu xác thực; test các status code (200, 404, 400).

---

## 5. Verification Method (Phương pháp Kiểm chứng Độc lập)

Người nhận handoff có thể kiểm chứng độc lập các kết quả khảo sát bằng các bước sau:

1. **Kiểm tra File và Cấu trúc Mã nguồn**:
   - Kiểm tra `ServiceStatus.java`:
     `cat backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceStatus.java`
     -> Thấy `PUBLISHED` đã được định nghĩa.
   - Kiểm tra các Entity:
     `cat backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceJpaEntity.java`
     -> Thấy các trường `vendorId`, `categoryId`, `price`, `latitude`, `longitude`, `status`, `viewCount`.
   - Kiểm tra `SecurityConfig.java`:
     `cat backend/src/main/java/com/danasea/backend/config/SecurityConfig.java`
     -> Thấy hiện tại chưa mở `/api/services/**`.
2. **Kiểm tra Dependencies & Dialect**:
   - Kiểm tra `pom.xml`: xác nhận Java 21, PostgreSQL driver, không có Flyway/Liquibase.
   - Kiểm tra `application.yml`: xác nhận `ddl-auto: update`, driver PostgreSQL.
3. **Điều kiện Vô hiệu hóa (Invalidation Conditions)**:
   - Nếu trong tương lai dự án cài đặt Flyway hoặc Liquibase, chiến lược migration phải chuyển từ `ddl-auto: update` sang migration script SQL.
   - Nếu dự án nâng cấp PostgreSQL container lên image có cài PostGIS (e.g., `postgis/postgis`), có thể thay thế công thức Haversine bằng `ST_DWithin` và kiểu `Point`.
