# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE I18n (ĐA NGÔN NGỮ QUỐC TẾ HÓA)

> Module I18n là giải pháp cross-cutting concern đảm nhiệm vai trò phân giải ngôn ngữ của từng HTTP Request, cung cấp ngữ cảnh ngôn ngữ cho tầng nghiệp vụ và tự động hóa việc bản địa hóa các thông báo lỗi, cảnh báo hệ thống.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `I18n_LocaleResolution` | Sequence Diagram | Quy trình phân giải ngôn ngữ: Đọc `Accept-Language` header -> Đối chiếu hồ sơ người dùng (fallback) -> Gán mặc định (Default EN), thiết lập LocaleContextHolder và phản hồi các headers tương ứng (`Content-Language`, `Vary`) |
| 2 | `I18n_ErrorLocalization` | Flowchart | Luồng bản địa hóa thông báo lỗi: Phân tách `LocalizedException` tự động nạp message từ các tệp Resource Bundle (`i18n/*.properties`) theo đúng ngôn ngữ người dùng |

---

## 1. I18n_LocaleResolution.png — Phân Giải Ngôn Ngữ Theo Request

**Lớp xử lý chính:** `com.danasea.backend.shared.i18n.LocaleContextFilter`

**Quy trình phân giải ngôn ngữ (theo thứ tự ưu tiên giảm dần):**
1. **Header `Accept-Language`:** Bộ lọc trích xuất giá trị ngôn ngữ từ header của request thông qua `SupportedLanguage.fromAcceptLanguage()`. Hệ thống hỗ trợ tiếng Việt (`VI`) và tiếng Anh (`EN`).
2. **Cấu hình hồ sơ cá nhân (User Profile):** Nếu header rỗng hoặc gửi ngôn ngữ không được hỗ trợ, hệ thống truy vấn hồ sơ người dùng đăng nhập (`AccountInternalApi.findUserByEmail`) để lấy `user.locale`.
3. **Giá trị mặc định (Fallback):** Nếu cả 2 nguồn trên đều không khả dụng, sử dụng ngôn ngữ mặc định của hệ thống là `SupportedLanguage.DEFAULT` (`EN`).
4. **Cài đặt ngữ cảnh:** Lưu locale đã phân giải vào `LocaleContextHolder` cho Thread hiện tại, đồng thời đính kèm attribute `"language"` vào request.
5. **Gán Response Headers:**
   - `Content-Language: vi` (hoặc `en`): Khai báo ngôn ngữ nội dung phản hồi.
   - `Vary: Accept-Language`: Hỗ trợ tầng Caching / CDN lưu trữ phiên bản theo từng ngôn ngữ.
6. **Dọn dẹp tài nguyên (Finally block):** Luôn gọi `LocaleContextHolder.resetLocaleContext()` sau khi chuỗi Filter hoàn tất để ngăn chặn rò rỉ bộ nhớ (ThreadLocal leak).

---

## 2. I18n_ErrorLocalization.png — Bản Địa Hóa Thông Báo Lỗi

**Thành phần tham gia:** `LocalizedException`, `LocalizedMessageService`, `GlobalExceptionHandler`, `ErrorResponseLocalizationAdvice`

**Cơ chế hoạt động:**
- Khi tầng UseCase/Service phát sinh lỗi nghiệp vụ có hỗ trợ đa ngôn ngữ, hệ thống ném ra `LocalizedException(errorCode, messageKey, params...)`.
- `LocalizedMessageService` nạp chuỗi thông báo từ các tệp `i18n/{module}_{lang}.properties` nằm trong classpath (hỗ trợ phân tách theo 8 domain chính: `common`, `validation`, `auth`, `booking`, `weather`, `notification`, `ai`, `policy`).
- Động cơ thực hiện thay thế (interpolate) các tham số động vào câu thông báo và đóng gói thành đối tượng chuẩn `ErrorResponse`.
- Khách hàng nhận được thông báo lỗi chính xác, tự nhiên theo đúng ngôn ngữ giao diện đã lựa chọn.
