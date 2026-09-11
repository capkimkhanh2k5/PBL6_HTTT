# BRIEFING — 2026-09-10T10:47:00+07:00

## Mission
Khảo sát toàn diện kiến trúc codebase, Cloudinary config/service, ORM/DB, Service Images entity/model, file upload handler cho R1.

## 🔒 My Identity
- Archetype: explorer
- Roles: investigator, surveyor
- Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_survey_1
- Original parent: 3cf76b9c-3d3a-4b20-bda9-91109ce9df1c
- Milestone: survey_service_assets_cloudinary

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Ghi báo cáo chi tiết vào analysis.md và handoff.md
- Gửi message hoàn tất đến parent qua send_message
- Trả lời bằng tiếng Việt

## Current Parent
- Conversation ID: 3cf76b9c-3d3a-4b20-bda9-91109ce9df1c
- Updated: 2026-09-10T10:47:00+07:00

## Investigation State
- **Explored paths**:
  - `backend/pom.xml`, `Dockerfile`, `docker-compose.yml`
  - `backend/src/main/resources/application.yml`, `application.properties`
  - `backend/docs/Clean_Architecture_Rules.md`, `backend/docs/DANASEA_Database_Design.docx`, `docs/DANASEA.docx`
  - `backend/src/main/java/com/danasea/backend/...`:
    - `modules/service/**`: Domain models (`Service`, `ServiceImage`, `ServiceSafetyDocument`, `Category`), JPA entities, JpaRepositories
    - `modules/vendor/**`: Domain models (`Vendor`, `VendorDocument`), JPA entities, JpaRepositories
    - `modules/account/**`: Domain models (`User`, `Role`), `AccountInternalApi`, `AccountInternalService`, mappers
    - `security/authentication/**`: Ports, use cases (`LoginUseCase`, etc.), filters, token provider
    - `security/authorization/**`: Policy, subjects, ownership port & adapter
    - `config/**`: `SecurityConfig`, `ApplicationBeans`, `OpenApiConfig`
    - `shared/**`: `BaseDomainModel`, `BaseJpaEntity`, `ErrorResponse`
    - `src/test/java/**`: JUnit 5 + Mockito use case test pattern (`LoginUseCaseTest`)
- **Key findings**:
  - Ngôn ngữ: Java 21; Framework: Spring Boot 4.1.1; DB: PostgreSQL 17 qua Spring Data JPA Hibernate update.
  - Cloudinary: CHƯA được tích hợp (chưa có dependency, chưa có config, chưa có code). Cần bổ sung `cloudinary-http44:1.39.0`.
  - Service & ServiceImage model/JPA entity: Đã có sẵn, cần bổ sung query methods vào `JpaServiceImageRepository`.
  - Multipart upload: Dùng Spring WebMVC `MultipartFile` và `StandardServletMultipartResolver`.
  - Kiến trúc: Modular Clean Architecture (Domain -> Application -> Infrastructure / Presentation).
- **Unexplored areas**: None for R1 survey scope.

## Key Decisions Made
- Kiến trúc cho module Service Assets tuân thủ 100% `backend/docs/Clean_Architecture_Rules.md`.
- Trích xuất đầy đủ sơ đồ bảng, luồng upload, giải pháp Cloudinary adapter, danh sách file tạo mới/sửa đổi vào `analysis.md` và `handoff.md`.

## Artifact Index
- analysis.md — Báo cáo khảo sát chi tiết
- handoff.md — Báo cáo bàn giao 5 thành phần
