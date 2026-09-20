# TÀI LIỆU BÀN GIAO & ĐỒNG BỘ NGHIỆP VỤ FRONTEND - MOBILE
**Dự án:** Nền tảng Đặt Trải Nghiệm & Thể Thao Biển Đà Nẵng (DANASEA)  
**Phiên bản:** Web Frontend Handoff v1.0  
**Đối tượng tham chiếu:** Thành viên phát triển Ứng dụng Di động (Mobile App Developer)

---

## 1. Hướng Dẫn Khởi Chạy Web Nhanh Để Đối Chiếu Giao Diện

Bạn có thể chạy dự án Web Frontend trên máy để tương tác trực quan với các luồng và màn hình:

```bash
# 1. Di chuyển vào thư mục frontend
cd frontend

# 2. Cài đặt các gói phụ thuộc (chỉ mất ~30 giây)
npm install

# 3. Khởi chạy dev server trên máy tính
npm run dev

# Hoặc mở server cho phép điện thoại thật truy cập qua Wi-Fi chung:
npm run dev -- --host
```

> **Mẹo kiểm tra trên điện thoại:** Khi chạy `npm run dev -- --host`, màn hình terminal sẽ in ra địa chỉ IP mạng nội bộ (ví dụ: `http://192.168.1.x:5173`). Bạn chỉ cần dùng trình duyệt trên điện thoại kết nối chung Wi-Fi là có thể mở ngay bản web để kiểm tra tỉ lệ co giãn, trải nghiệm vuốt chạm.

---

## 2. Tài Khoản Kiểm Thử 3 Phân Hệ & Công Cụ Dev Role Switcher

Web đã tích hợp sẵn bộ 3 tài khoản thử nghiệm với **mật khẩu chung: `123456`**:

| Phân hệ | Tên đăng nhập / Email | Gõ tắt | Mật khẩu | URL mặc định | Phạm vi nghiệp vụ |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 👤 **Khách hàng** | `customer@danasea.vn` | `customer` | `123456` | `/` | Đặt tour, giỏ hàng, vé QR, yêu thích, hồ sơ cá nhân |
| 🚤 **Đối tác Vendor** | `vendor@danasea.vn` | `vendor` | `123456` | `/vendor` | Quản lý tour, lịch xuất phát, quét QR check-in, doanh thu |
| 🛡️ **Quản trị viên** | `admin@danasea.vn` | `admin` | `123456` | `/admin` | Duyệt đối tác & tour, duyệt hoàn tiền, quản trị tài khoản |

### Tiện ích Chuyển Phân Hệ Nhanh (Dev Role Switcher)
- Ở góc dưới bên trái màn hình web (`bottom-5 left-5`), có một **nút tròn nổi phát sáng (DevRoleSwitcher)**.
- Bấm vào nút này sẽ hiển thị menu cho phép **chuyển đổi qua lại giữa Du khách ⇄ Vendor ⇄ Admin ngay lập tức** mà không cần đăng xuất hay nhập lại mật khẩu.

---

## 3. Bản Đồ Luồng Nghiệp Vụ (Flows) Cần Đồng Bộ Giữa Web & Mobile

### A. Phân Hệ Khách Hàng (Customer Flow) - Trọng Tâm Cho Mobile
1. **Khám phá & Tìm kiếm (Home & Search)**:
   - **Trang chủ (`/`)**: Banner trải nghiệm, danh mục thể thao biển (Chèo SUP, Cano lướt sóng, Lặn ngắm san hô Bán đảo Sơn Trà, Kayak, Tour đảo), danh sách tour nổi bật.
   - **Tìm kiếm (`/search`)**: Bộ lọc đa tiêu chí gồm: Từ khóa, Danh mục, Khoảng giá (VND), Địa điểm bãi biển, Thời lượng trải nghiệm.
2. **Yêu thích (Wishlist Flow)**:
   - Icon trái tim ở góc mỗi thẻ tour: Bấm vào ➔ Trái tim chuyển sang màu đỏ rực (`text-red-500` có hiệu ứng đổ bóng), hiển thị toast thông báo "Đã lưu vào danh sách yêu thích".
   - Badge số lượng trên Header và trong Trang cá nhân (`/profile/wishlist`) tự động tăng/giảm và đồng bộ qua LocalStorage.
   - Tại trang `/profile/wishlist`: Bấm bỏ lưu ➔ Xóa tour khỏi danh sách với toast thông báo và cập nhật tức thì.
3. **Chi tiết Dịch vụ & Chọn Lịch (Experience Detail)**:
   - Đường dẫn: `/tour/:id` hoặc `/experience/:id`.
   - Xem thông tin đầy đủ: Ảnh gallery, mô tả an toàn cứu hộ, trang bị đi kèm, vị trí bến đỗ / tọa độ cảng.
   - **Chọn Ngày & Khung giờ (Slot Booking)**: Chọn ngày khởi hành ➔ Chọn Slot giờ (ví dụ: `05:00 - 07:00` đón bình minh) ➔ Hiển thị số chỗ còn trống (capacity vs bookedCount) ➔ Chọn số lượng vé (Người lớn, Trẻ em).
4. **Giỏ hàng & Thanh toán (Cart & Checkout)**:
   - **Giỏ hàng (`/cart`)**: Xem các tour đã chọn, cập nhật số lượng, áp dụng mã khuyến mãi (Promotion).
   - **Thanh toán (`/checkout`)**: Điền thông tin hành khách (Họ tên, SĐT, Email để nhận vé QR), đồng ý cam kết miễn trừ trách nhiệm an toàn biển (Waiver Acceptance).
   - **Kết quả đặt chỗ (`/checkout/result`)**: Tạo đơn hàng thành công, hiển thị **Mã đơn hàng** và **Mã QR vé điện tử** để check-in tại cảng.
5. **Trang cá nhân & Lịch sử Đơn hàng (Profile & Orders)**:
   - Đường dẫn: `/profile/orders`.
   - Danh sách vé: Tab phân loại *Sắp diễn ra*, *Đã hoàn thành*, *Đã hủy*.
   - Mỗi vé có nút **"Xem vé điện tử QR"** mở popup mã QR sắc nét kèm thông tin slot giờ và vị trí cảng để nhân viên vendor quét.
   - Cho phép gửi yêu cầu hủy vé hoặc tạo khiếu nại (Dispute) nếu tour chưa khởi hành hoặc có sự cố dịch vụ.
6. **Trợ lý Thông minh (AI Assistant)**:
   - Đường dẫn: `/profile/ai-assistant`.
   - Trợ lý hỏi đáp thông minh hỗ trợ gợi ý lịch trình, dự báo thời tiết biển Đà Nẵng, tư vấn chuẩn bị đồ lặn.

---

### B. Phân Hệ Đối Tác Nhà Cung Cấp (Vendor Flow)
1. **Bảng điều khiển KPI (`/vendor`)**: Doanh thu trong ngày, số vé đã check-in, số chỗ còn trống trong các chuyến cano sắp xuất bến.
2. **Quản lý Dịch vụ (`/vendor/services`)**: Danh sách tour cano/lặn biển, trạng thái duyệt (`APPROVED`, `PENDING_APPROVAL`, `DRAFT`), thêm mới tour (`/vendor/services/new`).
3. **Quản lý Lịch trình (`/vendor/schedule`)**: Mở/đóng slot giờ xuất bến theo từng ngày.
4. **Trạm Soát Vé QR (QR Terminal Check-in) (`/vendor/orders`)**:
   - Quản lý các booking từ khách.
   - Nút **"Mở Trạm quét vé QR"** mô phỏng camera quét mã QR của du khách tại bến tàu: Quét mã QR ➔ Kiểm tra tính hợp lệ ➔ Đổi trạng thái vé sang `CHECKED_IN` / `COMPLETED`.
5. **Doanh thu & Rút tiền (`/vendor/revenue`, `/vendor/payouts`)**: Theo dõi doanh thu thực nhận sau khi trừ hoa hồng sàn (commissionRate thường là 10%), tạo lệnh yêu cầu rút tiền (Payout Request).
6. **Xử lý Khiếu nại (`/vendor/disputes`)**: Phản hồi giải trình với các khiếu nại từ du khách trước khi Admin phân xử.

---

### C. Phân Hệ Quản Trị Viên (Admin Flow)
1. **Duyệt Hồ Sơ Đối Tác (`/admin/vendor-approval`)**: Xem giấy phép kinh doanh bến bãi, phương tiện cano, thẩm định và phê duyệt.
2. **Duyệt Tour Mới (`/admin/service-approval`)**: Đảm bảo tour đáp ứng quy chuẩn an toàn cứu hộ biển trước khi cho phép mở bán công khai.
3. **Phân Xử Khiếu Nại & Hoàn Tiền (`/admin/disputes`, `/admin/orders`)**: Trọng tài xử lý khiếu nại chất lượng dịch vụ, duyệt lệnh hoàn tiền (Refund) về tài khoản khách hàng.
4. **Quản trị Danh mục & Người dùng (`/admin/categories`, `/admin/users`)**: Khóa/mở khóa tài khoản, phân quyền vai trò (`ADMIN`, `VENDOR`, `CUSTOMER`).

---

## 4. Tham Chiếu Data Models & Schema CSDL Backend

Toàn bộ cấu trúc TypeScript Entity đã được chuẩn hóa theo Database Schema Backend tại:  
👉 **`frontend/src/types/index.ts`**

### Các Thực Thể Chính Mà Mobile Cần Định Nghĩa Tương Ứng:

#### 1. Người dùng (`User`):
```typescript
interface User {
  id: string;
  email: string;
  phone: string;
  fullName: string;
  role: 'CUSTOMER' | 'VENDOR' | 'ADMIN';
  avatarUrl: string;
  isEmailVerified: boolean;
  isLocked: boolean;
  locale: string;
}
```

#### 2. Dịch vụ Trải nghiệm / Tour (`Service`):
```typescript
interface Service {
  id: string;
  categoryId: string;
  name: string;
  description: string;
  basePrice: number;           // Giá cơ sở (VND)
  durationMinutes: number;     // Thời lượng (phút)
  locationName: string;        // Địa điểm (ví dụ: Bán đảo Sơn Trà, Bãi Bụt)
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'ACTIVE' | 'REJECTED' | 'HIDDEN' | 'PAUSED';
}
```

#### 3. Khung Giờ Khởi Hành (`ServiceSlot`):
```typescript
interface ServiceSlot {
  id: string;
  serviceId: string;
  date: string;                // YYYY-MM-DD
  startTime: string;           // HH:mm (ví dụ: "05:00")
  endTime: string;             // HH:mm (ví dụ: "07:00")
  capacity: number;            // Sức chứa tối đa (người)
  bookedCount: number;         // Số chỗ đã đặt
  status: 'OPEN' | 'FULL' | 'CANCELLED' | 'COMPLETED';
}
```

#### 4. Đơn Hàng & Vé Điện Tử QR (`MasterOrder` & `SubOrder`):
```typescript
interface MasterOrder {
  id: string;
  userId: string;
  totalAmount: number;
  paymentStatus: 'UNPAID' | 'PAID' | 'FAILED' | 'REFUNDED';
  paymentMethod: 'VNPAY' | 'MOMO' | 'CREDIT_CARD';
  customerNotes?: string;
  createdAt: string;
}

interface SubOrder {
  id: string;
  masterOrderId: string;
  vendorId: string;
  serviceId: string;
  slotId: string;
  quantity: number;
  unitPrice: number;
  subtotalAmount: number;
  commissionRate: number;      // Tỉ lệ hoa hồng sàn (%)
  commissionAmount: number;
  vendorPayoutAmount: number;
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED';
  waiverAccepted: boolean;     // Đã chấp nhận cam kết an toàn
  waiverAcceptedAt?: string;
  qrSecret: string;            // Chuỗi mã hóa sinh mã QR check-in
  checkedInAt?: string;        // Thời điểm đã check-in tại cảng
}
```

---

## 5. Quy Chuẩn Thiết Kế & Nhận Diện (Design Tokens)

- **Typography**:
  - Tiêu đề, nút bấm, số liệu KPI: Font **`Outfit`** (weights: 600, 700, 800) mang lại phong cách hiện đại, phóng khoáng của đại dương.
  - Nội dung văn bản, bảng biểu, form nhập liệu: Font **`Plus Jakarta Sans`** (weights: 400, 500, 600) hiển thị tiếng Việt sắc nét, rõ ràng.
- **Bảng màu chủ đạo**:
  - **Primary (Đại dương sâu)**: Xanh nước biển đậm `#006699` / HSL: `199° 89% 48%`
  - **Primary-Container (Xanh ngọc biển Đà Nẵng)**: `#0284c7` / HSL: `201° 96% 32%`
  - **Secondary (Cát vàng & San hô)**: `#f59e0b` / Cam vàng ấm áp
  - **Surface (Nền sáng hiện đại)**: `#f8fafc` / Trắng ngà nhẹ nhàng
- **Bộ Icon**:
  - Sử dụng chuẩn **Google Material Symbols Outlined** (ví dụ: `surfing`, `speed`, `scuba_diving`, `kayaking`, `sailing`, `qr_code_2`, `favorite`).

---

## 6. Liên Hệ & Hỗ Trợ Kỹ Thuật

Mọi thắc mắc về luồng xử lý hoặc cấu trúc API có thể liên hệ trực tiếp thành viên phụ trách Web Frontend hoặc tham khảo mã nguồn chi tiết trong thư mục `src/` đính kèm.
