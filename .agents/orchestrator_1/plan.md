# Implementation Plan — Module Categories (Java/Spring Boot)

## Objective
Triển khai trọn vẹn module Categories (Admin quản lý, public đọc) cho dự án Backend Spring Boot theo đúng kiến trúc phân tầng hiện tại, đáp ứng 100% Acceptance Criteria và Unit Tests.

## Steps
1. **Phase 0: Survey & Codebase Exploration**
   - Dispatch 3 Explorers:
     - Explorer 1: Điều tra cấu trúc package, kiến trúc phân tầng (Entity, DTO, Repository, Service/UseCase, Controller, Exception handling) trong thư mục `backend/`.
     - Explorer 2: Điều tra cơ chế xác thực/phân quyền (Admin vs Public security config, JWT / Role checking), cách tổ chức route và database schema/migrations (Flyway, Liquibase, JPA ddl-auto).
     - Explorer 3: Điều tra môi trường build & test (Maven vs Gradle, test framework JUnit 5, Mockito, Spring Boot Test, cách chạy test và các dependencies hiện có).
   - Tổng hợp báo cáo survey thành `PROJECT.md` tại root dự án.

2. **Phase 1: Milestone Decomposition & Track Setup**
   - Xác định rõ các milestone theo kiến trúc:
     - M1: Entity, DTO, Exceptions, Repository (Category entity, CategoryRepository, custom exceptions như `CategoryHasActiveServicesException`, `SlugAlreadyExistsException`).
     - M2: UseCases & Business Logic (`CreateCategoryUseCase`, `GetCategoryTreeUseCase`, `DeactivateCategoryUseCase`, chu trình chống vòng lặp, kiểm tra active services).
     - M3: Controllers & API Endpoints (`CategoryController`, `AdminCategoryController`, validation, serialization, security annotation).
     - M4: Unit Tests Suite (Tất cả 8 test cases được yêu cầu trong ORIGINAL_REQUEST.md).
   - Cập nhật interface contracts và code layout trong `PROJECT.md`.

3. **Phase 2: Execution & Verification Loop**
   - Dispatch Worker để implement từng milestone.
   - Dispatch Reviewers để thẩm định code quality, clean code, và interface conformance.
   - Dispatch Challengers để test adversarial edge cases (loop detection, recursive tree, deactivation constraints).
   - Dispatch Forensic Auditor để kiểm tra tính toàn vẹn (không hardcode test, không dummy).
   - Thu thập kết quả vào `GATE_STATUS.md`.

4. **Phase 3: Final Completion & Victory Report**
   - Chạy toàn bộ test suite.
   - Báo cáo kết quả và handoff cho Sentinel.
