# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE AUTHENTICATION & IAM (GOOGLE OAUTH2)

> Module IAM & Authentication chịu trách nhiệm xác thực danh tính người dùng, cấp phát JWT và hỗ trợ cơ chế đăng nhập một chạm (Single Sign-On / One Tap) qua Google Identity Services (GSI).

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Google_OAuth2_Login_Flow` | Sequence Diagram | Quy trình xác thực Google ID Token, kiểm tra tài khoản hoặc tự động đăng ký (Auto-Provisioning) và phát hành cặp Access/Refresh Token Danasea |

---

## 1. Google_OAuth2_Login_Flow.png — Luồng Đăng Nhập & Tự Động Đăng Ký Với Google

**Lớp xử lý chính:**
- `com.danasea.backend.security.authentication.presentation.controllers.AuthenticationController`
- `com.danasea.backend.security.authentication.application.usecases.GoogleOAuth2LoginUseCase`
- `com.danasea.backend.security.authentication.infrastructure.adapters.GoogleTokenVerifierAdapter`
- `com.danasea.backend.modules.user.infrastructure.persistence.repositories.JpaUserRepository`
- `com.danasea.backend.security.jwt.JwtTokenProvider`

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
