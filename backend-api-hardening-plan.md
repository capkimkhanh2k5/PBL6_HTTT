# Backend API Hardening and Completion

## Goal
Khắc phục toàn bộ lỗi trong `backend/docs/api-audit-2026-09-23.md`, hoàn thiện các API order/payment còn thiếu bằng adapter thanh toán giả lập an toàn, giữ tương thích route cũ và đưa toàn bộ test backend về trạng thái xanh.

## Assumptions
- `/api/services` là contract canonical; `/api/v1/catalog` tiếp tục hoạt động như alias tương thích.
- Payment dùng provider-neutral adapter và signed/idempotent simulated webhook; không tuyên bố tiền đã hoàn nếu provider chưa xác nhận.
- Chỉ thêm migration Flyway mới; không sửa migration đã phát hành và không phá DTO/route hiện hữu.

## Tasks
- [x] 1. Khóa AI conversation/card theo owner, thay request/response Map/JPA bằng DTO có validation, giới hạn history → Verify: controller/use-case IDOR tests đỏ rồi xanh.
- [x] 2. Sửa auth invariants: verified-email access, OTP null, atomic refresh rotation và test profile/config → Verify: auth/security/rate-limit tests xanh.
- [x] 3. Đồng bộ catalog aliases/security, hoàn thiện response mapper/query validation/wishlist visibility → Verify: anonymous catalog tests và response-field tests xanh.
- [x] 4. Sửa vendor identity cho image/safety document, validate MIME/content/update DTO và parent-child service/doc → Verify: integration tests dùng userId khác vendorId xanh.
- [x] 5. Làm booking confirm/cancel transactional và idempotent; chỉ payment-success mới confirm, khóa capacity/hold race → Verify: concurrent confirm/cancel/payment tests xanh.
- [x] 6. Xây order/payment/refund APIs còn thiếu với state machine, HMAC webhook, transaction idempotency và provider adapter giả lập → Verify: create/list/detail/vendor/payment/webhook/refund controller + integration tests xanh.
- [x] 7. Sửa dispute/weather refund thành REQUESTED/PROCESSING đến khi provider xác nhận; chặn dispute trước trải nghiệm và validate action → Verify: financial-state tests xanh.
- [x] 8. Làm settlement fail-fast, dependency bắt buộc, lock/version, validation/audit; phân trang notification và bounds/sort chung → Verify: settlement concurrency/error tests xanh.
- [x] 9. Sửa weather contract/current location/cache, check-in window/QR uniqueness và các API validation/error-code còn lại → Verify: weather/check-in/controller regression tests xanh.
- [x] 10. Verification cuối: compile, Flyway/DDL, toàn bộ unit/integration/E2E không skip trọng yếu, endpoint inventory/OpenAPI khớp tracking → Verify: `./mvnw test` và package thành công.

## Done When
- [x] Không còn finding ❌/⚠️ chưa xử lý trong báo cáo audit; ngoại lệ test legacy được ghi rõ trong báo cáo kết quả.
- [x] 101 product endpoint production (105 mapping ở test profile, gồm 4 diagnostic route) có authorization, validation, error contract và regression gate.
- [x] Không còn refund giả `PROCESSED`, booking confirm trước payment, IDOR, race refresh/check-in/settlement đã biết; AI confirmation tạo inventory hold thật và được khóa xử lý.
- [x] Full Maven test suite và build đều xanh trên trạng thái worktree hiện tại.

## Verification Result — 2026-09-23

- `./mvnw test`: **BUILD SUCCESS**, 1,412 tests, 0 failures, 0 errors, 135 skipped.
- 135 skipped tests đều thuộc `BaseServiceE2ETest`, là scaffold legacy dùng UUID ngẫu nhiên và assertion permissive; đã được thay thế bởi `ServiceIntegrationE2ETest` cùng controller/use-case/integration tests đang chạy.
- `PostgreSqlMigrationIntegrationTest`: PostgreSQL 16.15 áp dụng và validate đủ 10 migration, schema đạt version 10.
- Endpoint inventory gate: đúng 105 mapping trong test profile; 4 route `/api/authorization/**` chỉ bật ở `dev`/`test`, còn 101 product route trong production.
- `./mvnw -DskipTests package`: **BUILD SUCCESS**, tạo `target/backend-0.0.1-SNAPSHOT.jar`.
