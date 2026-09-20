# DANASEA Vendor Web đã chỉnh

Được sửa từ `stitch_danasea_marine_experience_website.zip`, giữ 10 màn web và bố cục desktop gốc, bổ sung trang chi tiết khiếu nại theo yêu cầu.

1. Giải nén toàn bộ ZIP.
2. Mở `index.html` để chọn màn hình. Giữ các file `vendor-web.js` và `vendor-web.css` ở thư mục gốc.
3. Nếu thử camera QR, chạy máy chủ tĩnh tại thư mục này: `python -m http.server 8080`, rồi mở `http://localhost:8080`. Camera cần quyền truy cập và hỗ trợ trình duyệt; luôn có nhập mã thủ công.

Đây là bản xem trước HTML, chưa có backend. Thay đổi chỉ nằm trong phiên trang; không có upload, lưu database hoặc giao dịch thật. Cần mạng để tải Tailwind, font và ảnh nguồn.

Đọc `DATABASE_AUDIT.md` để xem 14 mục sửa và những điểm database chưa quy định. `DATABASE_REFERENCE.txt` trích tài liệu database đã cung cấp. `CHECK_RESULTS.json` ghi các kiểm tra đã chạy. Không tạo thêm trang xác thực và không mở rút gộp khi chưa thống nhất web/app.


Cập nhật theo góp ý giao diện: chi tiết khiếu nại là trang riêng `danasea_vendor_chi_tiet_khieu_nai/code.html?case=DSP-01`, có sidebar/header và liên kết quay lại danh sách. Dữ liệu chia thành thẻ thông tin, huy hiệu trạng thái, nội dung phản ánh, kết luận Admin và khu vực chọn/kéo thả minh chứng. Không thêm trường database.
