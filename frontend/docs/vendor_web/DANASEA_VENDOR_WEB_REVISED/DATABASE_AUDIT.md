# Đối chiếu Vendor Web với database

Đầu vào đúng: `stitch_danasea_marine_experience_website.zip` (10 màn web). Nguồn chuẩn: `DANASEA_Database_Design.docx`. ZIP `DANASEA_VENDOR_REVISED.zip` trước đó chỉ được dùng tham khảo cách hiển thị app; không dùng làm nền cho bản web này. Không sửa schema hoặc thêm migration.

## 14 mục đã chỉnh

| Mục | Kết quả trên web | Database |
|---|---|---|
| Hồ sơ | Xác minh APPROVED tách khỏi huy hiệu TOP_RATED; có giải thích đủ giá trị, chỉ đọc | vendors.verification_status PENDING/APPROVED/REJECTED; badge_tier NONE/VERIFIED/TOP_RATED |
| Ngân hàng | Modal nhập/đổi ngân hàng, số tài khoản dạng chuỗi giữ số 0 đầu, tên chủ tài khoản; cập nhật bản xem trước | bank_name, bank_account_number, bank_account_holder |
| Giấy tờ | Bỏ mục kiểm định phương tiện thứ ba; form chọn đúng BUSINESS_LICENSE/SAFETY_CERT và chọn tệp; tài liệu mới minh họa PENDING | vendor_documents; chứng chỉ riêng dịch vụ thuộc service_safety_documents |
| Soạn dịch vụ | Thêm min_wind_kmh, giữ max_wave_m, toggle thời tiết, waiver_content; ảnh có nút lên/xuống và sort_order | services và service_images |
| Tạo slot | Modal dịch vụ, ngày, giờ bắt đầu/kết thúc, sức chứa; kiểm tra giờ và ca trùng trong phiên | service_slots: OPEN, booked_count=0 khi tạo mẫu |
| Sức chứa | Ca 1 capacity=12, booked=10, holds=2, available=0; tối thiểu 12 ở form và mô tả | service_slots.booked_count; giữ tạm ở Redis, không thêm cột PostgreSQL |
| Đơn | Mã đơn tổng/con trên thẻ; đủ PENDING/CONFIRMED/REJECTED/COMPLETED/CANCELLED/REFUNDED; bộ lọc riêng | master_orders và sub_orders |
| Check-in | Giữ nhập mã/QR; camera trên trình duyệt hỗ trợ; phản hồi chưa xác nhận/từ chối/hủy/hoàn tiền/hoàn thành/đã dùng; không xác thực mã chỉ vì chứa chuỗi số giống vé mẫu | qr_secret, checked_in_at; không công khai secret |
| Khuyến mãi | PERCENTAGE/FIXED, scope VENDOR; form tạo/sửa và kiểm tra giá trị, thời gian, số lượt; giữ lịch sử sử dụng | discount_codes, discount_redemptions |
| Đối soát | Bỏ mô tả 10% cố định cấp kỳ; cộng commission_amount theo snapshot từng đơn; đồng nhất đơn SUB-89412-01 thành 12%, 67.200đ, thực nhận 492.800đ | sub_orders.commission_rate/commission_amount/vendor_payout_amount; settlements/settlement_items |
| Yêu cầu nhận tiền | Đủ REQUESTED/APPROVED/PAID/REJECTED, số tiền, thời điểm xử lý; Vendor chỉ gửi; bỏ ghi chú, biên lai và mã giao dịch chi trả | payout_requests |
| Khiếu nại | Tab danh sách chuyển sang trang chi tiết riêng: đơn con/người khiếu nại/loại/nội dung/minh chứng/trạng thái/kết luận Admin; chọn tệp minh chứng mẫu | disputes và dispute_attachments |
| Hoàn tiền | Tab danh sách và chi tiết số tiền/tỷ lệ/lý do/thời điểm; PENDING/PROCESSED/FAILED; chỉ xem | refunds |
| Thông báo | Chuông mở danh sách và chi tiết; thời gian tạo/gửi, đối tượng; cảnh báo gắn dịch vụ và slot | notifications và safety_rule_evaluations |

## Những điểm khác hoặc chưa được database quy định

1. **Ngưỡng gió:** giữ đúng `min_wind_kmh` = ngưỡng gió tối thiểu. Tài liệu không nêu công thức rule engine. Nếu ý định là gió tối đa thì cần đổi đặc tả; chưa tự tạo `max_wind_kmh`.
2. **Rút gộp:** database cho phép settlement_id=NULL khi gộp nhưng chưa mô tả bảng liên kết các kỳ và phân bổ số tiền. Lựa chọn gộp bị khóa, chờ thống nhất triển khai ở web và app. Không tự mở tính năng.
3. **Giữ chỗ:** số holds lấy từ Redis còn TTL, không phải trường lưu trong PostgreSQL. Minimum=booked+holds là kiểm tra UI trên snapshot. Backend phải kiểm tra lại và cập nhật nguyên tử; ZIP HTML không xử lý được giữ chỗ đồng thời.
4. **Mã hiển thị:** mã DNS/SUB/SET/SUP trong bản gốc và các dữ liệu bổ sung là mẫu đọc hiểu. Schema chỉ có UUID id, không có cột mã đơn ngắn/mã vé riêng. API cần thống nhất ánh xạ; không tự thêm cột mã.
5. **Ngân hàng:** không có trạng thái duyệt ngân hàng hoặc bảng yêu cầu đổi ngân hàng. Form chỉ chỉnh ba trường hiện có. Không coi UI là xác minh ngân hàng thật.
6. **Yêu cầu nhận tiền:** không có created_at, ghi chú, lý do từ chối riêng, biên lai hoặc mã giao dịch ngân hàng. Đã bỏ thông tin đó ở màn chi trả. REQUESTED chưa xử lý hiển thị processed_at=NULL. Các trạng thái lịch sử là các bản ghi minh họa riêng.
7. **Thông báo:** không có is_read/read_at. Không bổ sung thao tác ghi trạng thái đã đọc; SENT chỉ là trạng thái gửi. type/related_entity_type là VARCHAR, các tên trong mẫu cần API thống nhất.
8. **Khiếu nại:** dispute_attachments chỉ có dispute_id/file_url, không có uploader hoặc timestamp. Tệp chọn là blob xem trước, chưa upload để nhận URL thật. Vendor không tự sửa kết luận hoặc kết thúc khiếu nại.
9. **QR:** camera chỉ đọc QR nếu trình duyệt hỗ trợ BarcodeDetector và có quyền trên HTTPS/localhost. Xác minh chữ ký/secret, thu hồi vé, quyền Vendor và chống quét lại xuyên thiết bị cần server. Chống lặp mẫu chỉ có hiệu lực trong phiên trang, không phải bảo đảm production. Mã nhập bất kỳ không được coi là hợp lệ.
10. **Hoàn tiền:** từ chối đơn không tự tạo hoàn 100%. CANCELLED và REFUNDED tách biệt; trạng thái bản ghi refund cũng tách biệt. Tỷ lệ 50%/100% ở dữ liệu mẫu không xác lập chính sách.
11. **Chu kỳ:** không có lịch chi trả cố định hoặc SLA 24 giờ trong database. Đã bỏ các mô tả thứ Ba/thứ Sáu, ngày 18/ngày 3 và thời hạn tự đặt. Ngày 18/10/2024 còn lại là thời điểm xử lý của bản ghi PAID mẫu, không phải lịch định kỳ.
12. **Lịch sử mã giảm giá:** discount_redemptions không có timestamp áp dụng và trạng thái đối soát riêng. Đã đổi nhãn thời gian thành thời điểm tạo đơn tổng; dữ liệu mẫu cần join master_orders.created_at. Các nhãn đối soát của bảng gốc chỉ là trình bày minh họa, không được gửi như trường của discount_redemptions.
13. **Validation bổ sung:** số dương, phần trăm <=100, kết thúc sau bắt đầu, không vượt số tiền khả dụng và max_uses>=used_count là quy tắc UI đề xuất; tài liệu schema không mô tả toàn bộ CHECK constraints. Form slot hiện tạo ca trong cùng ngày; ca qua đêm cần đặc tả thêm.
14. **Dữ liệu tổng hợp gốc:** số liệu dashboard, tên khách/dịch vụ, kỳ lịch sử, tỷ lệ đánh giá và ảnh là minh họa độc lập, không phải dataset đồng bộ thật. Bảng đối soát vẫn chỉ hiển thị một phần đơn trong kỳ. Không suy ra tổng kỳ bằng cách áp một tỷ lệ cho mọi đơn.
15. **Phạm vi app:** đã dùng cùng enum/khái niệm với tài liệu database và bộ HTML app tham chiếu. Chưa có mã app thực thi/API để xác nhận đồng bộ dữ liệu hai sản phẩm. Điểm rút gộp và lịch chi trả cần chốt chung.
16. **Xác thực:** không tạo thêm đăng nhập/đăng ký/email/đổi mật khẩu; dùng trang chung khi tích hợp web. Không đưa ba màn xác thực từ ZIP app cũ vào bộ web này.
17. **Ngoài phạm vi database:** đã bỏ hoặc viết lại ví/VITA Pay/Escrow/quỹ tín thác, cấp-thu thiết bị, phân công hướng dẫn viên, GPS theo dõi và kiểm định định kỳ như tính năng hệ thống. Tọa độ cố định của dịch vụ được giữ vì schema có latitude/longitude. Mô tả tour có hướng dẫn viên/thiết bị là nội dung dịch vụ, không phải module quản lý vận hành.
18. **Nội dung pháp lý/đơn vị gốc:** các nhãn Cảng vụ/VITA, hotline và mô tả tiêu chuẩn còn lại là nội dung thiết kế tham chiếu, không được schema xác nhận là tích hợp cơ quan thực tế. Đã loại lời hứa chi trả và xác thực nghiệp vụ liên quan; cần chủ sản phẩm duyệt lại nội dung thương hiệu/pháp lý trước khi công bố.

## Giới hạn bàn giao

Đây là bộ HTML tĩnh, không có backend, migration hoặc database chạy thật. Không gửi yêu cầu thật, không upload thật, không chuyển tiền. Thay đổi mẫu không được lưu sau reload hoặc đồng bộ xuyên trang. Một số thao tác trang trí của bản Stitch vẫn chỉ là nút mẫu; không coi là chức năng backend đã triển khai.

Giữ bố cục desktop/sidebar của 10 màn gốc; thêm form/modal/tab và CSS/JS dùng chung. Font, Tailwind và ảnh ngoài vẫn cần mạng. Đã thay logo ngoài bị lỗi bằng biểu tượng sóng cùng wordmark có sẵn. screen.png được chụp lại từ HTML đã sửa, không sử dụng ảnh cũ để minh họa kết quả mới.

Kết quả kiểm tra trình duyệt ở CHECK_RESULTS.json. Không kiểm thử giao dịch ngân hàng, API, camera thiết bị vật lý, chống giữ chỗ đồng thời hay app thực thi.


Cập nhật theo góp ý giao diện: chi tiết khiếu nại là trang riêng `danasea_vendor_chi_tiet_khieu_nai/code.html?case=DSP-01`, có sidebar/header và liên kết quay lại danh sách. Dữ liệu chia thành thẻ thông tin, huy hiệu trạng thái, nội dung phản ánh, kết luận Admin và khu vực chọn/kéo thả minh chứng. Không thêm trường database.
