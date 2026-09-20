# WorkFlow Tổng AI Chat
BE -> LLM Moderation (ít tham số để quét mã độc) -> LLM (để chat) -> gọi Tools ( nếu LLM yêu cầu ) -> LLM trả về kết quả -> Lưu vào Database (history) -> Trả về FE -> FE hiển thị

# Các Tools của LLM tương tác với Database
*Lưu ý: LLM không bao giờ được phép thực thi SQL trực tiếp (đã chặn bằng PromptGuard). LLM chỉ được gọi Tool, và Code Java phía sau Tool đó mới thực hiện query vào DB.*

- **`search_service`** (READ) (PostgreSQL): Dùng câu lệnh `SELECT` kết hợp toán tử `@@ plainto_tsquery` để quét dữ liệu bảng `services`
- **`get_service_detail`** (READ) (PostgreSQL): `SELECT` chi tiết bảng `services` kèm các bảng liên quan (giá theo ngày, slot còn trống)
- **`get_policy`** (READ) (PostgreSQL): Đọc các cài đặt, cấu hình, chính sách hệ thống (có thể cache trên Redis để load nhanh)
- **`request_booking_confirmation`** (WRITE) (Redis): Ghi thông tin vé (Draft Card) vào RAM (Redis) với thời gian hết hạn (TTL:15p)


# Các API hệ thống tự động tương tác với Database (Đứng sau lưng LLM)

- **`POST /.../chat`** (READ + WRITE) (PostgreSQL)  
    + READ: Lấy lịch sử chat cũ (Conversation) làm bối cảnh. 
    + WRITE: Lưu tin nhắn của Khách và AI vào DB. 
    + WRITE: Lưu hành động vào `AssistantAuditLog` để giám sát.
- **`POST /.../confirm`** (READ + WRITE) (Redis & PostgreSQL) 
    + READ: Lấy Card nháp từ Redis. 
    + READ: Kiểm tra lại giá thực tế từ Postgres. 
    + WRITE: Ghi dữ liệu Hóa Đơn/Booking chính thức vào Postgres. 
    + WRITE: Xóa (DEL) Card nháp khỏi Redis.

---

# Ví dụ luồng Chat thực tế: Hỏi Dịch Vụ A -> Chốt Đơn Hàng

Giả sử khách hàng muốn tìm tour lặn ngắm san hô. Dưới đây là cách các Tools và APIs phối hợp xử lý ở BE:

## BƯỚC 1: Khách hàng hỏi thông tin
* Khách: *"Mai tôi muốn đi lặn ngắm san hô ở Cù Lao Chàm, giá sao?"*
* Hệ thống: Khách gọi `POST /api/assistant/chat`.
* AI (LLM): Phân tích câu hỏi, nhận thấy thiếu dữ liệu thực tế -> Quyết định gọi Tool 🟢 **READ DB**.
  * AI kích hoạt `search_service(keyword="lặn ngắm san hô", location="Cù Lao Chàm", date="tomorrow")`.
  * *Backend Query Postgres trả về kết quả cho AI.*
* 🤖 **AI (LLM):** Đọc kết quả và trả lời khách: *"Dạ, Tour Lặn san hô Cù Lao Chàm ngày mai giá 500k/người, đang còn trống 5 chỗ. Bạn đi mấy người ạ?"* 
  *(Tin nhắn này được Backend 🔴 **WRITE** lưu vào lịch sử DB).*

## BƯỚC 2: Khách hàng chốt số lượng
* 🙎‍♂️ **Khách:** *"Mình đi 2 người nhé."*
* 💻 **Hệ thống:** Khách gọi `POST /api/assistant/chat`.
* 🤖 **AI (LLM):** Tính toán tổng tiền (500k x 2) -> Quyết định gọi Tool 🔴 **WRITE Redis** để chốt đơn.
  * AI kích hoạt `request_booking_confirmation(service_id="TOUR_01", quantity=2, total_price=1000000)`.
  * *Backend sinh ra mã thẻ `card_123` lưu vào Redis.*
* 🤖 **AI (LLM):** Trả lời: *"Tuyệt vời! Mình đã tạo phiếu xác nhận bên dưới. Tổng tiền là 1.000.000đ cho 2 người. Bạn bấm xác nhận nhé!"* (Gửi kèm giao diện Card thanh toán cho Frontend).

## BƯỚC 3: Xác nhận đơn hàng (Bỏ qua LLM, chạy code nghiệp vụ 100%)**
* 🙎‍♂️ **Khách:** Bấm nút **[Xác Nhận Đặt Chỗ]** trên giao diện.
* 💻 **Hệ thống:** Frontend không gọi `/chat` nữa mà gọi API `POST /api/assistant/conversations/{id}/confirm` gửi kèm `cardId="card_123"`.
* ⚙️ **Backend Nghiệp Vụ:** 
  1. 🟢 Lấy `card_123` từ Redis.
  2. 🟢 **READ DB:** Kiểm tra lại lần cuối xem `TOUR_01` còn đủ 2 chỗ trống không, giá có bị admin đổi đột xuất không. (Bước Re-validation).
  3. Mọi thứ hợp lệ -> 🔴 **WRITE DB:** Tạo Record Đơn Hàng (Booking) chính thức trong PostgreSQL. Xóa Card trong Redis.
  4. Trả về HTTP 200 SUCCESS.
* 💻 **Giao diện:** Chuyển khách hàng sang trang chọn phương thức thanh toán.

💡 Điểm mấu chốt ở thiết kế này:** LLM chỉ làm nhiệm vụ tư vấn và "đặt nháp" (nháp lưu trên RAM - Redis để giữ rác khỏi DB). Quyền quyết định ghi vào Database Hóa đơn/Booking cuối cùng hoàn toàn nằm ở Core Backend thông qua API Confirm, LLM không thể bị "lừa" tạo hóa đơn ảo vào DB.