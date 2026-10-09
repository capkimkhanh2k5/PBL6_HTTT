# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE AUTHENTICATION & IAM

> Module IAM & Authentication chịu trách nhiệm quản lý định danh người dùng, xác thực đăng nhập (Local & Google OAuth2 SSO), cấp phát/thu hồi JWT (Access/Refresh Token) và xử lý quy trình phục hồi tài khoản (Quên mật khẩu / Đặt lại mật khẩu) an toàn với nhiều lớp phòng vệ (Anti-Enumeration, Brute-force Protection, Rate Limiting).

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Google_OAuth2_Login_Flow` | Sequence Diagram | Quy trình xác thực Google ID Token, kiểm tra tài khoản hoặc tự động đăng ký (Auto-Provisioning) và phát hành cặp Access/Refresh Token Danasea |
| 2 | `Auth_ForgotPassword_Flow` | Sequence Diagram | Quy trình yêu cầu cấp mã OTP quên mật khẩu: Giới hạn tần suất IP (Rate Limiting), phòng vệ chống lộ người dùng (Anti-Enumeration Defense), sinh mã OTP băm SHA-256 (hết hạn 15 phút), phát sự kiện RabbitMQ và gửi email nền song ngữ qua SMTP |
| 3 | `Auth_ResetPassword_Flow` | Sequence Diagram | Quy trình đặt lại mật khẩu mới: Khóa bi quan (Pessimistic Lock), phòng thủ chống tấn công Brute-force OTP (giới hạn 5 lần thử), cập nhật mật khẩu mã hóa BCrypt, tăng session version, thu hồi toàn bộ Refresh Token của các phiên cũ và ghi nhận Audit Log |

---

## 1. Google_OAuth2_Login_Flow.png — Luồng Đăng Nhập & Tự Động Đăng Ký Với Google

**Luồng nghiệp vụ chi tiết:**

1. **Khách hàng thao tác trên Giao diện (Client Tier):**
   - Người dùng bấm nút *"Đăng nhập với Google"* hoặc sử dụng Google One Tap.
   - Thư viện Google Identity Services (GSI) mở popup xác thực tài khoản Google.
   - Khi đăng nhập thành công, Google trả về `credential` (Google ID Token dạng JWT) cho React Frontend.

2. **Frontend gửi yêu cầu lên Backend:**
   - Frontend gửi `POST /api/v1/auth/google` kèm payload `{ idToken: "..." }`.
   - `AuthenticationController` tiếp nhận và chuyển giao cho `GoogleOAuth2LoginUseCase`.

3. **Xác thực bảo mật với Google Token Verifier Adapter:**
   - `GoogleTokenVerifierAdapter` sử dụng `GoogleIdTokenVerifier` chính thức của Google Client API.
   - Kiểm tra toàn vẹn chữ ký số cryptographic, kiểm tra `audience` khớp với `GOOGLE_CLIENT_ID` của Danasea, và đảm bảo token chưa hết hạn (`exp`).
   - Trích xuất thông tin người dùng: `googleId (sub)`, `email`, `fullName (name)`, `avatarUrl (picture)`.

4. **Tra cứu tài khoản & Tự động tạo mới (Auto-Provisioning):**
   - Tìm kiếm người dùng qua `JpaUserRepository.findByEmail(email)`.
   - **Trường hợp tài khoản đã tồn tại:** Nạp thông tin tài khoản hiện có, bảo lưu quyền hạn và vai trò hiện tại.
   - **Trường hợp tài khoản chưa tồn tại:** Tự động khởi tạo tài khoản mới với:
     - Vai trò mặc định: `Role = CUSTOMER`.
     - Trạng thái kích hoạt: `status = ACTIVE`.
     - Nhà cung cấp danh tính: `authProvider = GOOGLE`.
     - Đánh dấu đã xác thực email: `emailVerified = true`.

5. **Phát hành Token Danasea & Hoàn tất:**
   - `JwtTokenProvider` ký phát `accessToken` (thời hạn ngắn) và `refreshToken` (thời hạn 7 ngày lưu trữ trong Redis).
   - Backend phản hồi `AuthenticationResponse` kèm thông tin user và cặp token.
   - Frontend lưu trữ token an toàn và điều hướng người dùng về Dashboard / Trang chủ.

---

## 2. Auth_ForgotPassword_Flow.png — Yêu Cầu Mã Xác Nhận Quên Mật Khẩu (OTP)

**Lớp xử lý chính:** `com.danasea.backend.security.authentication.application.usecases.ForgotPasswordUseCase`  
**Endpoint:** `POST /api/auth/forgot-password`

**Luồng nghiệp vụ chi tiết:**

1. **Khởi tạo yêu cầu từ Giao diện (Client Tier):**
   - Người dùng nhập địa chỉ email vào form quên mật khẩu trên Web Frontend hoặc Mobile App.
   - Client gửi `POST /api/auth/forgot-password` kèm body `{ email: "user@example.com" }` và header `Accept-Language` (mặc định `vi` hoặc `en`).

2. **Kiểm soát tần suất truy cập (Rate Limiting Filter):**
   - `RateLimitFilter` áp dụng thuật toán Token Bucket với Bucket4j lưu trữ trên Redis (`LettuceBasedProxyManager`).
   - Giới hạn: **Tối đa 5 requests / phút theo địa chỉ IP của Client** (`keyPrefix = "forgot-pw:" + IP`).
   - Nếu vượt ngưỡng cho phép, hệ thống lập tức từ chối với mã lỗi `429 Too Many Requests` (kèm header `Retry-After`), ngăn chặn nguy cơ spam mail và tấn công từ chối dịch vụ (DoS).

3. **Tra cứu tài khoản & Cơ chế chống rò rỉ người dùng (Anti-Enumeration Defense):**
   - Chuyển tiếp request đến `ForgotPasswordUseCase`.
   - Chuẩn hóa email (`trim().toLowerCase(Locale.ROOT)`) và tra cứu người dùng qua `AccountInternalApi.findUserByEmailForUpdate(normalizedEmail)`.
   - **Phòng vệ an ninh:**
     - Nếu email không tồn tại trong hệ thống: Ghi log info nội bộ và **Silent Return** (kết thúc xử lý trong im lặng).
     - Nếu tài khoản tồn tại nhưng đang bị khóa (`isLocked == true`): Ghi log warn nội bộ và **Silent Return**.
     - Ở cả hai trường hợp trên, Controller **luôn trả về HTTP 200 OK** với thông báo chung: *"Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn."* Điều này triệt tiêu hoàn toàn khả năng kẻ tấn công quét dò danh sách email người dùng (User Enumeration).

4. **Vô hiệu hóa mã cũ & Khởi tạo mã OTP mới:**
   - Trường hợp tài khoản hợp lệ và đang hoạt động (`ACTIVE`):
     - Gọi `AccountInternalApi.invalidatePasswordResetTokens(userId)` để đánh dấu toàn bộ mã OTP cũ chưa dùng thành `used = true`, đảm bảo tại một thời điểm mỗi người dùng chỉ sở hữu duy nhất 1 mã OTP hợp lệ.
     - Sinh mã OTP ngẫu nhiên gồm **6 chữ số** sử dụng bộ sinh số ngẫu nhiên an toàn mật mã học (`SecureRandom`).
     - Mã hóa OTP một chiều bằng thuật toán **SHA-256** (`HashUtils.sha256(rawOtp)`). Mã OTP dạng thô không bao giờ được lưu trữ trong cơ sở dữ liệu.
     - Thiết lập thời gian hết hạn là **15 phút** (`expiresAt = now() + 15 minutes`).
     - Lưu trữ bản ghi token mới vào bảng `password_reset_tokens` (`used = false, failed_attempts = 0`).

5. **Phát sự kiện và Xử lý gửi Email nền (Event-Driven & Asynchronous Worker):**
   - `ForgotPasswordUseCase` gọi `AuthEventPublisher.publishPasswordResetRequestedEvent(...)` để phát sự kiện `PasswordResetRequestedEvent(userId, email, rawOtp, locale)` lên RabbitMQ exchange `auth.events` với routing key `auth.user.password_reset_requested`.
   - Quá trình publish bất đồng bộ giúp API phản hồi về Client gần như tức thì mà không bị trễ do tác vụ mạng của SMTP.
   - Worker `PasswordResetEmailConsumer` lắng nghe queue `notification.password-reset-email.queue`:
     - Phân giải ngôn ngữ dựa vào trường `locale` để tải thông điệp email tương ứng (`email.password_reset.subject`, `email.password_reset.body`).
     - Điền mã OTP 6 số vào nội dung thư và chuyển giao cho `JavaMailSender` thực hiện gửi email qua giao thức SMTP.
     - **Cơ chế xử lý lỗi (DLQ):** Nếu gửi email thất bại do sự cố mạng/SMTP, Consumer sẽ ném `AmqpRejectAndDontRequeueException` để đưa thông điệp vào Dead Letter Queue (`auth.events.dlq`) phục vụ giám sát và retry.

---

## 3. Auth_ResetPassword_Flow.png — Xác Thực OTP & Đặt Lại Mật Khẩu Mới

**Lớp xử lý chính:** `com.danasea.backend.security.authentication.application.usecases.ResetPasswordUseCase`  
**Endpoint:** `POST /api/auth/reset-password`

**Luồng nghiệp vụ chi tiết:**

1. **Khách hàng gửi mã xác nhận và mật khẩu mới:**
   - Người dùng nhập mã OTP 6 số nhận được từ email và nhập mật khẩu mới.
   - Client gửi `POST /api/auth/reset-password` kèm payload: `{ email, otp, newPassword }`.
   - `RateLimitFilter` kiểm tra giới hạn tần suất trên endpoint (`5 requests / phút`).

2. **Tra cứu tài khoản với Khóa Bi Quan (Pessimistic Lock):**
   - `ResetPasswordUseCase` thực thi trong một `@Transactional(noRollbackFor = {OtpInvalidException.class, OtpMaxAttemptsExceededException.class})`.
   - Tìm kiếm người dùng qua `accountInternalApi.findUserByEmailForUpdate(normalizedEmail)` với mệnh đề SQL `SELECT ... FOR UPDATE`.
   - Khóa hàng (row-level lock) này bảo vệ hệ thống trước tình trạng tranh chấp dữ liệu (Race Condition) khi người dùng hoặc kẻ tấn công gửi nhiều request đổi mật khẩu đồng thời.
   - Nếu tài khoản không tồn tại hoặc `isLocked == true`: Ném ngoại lệ `OtpInvalidException` (trả về mã lỗi chung `AUTH_007`) để che giấu trạng thái thực tế của người dùng.

3. **Tra cứu Token OTP & Kiểm tra vòng đời:**
   - Gọi `accountInternalApi.findLatestActivePasswordResetToken(userId)` để tìm token mới nhất có `used = false` và `expiresAt > now()`.
   - Nếu không tìm thấy hoặc token đã quá hạn 15 phút: Ném ngoại lệ `OtpExpiredException` (mã lỗi `AUTH_007: Mã xác nhận không hợp lệ hoặc đã hết hạn`).

4. **Cơ chế phòng thủ tấn công Brute-force OTP (Brute-force Defense):**
   - **Bước kiểm tra trước (Pre-check):** Nếu token đã ghi nhận `failedAttempts >= 5`, UseCase lập tức gọi `markPasswordResetTokenUsed(tokenId)` để hủy token ngay và ném `OtpMaxAttemptsExceededException` (mã lỗi `AUTH_008`).
   - **Xác thực mã băm SHA-256:** Tính toán `HashUtils.sha256(rawOtp)` và đối chiếu với `token.getTokenHash()`.
   - **Trường hợp OTP không chính xác:**
     - Gọi `accountInternalApi.recordFailedPasswordResetAttempt(tokenId)` tăng bộ đếm `failed_attempts` lên 1 trong cơ sở dữ liệu.
     - Nhờ cấu hình `noRollbackFor`, giao dịch vẫn commit thành công giá trị tăng số lần thử sai dù ngoại lệ được ném ra.
     - Nếu sau khi tăng, số lần thử sai đạt mốc **5 lần**: Ném `OtpMaxAttemptsExceededException` (`AUTH_008`). Kẻ tấn công bị chặn hoàn toàn và buộc phải yêu cầu lại mã mới từ đầu.
     - Nếu chưa vượt quá 5 lần: Ném `OtpInvalidException` (`AUTH_007`).
   - **Trường hợp OTP hoàn toàn chính xác:** Chuyển sang bước đổi mật khẩu.

5. **Cập nhật Mật khẩu & Thu hồi bảo mật phiên (Session Revocation):**
   - Băm mật khẩu mới bằng `PasswordHasher.hash(newPassword)` (thuật toán an toàn BCrypt với độ phức tạp cao).
   - Cập nhật phiên bản phiên làm việc: `user.sessionVersion = user.sessionVersion + 1`. Cơ chế này khiến tất cả các Access Token (JWT) hiện có trên các thiết bị lập tức bị vô hiệu hóa khi gọi các API yêu cầu xác thực.
   - Lưu thông tin người dùng mới: `accountInternalApi.saveUser(user)`.
   - Đánh dấu token OTP đã sử dụng: `accountInternalApi.markPasswordResetTokenUsed(tokenId)` (`used = true`, `usedAt = now()`), đảm bảo tính chất **One-Time Use**.
   - Thu hồi toàn bộ Refresh Tokens: Gọi `accountInternalApi.revokeAllRefreshTokensByUserId(userId)` để vô hiệu hóa tất cả token làm mới trong Redis/PostgreSQL. Mọi thiết bị đã đăng nhập trước đó bắt buộc phải đăng nhập lại bằng mật khẩu mới.

6. **Ghi nhận Nhật ký kiểm toán (Audit Logging) & Phản hồi:**
   - Gọi `AuditLogInternalApi.recordAuditLog(userId, "PASSWORD_RESET", "USER", userId, "User reset password via OTP")` để lưu vết lịch sử đổi mật khẩu phục vụ an ninh và thanh tra.
   - Phản hồi `HTTP 200 OK` kèm thông điệp: *"Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại bằng mật khẩu mới."*
   - Client điều hướng người dùng quay trở lại màn hình Đăng nhập.
