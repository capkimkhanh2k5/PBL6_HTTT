# Tổng hợp các tính năng AI cho hệ thống DANASEA

DANASEA là sàn thương mại điện tử đa nhà cung cấp chuyên về trải nghiệm du lịch biển tại Đà Nẵng. AI sẽ hỗ trợ 3 nhóm đối tượng: Khách hàng, Nhà cung cấp và Quản trị viên (Admin).

## 1. AI dành cho khách hàng

| Tính năng                | Mô tả ngắn gọn                                                                                   |
| ------------------------ | ------------------------------------------------------------------------------------------------ |
| 1. AI Travel Assistant   | Trợ lý AI tư vấn, giải đáp về dịch vụ, giá, lịch trống, chính sách và đặt trải nghiệm.           |
| 2. AI Smart Search       | Tìm dịch vụ bằng ngôn ngữ tự nhiên thay vì chỉ sử dụng bộ lọc truyền thống.                      |
| 3. AI Recommendation     | Cá nhân hóa gợi ý theo sở thích, ngân sách, số người, vị trí và thời gian.                       |
| 4. AI Itinerary Planner  | Tự động lập lịch trình du lịch biển theo ngân sách, thời gian và dịch vụ thực tế đang còn chỗ.   |
| 5. Weather-Aware AI      | Phân tích dữ liệu thời tiết và kết quả kiểm tra quy tắc an toàn để cảnh báo, gợi ý lịch phù hợp. |
| 6. AI Re-planning        | Đề xuất điều chỉnh lịch trình khi thời tiết thay đổi hoặc dịch vụ bị hủy.                        |
| 7. AI Service Comparison | So sánh nhiều dịch vụ về giá, chất lượng, đánh giá, thời lượng và chính sách.                    |
| 8. AI Review Summary     | Tóm tắt ưu điểm, nhược điểm từ đánh giá thực tế của khách hàng.                                  |
| 9. AI Nearby Discovery   | Đề xuất trải nghiệm gần vị trí hiện tại, phù hợp thời gian và lịch còn trống.                    |
| 10. AI Customer Support  | Tra cứu đơn hàng, giải đáp chính sách, hỗ trợ yêu cầu đổi lịch, hủy hoặc hoàn tiền.              |

## 2. AI dành cho nhà cung cấp (Vendor)

| Tính năng                | Mô tả ngắn gọn                                                                            |
| ------------------------ | ----------------------------------------------------------------------------------------- |
| 11. AI Listing Copilot   | Hỗ trợ tạo tiêu đề, mô tả, điểm nổi bật, FAQ và nội dung giới thiệu dịch vụ.              |
| 12. AI Smart Reply       | Gợi ý câu trả lời tự động cho tin nhắn khách hàng dựa trên thông tin dịch vụ và lịch đặt. |
| 13. AI Review Insights   | Phân tích đánh giá để xác định điểm mạnh, điểm yếu và đề xuất cải thiện dịch vụ.          |
| 14. AI Business Insights | Phân tích doanh thu, số đơn, tỷ lệ hủy, hiệu quả dịch vụ và gợi ý cải thiện kinh doanh.   |

## 3. AI dành cho quản trị viên (Admin)

| Tính năng                        | Mô tả ngắn gọn                                                                                   |
| -------------------------------- | ------------------------------------------------------------------------------------------------ |
| 15. AI Content Moderation        | Phát hiện văn bản dịch vụ và đánh giá có dấu hiệu vi phạm; chuyển admin duyệt.                            |
| 16. AI Fraud Detection           | Phát hiện dấu hiệu bất thường trong giao dịch, tài khoản, nhà cung cấp hoặc đánh giá.            |
| 17. AI Complaint Analysis        | Tự động phân loại, tóm tắt khiếu nại và đề xuất hướng xử lý để admin xem xét.                    |
| 18. AI Provider Quality Analysis | Phân tích chất lượng nhà cung cấp dựa trên đánh giá, khiếu nại, tỷ lệ hủy và hiệu suất vận hành. |

## 4. Nên ưu tiên triển khai tính năng nào?

Để phù hợp với phạm vi đồ án và công nghệ DANASEA (Spring Boot, React, Flutter, PostgreSQL), mình đề xuất:

Giai đoạn 1 — AI cốt lõi

Ưu tiên cao

- AI Travel Assistant
- AI Smart Search
- AI Recommendation
- AI Itinerary Planner
- Weather-Aware AI
- AI Service Q&A (hỏi đáp theo dữ liệu dịch vụ)

  Giai đoạn 2 — AI nâng cao
- AI Re-planning
- AI Service Comparison
- AI Review Summary
- AI Nearby Discovery
- AI Listing Copilot
- AI Smart Reply

  Giai đoạn 3 — AI vận hành và phân tích
- AI Business Insights
- AI Review Insights
- AI Customer Support
- AI Content Moderation
- AI Fraud Detection
- AI Complaint Analysis
- AI Provider Quality Analysis


Kết luận: DANASEA có thể phát triển 18 tính năng AI chính, nhưng không cần triển khai toàn bộ ngay. Bộ 6 tính năng AI cốt lõi ở giai đoạn 1 là đủ để tạo ra một hệ thống có giá trị thực tế và thể hiện rõ sự khác biệt so với website đặt dịch vụ truyền thống.

Quan trọng nhất: AI phải sử dụng dữ liệu thực từ DANASEA; hệ thống nghiệp vụ vẫn là nơi quyết định tồn chỗ, thanh toán, quyền truy cập và điều kiện an toàn.

## 5. Phạm vi triển khai đã chốt (08/10/2026)

Backend đã triển khai contract cho 10 tính năng khách hàng, phân loại/kiểm duyệt văn bản và hàng chờ dấu hiệu giao dịch bất thường. Quyet Small phục vụ quyết định text/JSON; Groq tiếp tục phục vụ hội thoại. Florence và xử lý ảnh không nằm trong runtime. Các nhóm Vendor/provider-quality còn là đề xuất. Phần triển khai lần này chỉ sửa backend; web/mobile và các màn hình mô phỏng không được nối vào API thật.

Discovery và recommendation chỉ trả tối đa 15 dịch vụ phù hợp sau khi lọc option ACTIVE, báo giá theo số người và slot còn chỗ. Retrieval có thể trả `PARTIAL`, `nextOffset` và limitation khi chưa quét hết catalog hoặc weather phải revalidate; `NO_MATCHES` không đồng nghĩa toàn hệ thống không có dịch vụ.

Chat trả thêm `cards`, `sources`, `actions`, `requiredInputs`, `context` và `generatedTextVerified=false`; phần văn bản hội thoại không được xem là nguồn dữ kiện. Chat và xác nhận có idempotency theo actor. Planner dùng preview, lifecycle, revision/CAS và proposal replan; không sửa booking, payment hoặc refund.

Review summary tách thống kê toàn bộ review public hợp lệ khỏi mẫu NLP đại diện, invalidation theo fingerprint nguồn. Support có preview và request hỗ trợ thủ công với trạng thái/idempotency; admin xử lý queue. Không có AI tự đổi lịch, hoàn tiền hay thay trạng thái đơn.

Xem [API, hành vi và giới hạn thực tế](AI-IMPLEMENTATION.md), [cấu hình worker local](../ai-decision/README.md) và [bằng chứng kiểm tra](AI-VALIDATION.md). Recommendation hiện dùng baseline theo context với Quyet hỗ trợ; replan là API theo yêu cầu; kiểm duyệt/fraud là gợi ý cho người duyệt, không tự thay trạng thái nguồn hoặc tiền.
