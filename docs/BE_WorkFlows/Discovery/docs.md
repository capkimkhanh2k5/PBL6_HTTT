# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE DISCOVERY & PUBLIC STOREFRONT

> Phân hệ Khám phá Dịch vụ & Cửa hàng Đối tác Công khai (Discovery & Public Storefront) chịu trách nhiệm cung cấp khả năng tìm kiếm đa tiêu chí theo thời gian thực, tính toán số chỗ khả dụng của từng khung giờ (Structured Slots Availability), bảo vệ an toàn thông tin nhạy cảm của đối tác (Strict DTO Separation) và hiển thị danh mục sản phẩm của cửa hàng đối tác (Vendor Storefront).

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Service_Discovery_MultiCriteria_Search_Flow` | Sequence Diagram | Khách hàng tìm kiếm dịch vụ (`GET /api/services`): Lọc theo ngày/giờ, số khách, khoảng giá, đối tác, đánh giá tối thiểu và sắp xếp linh hoạt. |
| 2 | `Public_Service_Detail_And_Structured_Slots_Flow` | Sequence Diagram | Xem chi tiết dịch vụ (`GET /api/services/{id}`): Nạp thông tin vận hành, chính sách hủy/hoàn, khung giờ cấu trúc (Structured Slots) và bảo mật dữ liệu đối tác. |
| 3 | `Public_Vendor_Storefront_Flow` | Sequence Diagram | Hồ sơ và cửa hàng đối tác công khai (`GET /api/vendors/{id}`, `GET /api/vendors/{id}/services`): Hiển thị chỉ số uy tín và danh sách dịch vụ đang mở bán. |

---

## 1. Service_Discovery_MultiCriteria_Search_Flow.mmd — Tìm Kiếm & Khám Phá Dịch Vụ Mở Rộng

**Điểm truy cập API:** `GET /api/services` (hoặc alias `/api/v1/catalog`)

**Các tiêu chí tìm kiếm mở rộng (R1):**
- `date` (`LocalDate` ISO-8601) & `timeSlot` (`LocalTime`): Lọc các dịch vụ có khung giờ hoạt động mở trong ngày và giờ chỉ định.
- `dateTime` (`LocalDateTime`): Tự động phân tách thành `date` và `timeSlot` nếu client truyền dạng gộp.
- `guests` (hoặc alias `participantCount`): Số lượng hành khách yêu cầu. Hệ thống tính toán slot khả dụng và chỉ giữ lại các dịch vụ có ít nhất một khung giờ đáp ứng `availableCapacity >= guests`.
- `vendorId`: Lọc chính xác dịch vụ thuộc quyền quản lý của một nhà cung cấp cụ thể.
- `minRating` (`BigDecimal` từ 0.0 đến 5.0): Lọc theo điểm đánh giá sao trung bình của dịch vụ.
- `sortBy`: Sắp xếp động gồm `price_asc` (giá tăng dần), `price_desc` (giá giảm dần), `rating_desc` (đánh giá cao nhất), `views_desc` (lượt xem nhiều nhất).
- **Tương thích ngược đầy đủ:** Hỗ trợ song song các bộ lọc cũ gồm `categoryId`, `keyword`, `minPrice`, `maxPrice`, tọa độ địa lý `lat`/`lng`/`radiusKm` (công thức Haversine) và phân trang `page`/`size`.

**Quy tắc kiểm tra biên (Validation Guardrails):**
- `minRating`: Nằm trong khoảng `[0.0, 5.0]`, vi phạm trả về `400 Bad Request`.
- `guests`: Phải là số nguyên dương `> 0`, `<= 0` trả về `400 Bad Request`.
- `page`: Phải `>= 0`. `size`: Nằm trong khoảng `[1, 100]`.
- Dịch vụ phải có trạng thái `PUBLISHED` và `isDeleted = false`.

---

## 2. Public_Service_Detail_And_Structured_Slots_Flow.mmd — Chi Tiết Dịch Vụ & Khung Giờ Cấu Trúc

**Điểm truy cập API:** `GET /api/services/{id}` (Truy cập công khai không yêu cầu JWT)

**Kiến trúc phân tách & Bảo mật DTO (R2):**
- **Strict Data Isolation (Zero Leakage):** Loại bỏ hoàn toàn nguy cơ rò rỉ dữ liệu tài chính của đối tác. DTO phản hồi chỉ chứa thông tin công khai gồm: `vendorId`, `businessName`, `badgeTier` và `avatarUrl`. Tuyệt đối **không** chứa số tài khoản ngân hàng, tên ngân hàng, chủ tài khoản hay mã số thuế của đối tác.
- **Thông tin vận hành & điều kiện tham gia:**
  - `duration` (thời lượng trải nghiệm).
  - `capacity` (sức chứa tối đa của dịch vụ).
  - `participantConditions` (điều kiện sức khỏe, độ tuổi tham gia).
  - `cancellationPolicy`, `refundPolicy`, `safetyRules` (quy định hoàn hủy và an toàn biển).
- **Khung giờ cấu trúc thời gian thực (Structured Slots):**
  - Trả về danh sách `structuredSlots` chứa: `slotId`, `startTime`, `endTime`, `capacity`, `availableCapacity`, `price`.
  - Công thức tính: `availableCapacity = Math.max(0, slot.capacity - slot.bookedCount)`. Chống tràn số âm nếu số lượng đặt thực tế vượt quá sức chứa do quá trình điều chỉnh slot.
  - Vẫn duy trì mảng `availableSlots` dạng `List<LocalDateTime>` để bảo đảm tính tương thích ngược cho các client di động phiên bản cũ.

---

## 3. Public_Vendor_Storefront_Flow.mmd — Hồ Sơ & Cửa Hàng Đối Tác Công Khai

**Điểm truy cập API:**
- `GET /api/vendors/{id}`: Hồ sơ công khai của nhà cung cấp.
- `GET /api/vendors/{id}/services`: Cửa hàng danh mục dịch vụ của nhà cung cấp (Storefront).

**Quy trình xử lý (R3):**
1. **Kiểm tra tồn tại của đối tác:** Nếu `id` không khớp với nhà cung cấp nào trong cơ sở dữ liệu, ném ngoại lệ `VendorNotFoundException` và trả về `404 Not Found`.
2. **Tổng hợp dữ liệu công khai:**
   - Truy vấn thông tin doanh nghiệp, địa chỉ, ảnh đại diện, danh hiệu huy hiệu (`badgeTier`).
   - Nạp điểm đánh giá trung bình (`ratingAvg`) và tổng số lượt đánh giá (`ratingCount`).
   - Gọi `VendorActiveServicesPort.countActiveServicesByVendorId(vendorId)` để đếm tổng số dịch vụ đang hoạt động (`status = 'PUBLISHED'`).
3. **Danh mục cửa hàng đối tác (Storefront Services):**
   - Tái sử dụng `SearchServicesUseCase` với điều kiện lọc chặt chẽ `vendorId = id`.
   - Hỗ trợ sắp xếp theo giá và phân trang an toàn (`page >= 0`, `1 <= size <= 100`).
   - Trả về danh sách `PageResponse<ServiceSummaryResponse>` đồng nhất với giao diện tìm kiếm chung.
