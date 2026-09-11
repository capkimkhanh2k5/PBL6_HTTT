# HANDOFF REPORT: Khảo sát Codebase về Cloudinary & Service Images (R1)

**Agent:** Explorer Survey 1 (`teamwork_preview_explorer_survey_1`)  
**Mục tiêu:** Khảo sát kiến trúc codebase, cấu hình Cloudinary, ORM/Database, Service Images entity/model, file upload handler, và các file liên quan theo DISPATCH.md và ORIGINAL_REQUEST.md.  
**Ngày bàn giao:** 2026-09-10  

---

## 1. Observation (Các phát hiện thực tế trực tiếp)

1. **Tech Stack & Build System:**
   - File `backend/pom.xml`: Khai báo Spring Boot parent `4.1.1`, Java `21`, các dependencies:
     - `spring-boot-starter-webmvc` (Line 71)
     - `spring-boot-starter-data-jpa` (Line 59)
     - `spring-boot-starter-security` (Line 63)
     - `spring-boot-starter-validation` (Line 67)
     - `postgresql` (Line 76)
     - `lombok` (Line 81)
     - `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (Line 123-140)
     - `springdoc-openapi-starter-webmvc-ui` (Line 143)
     - `spring-boot-starter-data-redis` (Line 149)
     - `spring-boot-starter-amqp` (Line 153)
     - `spring-boot-starter-mail` (Line 158)
     - `bucket4j-core`, `bucket4j-redis` (Line 164-173)
     - `spring-boot-starter-test` (Line 117)
   - Hoàn toàn **KHÔNG CÓ** dependency Cloudinary (ví dụ `com.cloudinary:cloudinary-http44`) trong `backend/pom.xml`.

2. **Cấu hình ứng dụng:**
   - File `backend/src/main/resources/application.yml`:
     - Hibernate cấu hình `spring.jpa.hibernate.ddl-auto: update` (Line 16).
     - Chưa có cấu hình `spring.servlet.multipart.*`.
     - Hoàn toàn **KHÔNG CÓ** thuộc tính `cloudinary.*`.
   - File `.env.example`: Chỉ có `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `FRONTEND_PORT`. Không có biến môi trường Cloudinary.

3. **Cấu trúc Entities & Models đã có:**
   - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Service.java` và `ServiceJpaEntity.java` (`@Table(name = "services")`): Có đầy đủ các trường `id`, `vendorId`, `categoryId`, `name`, `slug`, `price`, `status`, `weatherSensitive`, `waiverContent`, v.v.
   - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceImage.java` và `ServiceImageJpaEntity.java` (`@Table(name = "service_images")`):
     - `serviceId`: UUID
     - `url`: String
     - `sortOrder`: Short
     - Kế thừa `BaseJpaEntity` (`id`, `createdAt`, `updatedAt`).
   - `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/repositories/JpaServiceImageRepository.java`: Đã có interface kế thừa `JpaRepository<ServiceImageJpaEntity, UUID>`, nhưng chưa có query methods tùy biến.
   - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/Category.java` và `CategoryJpaEntity.java` (`@Table(name = "categorys")`): Đang có `name`, `nameEn`, `slug`, `parentId`, `iconUrl`, `isActive`. Chưa có cờ `requiresSafetyCert`.

4. **Kiến trúc Clean Architecture:**
   - File quy chuẩn: `backend/docs/Clean_Architecture_Rules.md`.
   - Các use case hiện tại (`backend/src/main/java/com/danasea/backend/security/authentication/application/usecase/*`) được viết dưới dạng Plain Java Class độc lập, không gắn annotation Spring Framework (`@Service`), được khởi tạo và wire dependencies thông qua Configuration bean (`ApplicationBeans.java`).
   - Module `modules/service` hiện mới chỉ có layer `domain/models` và `infrastructure/persistence/{entities, repositories}`. Chưa có `application`, `presentation`, `storage adapter`, hay `exceptions`.

5. **Mô hình tài liệu an toàn tham khảo:**
   - `backend/src/main/java/com/danasea/backend/modules/vendor/domain/models/VendorDocument.java` và `VendorDocumentJpaEntity.java`: Có `vendorId`, `docType`, `fileUrl`, `status` (`DocStatus`), `reviewedBy`, `reviewedAt`.
   - `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceSafetyDocument.java` và `ServiceSafetyDocumentJpaEntity.java`: Có `serviceId`, `fileUrl`, `status`, `reviewedBy`, `reviewedAt`.

---

## 2. Logic Chain (Chuỗi suy luận từng bước)

1. **Từ Observation 1 & 2:**
   - Yêu cầu ban đầu nói "kế thừa cấu hình Cloudinary có sẵn", nhưng qua kiểm tra mã nguồn, `pom.xml`, `application.yml`, và `.env.example`, Cloudinary hoàn toàn chưa từng được cài đặt trong repo.
   - *Suy luận:* Cần thêm dependency Maven `com.cloudinary:cloudinary-http44:1.39.0`, thêm cấu hình properties vào `application.yml` và biến môi trường vào `.env.example`.
2. **Từ Observation 3:**
   - Các bảng và entity `services` và `service_images` đã có sẵn và tuân thủ đúng tài liệu `DANASEA_Database_Design.docx`.
   - *Suy luận:* Không cần tạo mới Entity/Model cho `ServiceImage`, chỉ cần bổ sung các phương thức truy vấn cần thiết vào `JpaServiceImageRepository` (đếm số ảnh, tìm theo `serviceId` sắp xếp theo `sortOrder`, tìm `maxSortOrder`, tìm theo `id` và `serviceId`, xóa theo `id` và `serviceId`).
3. **Từ Observation 4:**
   - Codebase tuân thủ nghiêm ngặt Clean Architecture. Use case không được phụ thuộc trực tiếp vào SDK bên ngoài (Cloudinary SDK) hay Servlet API (`MultipartFile`).
   - *Suy luận:*
     - Cần tạo interface port `ImageStoragePort` tại Application layer nhận dữ liệu nguyên thủy/DTO (`byte[]`, `String filename`, `String folder`).
     - Tạo `CloudinaryStorageAdapter` tại Infrastructure layer implements port này.
     - Cách làm này cho phép viết Unit Test (`UploadServiceImageUseCaseTest`) với Mockito mà không phụ thuộc vào kết nối mạng bên ngoài.
4. **Từ Observation 3 & 4 về bảo mật/ownership:**
   - API upload/xóa/sắp xếp ảnh là API dành cho Vendor (`/api/vendor/services/{id}/images/**`).
   - *Suy luận:* Cần kiểm tra quyền sở hữu service: người dùng đăng nhập phải có role `VENDOR` (hoặc `ADMIN`), và `service.vendorId` phải khớp với `vendor.id` của user. Nếu không khớp trả về 403 Forbidden. Nếu service không tìm thấy trả về 404 Not Found.
5. **Từ Observation 3 về R1 Reorder API:**
   - Khi đổi thứ tự hàng loạt qua `PATCH /api/vendor/services/{id}/images/reorder`:
   - *Suy luận:* Phải kiểm tra tất cả các `imageId` gửi lên có thực sự thuộc về `serviceId` đó hay không. Nếu có bất kỳ `imageId` nào thuộc service khác hoặc không tồn tại, phải chặn đứng ngay và trả về 403 hoặc 404 để chống tấn công IDOR / thao túng dữ liệu trái phép.

---

## 3. Caveats (Các giả định & Vùng lưu ý)

1. **Giả định về Cloudinary SDK:**
   - Bản `cloudinary-http44:1.39.0` tương thích tốt với Java 21 và Spring Boot.
   - Khi chạy unit test trong CI/CD hoặc môi trường cục bộ, `ImageStoragePort` sẽ được mock nên không cần có API key thật vẫn test được 100% logic nghiệp vụ.
2. **Giới hạn số ảnh tối đa:**
   - Giả định giới hạn tối đa cho mỗi dịch vụ là 10 ảnh (`MAX_IMAGES_PER_SERVICE = 10`), phù hợp với quy mô trải nghiệm du lịch biển. Có thể đưa vào constant hoặc cấu hình.
3. **Cờ `requires_safety_cert` (R3):**
   - Cờ này thuộc phạm vi R3 nhưng nằm trên bảng `categorys`. Việc thêm trường `requiresSafetyCert` vào `Category` và `CategoryJpaEntity` cần được đồng bộ khi triển khai R3.

---

## 4. Conclusion (Đánh giá kết luận & Phạm vi công việc)

1. Codebase đã sẵn sàng để phát triển module Service Images (R1) theo đúng kiến trúc Clean Architecture.
2. Các bước thực hiện chính cho R1 gồm:
   - Bổ sung dependency Cloudinary và cấu hình liên quan.
   - Tạo `ImageStoragePort` và `CloudinaryStorageAdapter`.
   - Bổ sung query methods vào `JpaServiceImageRepository`.
   - Tạo 3 Use Case: `UploadServiceImageUseCase`, `DeleteServiceImageUseCase`, `ReorderServiceImagesUseCase`.
   - Tạo Controller `VendorServiceImageController` với 3 endpoint RESTful.
   - Tạo Unit Test: `UploadServiceImageUseCaseTest` và `ReorderServiceImagesUseCaseTest` bao phủ toàn bộ các case yêu cầu (upload tự tăng sort_order, chặn vượt max ảnh, chặn file sai định dạng, đổi thứ tự thành công, chặn đổi thứ tự ảnh của service khác).

---

## 5. Verification Method (Phương pháp kiểm chứng độc lập)

1. **Kiểm tra sự tồn tại của các file cấu trúc:**
   - Xem file chi tiết tại `.agents/teamwork_preview_explorer_survey_1/analysis.md`.
2. **Kiểm tra các model hiện có:**
   - Inspect: `backend/src/main/java/com/danasea/backend/modules/service/domain/models/ServiceImage.java`
   - Inspect: `backend/src/main/java/com/danasea/backend/modules/service/infrastructure/persistence/entities/ServiceImageJpaEntity.java`
3. **Kiểm tra Clean Architecture Rules:**
   - Inspect: `backend/docs/Clean_Architecture_Rules.md`
4. **Kiểm chứng test runner dự án:**
   - Chạy lệnh test sau khi implement:
     `./mvnw test -Dtest=UploadServiceImageUseCaseTest,ReorderServiceImagesUseCaseTest`
     hoặc chạy toàn bộ test: `./mvnw test`
