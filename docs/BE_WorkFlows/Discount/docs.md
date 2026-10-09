# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE DISCOUNT (QUẢN LÝ KHUYẾN MÃI & CHIẾT KHẤU)

> Module Discount chịu trách nhiệm quản lý toàn bộ vòng đời của mã giảm giá (voucher), kiểm tra tính hợp lệ và xem trước chiết khấu khi checkout, phân bổ chính xác theo giải thuật **Largest Remainder (Hare-Niemeyer)**, khóa bi quan (Pessimistic Locking) chống vượt hạn mức (Over-redemption), tự động giải phóng quota khi đơn bị hủy/hết hạn, và tích hợp sâu với luồng Hoàn tiền (Refund) và Đối soát (Settlement) đảm bảo dòng tiền tài chính chính xác tuyệt đối 100%.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Discount_Code_Management_And_Validation` | Sequence Diagram | Quản lý mã giảm giá Admin & Vendor (`GET/POST/PATCH`): kiểm tra toàn vẹn nghiệp vụ qua `DiscountCodeRules`, phân lập phạm vi tài trợ (`sponsor_type`: `PLATFORM` vs `VENDOR`), bảo vệ chống IDOR và ngăn chặn vendor chỉnh sửa voucher do sàn tài trợ. |
| 2 | `Discount_Checkout_Preview_And_Eligibility` | Sequence Diagram | Xem trước giảm giá khi đặt đơn (`POST /api/checkout/discount-preview`): kiểm tra trạng thái booking `HOLD`, tính hợp lệ thời gian/quota tổng/quota user, đánh giá ứng viên theo scope, tính toán phân bổ dự kiến, chặn voucher giảm 100% (`DISCOUNT_ZERO_PAYABLE_UNSUPPORTED`) và trả thông điệp đa ngôn ngữ (i18n). |
| 3 | `Discount_Order_Creation_Allocation_And_Locking` | Sequence Diagram | Khởi tạo đơn hàng có áp dụng mã (`POST /api/orders`): kiểm tra Idempotency & IDOR, khóa bi quan Voucher trên PostgreSQL (`findByCodeForUpdate`), kiểm tra quota, chạy giải thuật Hare-Niemeyer, tách SubOrders với `finalAmount` & `commissionBasisAmount`, tăng `usedCount` nguyên tử và ghi nhận `DiscountRedemption`. |
| 4 | `Discount_Reservation_Release_On_Cancel_Or_Expiry` | Sequence Diagram | Giải phóng và hoàn trả quota voucher tự động (`DiscountReservationService`): kích hoạt khi khách hủy booking chưa thanh toán hoặc khi hết hạn giữ chỗ 15 phút (`BookingHoldExpiredEvent`), xóa bản ghi redemption nguyên tử và giảm `usedCount`, chống hoàn trả trùng lặp (Idempotent). |
| 5 | `Discount_Financial_Impact_Refund_And_Settlement` | Sequence Diagram | Tác động tài chính liên module: Hoàn tiền (Refund) chỉ hoàn tiền mặt thực trả của khách (`finalAmount`), không hoàn tiền phần voucher; Đối soát (Settlement) tính hoa hồng dựa trên cơ sở tài trợ (`effectiveCommissionBasis`), xử lý hoàn tiền một phần bằng cơ chế đảo ngược trợ giá theo tỷ lệ. |

---

## 1. Discount_Code_Management_And_Validation.png — Quản Lý Mã Giảm Giá & Quy Tắc Ràng Buộc

**Luồng nghiệp vụ chi tiết:**
1. **Admin quản lý mã toàn sàn hoặc tài trợ (`POST/PATCH /api/admin/discount-codes`):**
   - Admin có quyền tạo mã toàn sàn (`scope = PLATFORM`, `sponsorType = PLATFORM`), mã tài trợ riêng cho từng nhà cung cấp (`scope = VENDOR`, `sponsorType = PLATFORM`), hoặc mã tài trợ theo dịch vụ cụ thể (`scope = SERVICE`, `serviceId != null`).
   - Lớp kiểm tra ràng buộc `DiscountCodeRules.validate(codeEntity)` thực thi các quy tắc:
     - `scope`, `discountType`, `discountValue` bắt buộc dương (`> 0`).
     - Với loại giảm `%` (`PERCENTAGE`): `discountValue` không được vượt quá `100%` (`DISCOUNT_PERCENTAGE_INVALID`).
     - Khoảng thời gian hiệu lực: `validTo` bắt buộc phải sau `validFrom` (`DISCOUNT_DATE_RANGE_INVALID`).
     - Khi `scope = VENDOR`: bắt buộc cung cấp `vendorId` (`VENDOR_ID_REQUIRED`).
     - Khi `scope = PLATFORM`: không được gán `vendorId`.
     - Khi `sponsorType = VENDOR`: bắt buộc `scope = VENDOR` (`DISCOUNT_SPONSOR_SCOPE_INVALID`).
     - Các hạn mức `minOrderAmount >= 0`, `maxDiscountAmount > 0`, `maxUses > 0`, `maxUsesPerUser > 0`.
2. **Vendor tự tạo mã giảm giá (`POST /api/vendor/discount-codes`) — Chống IDOR:**
   - Hệ thống tự động gán `vendorId = currentVendorId`, `sponsorType = VENDOR` (Vendor tự chịu chi phí giảm giá) và `scope = VENDOR`.
   - Nếu áp dụng cho dịch vụ cụ thể (`serviceId != null`), usecase truy vấn `JpaServiceRepository` để xác thực dịch vụ đó thuộc quyền sở hữu của vendor. Nếu sai, ném ngay ngoại lệ `400 Bad Request` (`DISCOUNT_SERVICE_INVALID`).
3. **Vendor cập nhật mã giảm giá (`PATCH /api/vendor/discount-codes/{id}`):**
   - Áp dụng khóa bi quan `codes.findByIdForUpdate(codeId)`.
   - **Chống IDOR & Bảo vệ quyền sở hữu:** Nếu mã thuộc vendor khác hoặc mã do sàn tài trợ (`code.vendorId != currentVendorId || code.sponsorType != VENDOR`), hệ thống chặn đứng với mã lỗi **HTTP 403 Forbidden**.
   - Cập nhật thông tin và gọi lại `DiscountCodeRules.validate(mergedEntity)` để bảo đảm dữ liệu sau khi sửa vẫn thỏa mãn toàn vẹn nghiệp vụ.

---

## 2. Discount_Checkout_Preview_And_Eligibility.png — Xem Trước Giảm Giá Khi Đặt Đơn (Preview)

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu xem trước qua `POST /api/checkout/discount-preview` kèm `{bookingId, discountCode}`.
2. **Kiểm tra Booking & IDOR Guard:**
   - Tra cứu qua `BookingLookupPort.findBookingForOrder(bookingId)`.
   - Xác thực quyền sở hữu: `booking.customerId == currentUserId` (403 Forbidden nếu không khớp).
   - Kiểm tra trạng thái Booking: Bắt buộc ở trạng thái `HOLD` và thời điểm hiện tại `now < holdExpiresAt`. Nếu không thỏa mãn, trả về `valid = false` với mã lỗi `BOOKING_NOT_ELIGIBLE_FOR_ORDER`.
3. **Đánh giá điều kiện mã giảm giá:**
   - Tra cứu mã giảm giá qua `codes.findByCodeIgnoreCase(discountCode)`. Nếu không thấy → `DISCOUNT_NOT_FOUND`.
   - Kiểm tra cờ kích hoạt: `code.isActive == false` → `DISCOUNT_INACTIVE`.
   - Kiểm tra khoảng thời gian: `now < validFrom` → `DISCOUNT_NOT_STARTED`; `now > validTo` → `DISCOUNT_EXPIRED`.
   - Kiểm tra hạn mức toàn sàn: `code.usedCount >= code.maxUses` → `DISCOUNT_QUOTA_EXCEEDED`.
   - Kiểm tra hạn mức mỗi khách hàng: Truy vấn bảng `discount_redemptions` qua `redemptions.countByDiscountCodeIdAndCustomerId(codeId, customerId)`. Nếu `usageCount >= code.maxUsesPerUser` → `DISCOUNT_USER_LIMIT_EXCEEDED`.
4. **Đánh giá ứng viên & Phân bổ tạm tính:**
   - Lọc các `BookingItem` đủ điều kiện theo `scope` (mã toàn sàn, mã theo vendor, mã theo dịch vụ).
   - Kiểm tra giá trị đơn tối thiểu (`minOrderAmount`).
   - Chạy công cụ phân bổ `DiscountAllocationEngine.evaluateAndAllocate(...)`.
   - **Ràng buộc thanh toán tối thiểu:** Nếu `finalPayableAmount <= 0` (voucher giảm 100% hoặc vượt quá tiền vé), hệ thống từ chối với mã lỗi `DISCOUNT_ZERO_PAYABLE_UNSUPPORTED` (bắt buộc khách phải trả số tiền dương).
5. **Hỗ trợ Đa ngôn ngữ (i18n):**
   - Tự động phát hiện ngôn ngữ qua `LocaleContextHolder` (tiêu đề `Accept-Language: vi` hoặc `en`) để dịch thông điệp phản hồi thân thiện và chính xác.

---

## 3. Discount_Order_Creation_Allocation_And_Locking.png — Tạo Đơn Hàng, Khóa Bi Quan & Phân Bổ

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu tạo đơn qua `POST /api/orders` kèm `{bookingId, discountCode}` và header `Idempotency-Key`.
2. **Kiểm tra Idempotency & IDOR trên mọi nhánh:**
   - Kiểm tra theo `bookingId` và `customerId + idempotencyKey`: Nếu đơn đã tồn tại, kiểm tra `order.customerId == currentUserId` (chặn 403 nếu sai chủ đơn).
3. **Khóa bi quan Voucher (Pessimistic Locking):**
   - Khi có `discountCode`, hệ thống gọi `discountCodeRepository.findByCodeForUpdate(cleanCode)`:
     ```sql
     SELECT * FROM discount_codes WHERE UPPER(code) = UPPER(?) FOR UPDATE;
     ```
   - Khóa bản ghi voucher trong PostgreSQL nhằm tuần tự hóa các giao dịch thanh toán cạnh tranh (Concurrency Race Condition), loại trừ hoàn toàn nguy cơ vượt hạn mức toàn sàn (`maxUses`) và hạn mức cá nhân (`maxUsesPerUser`).
4. **Giải thuật phân bổ Hare-Niemeyer (Largest Remainder):**
   - Hệ thống tính toán tỷ trọng tiền `subtotal` của từng BookingItem hợp lệ.
   - Số tiền giảm giá được phân bổ tỷ lệ thuận theo giá trị từng item. Phần dư làm tròn lẻ từng đồng (VND) được giải quyết triệt để bằng thuật toán Hare-Niemeyer: sắp xếp phần thập phân giảm dần và phân bổ từng đơn vị 1 VND cho đến khi tổng tiền giảm các sub-orders bằng chính xác 100% giá trị voucher.
5. **Tăng hạn mức sử dụng nguyên tử (Atomic Counter Increment):**
   - Thực thi câu lệnh UPDATE nguyên tử:
     ```sql
     UPDATE discount_codes 
     SET used_count = used_count + 1 
     WHERE id = ? AND (max_uses IS NULL OR used_count < max_uses);
     ```
   - Nếu số dòng cập nhật bằng 0, ném ngay ngoại lệ `400 Bad Request` (`DISCOUNT_QUOTA_EXCEEDED`).
6. **Lưu trữ MasterOrder & Tách SubOrders 1:1:**
   - `MasterOrder` được lưu với: `totalAmount = finalPayableAmount`, `discountCodeId`, `discountAmount`.
   - Từng `SubOrder` được tách và lưu các trường tài chính cố định:
     - `finalAmount = subtotal - (vendorDiscount + platformDiscount)`
     - `commissionBasisAmount = subtotal - vendorDiscount`
     - `commissionAmount = commissionBasisAmount * commissionRate`
     - `vendorPayoutAmount = commissionBasisAmount - commissionAmount`
7. **Lưu vết sử dụng Voucher (Discount Redemption):**
   - Lưu bản ghi vào bảng `discount_redemptions` gồm: `{discountCodeId, masterOrderId, customerId, amountDeducted}`.
8. Chuyển trạng thái Booking sang `PENDING_PAYMENT` và phát sự kiện `OrderCreatedEvent`.

---

## 4. Discount_Reservation_Release_On_Cancel_Or_Expiry.png — Giải Phóng & Hoàn Trả Quota Tự Động

**Luồng nghiệp vụ chi tiết:**
1. **Hai nguồn kích hoạt giải phóng voucher khi đơn chưa thanh toán:**
   - **Kịch bản A: Khách hàng chủ động hủy booking chưa thanh toán** (`PATCH /api/bookings/{id}/cancel`):
     - `BookingCancellationFinancialAdapter.cancelUnpaidOrder(bookingId)` gọi khóa đơn hàng `masterOrderRepository.findByBookingIdForUpdate(bookingId)`.
     - Nếu đơn hàng ở trạng thái `PENDING_PAYMENT`, cập nhật `SubOrder.status = CANCELLED`.
     - Gọi `DiscountReservationService.release(order.getId(), order.getDiscountCodeId())`.
     - Cập nhật `MasterOrder.status = CANCELLED`.
   - **Kịch bản B: Hết hạn giữ chỗ 15 phút (Timeout Expiry)**:
     - Background Job (`BookingHoldCleanupJob`) quét các booking quá hạn, bắn `BookingHoldExpiredEvent`.
     - `OrderExpiryEventListener` tiếp nhận sự kiện qua `@TransactionalEventListener(phase = AFTER_COMMIT)` với giao dịch độc lập `@Transactional(propagation = REQUIRES_NEW)`.
     - Khóa đơn hàng `findByBookingIdForUpdate`:
       - Nếu đơn đã kịp thanh toán `status == PAID`: An toàn bỏ qua (Bảo vệ đơn PAID).
       - Nếu đơn vẫn ở trạng thái `PENDING_PAYMENT`: Chuyển sang `CANCELLED`, gọi `DiscountReservationService.release(...)` và phát `OrderCancelledEvent`.
2. **Cơ chế bảo đảm Idempotency của `DiscountReservationService`:**
   - Khóa bản ghi mã giảm giá: `codes.findByIdForUpdate(codeId)`.
   - Xóa bản ghi redemption nguyên tử:
     ```sql
     DELETE FROM discount_redemptions 
     WHERE master_order_id = ? AND discount_code_id = ?;
     ```
   - **Quy tắc bảo vệ quota:** Chỉ khi câu lệnh `DELETE` xóa thành công bản ghi reservation (`deletedRows > 0`), hệ thống mới thực hiện giảm bộ đếm quota:
     ```sql
     UPDATE discount_codes 
     SET used_count = GREATEST(0, used_count - 1) 
     WHERE id = ?;
     ```
   - Nếu sự kiện bị replay hoặc gọi hủy nhiều lần, `deletedRows` sẽ bằng 0 và không có lệnh giảm nào được thực thi thêm.

---

## 5. Discount_Financial_Impact_Refund_And_Settlement.png — Tác Động Tài Chính Hoàn Tiền & Đối Soát

**Luồng nghiệp vụ chi tiết:**
1. **Tác động Hoàn tiền (Refund Impact):**
   - Áp dụng trên cả 4 kịch bản hoàn tiền:
     1. Khách hủy đơn đã thanh toán: `BookingCancellationFinancialAdapter.requestRefund`
     2. Vendor từ chối đơn hàng: `OrderPaymentService.rejectVendorBooking`
     3. Cảnh báo thời tiết tự động hủy / Admin resolve alert: `SlotWeatherMonitoringJob` & `AdminWeatherAlertController`
     4. Tranh chấp khiếu nại được duyệt hoàn tiền: `ResolveDisputeUseCase`
   - **Nguyên tắc vàng:** Hệ thống luôn tính toán chính sách hoàn tiền dựa trên `subOrder.getFinalAmount()`:
     ```java
     RefundEvaluationResult evaluation = refundPolicyEngine.evaluate(
         reason, departureTime, now, subOrder.getFinalAmount()
     );
     ```
   - Khách hàng chỉ nhận lại tối đa số tiền mặt thực tế đã thanh toán; phần chiết khấu/trợ giá của voucher tuyệt đối không được quy đổi thành tiền mặt để hoàn lại.
   - Nếu `finalAmount == 0` hoặc số tiền hoàn `<= 0`, hệ thống không tạo bản ghi Refund chuyển sang cổng thanh toán.
2. **Tác động Quyết toán Đối soát (Settlement Impact):**
   - `SettlementCalculationEngine` thực hiện tính toán kỳ quyết toán cho Vendor dựa trên `SubOrderCalculationContext`:
     - Cơ sở tính hoa hồng: `effectiveCommissionBasis = subtotal - vendorDiscountAmount`.
     - **Voucher do Vendor tài trợ (`sponsor_type = VENDOR`)**:
       - Vendor chịu chi phí khuyến mãi của mình.
       - Cơ sở hoa hồng bị giảm tương ứng: `commissionBasisAmount = subtotal - vendorDiscount`.
       - Hoa hồng sàn thu: `commissionAmount = commissionBasisAmount * commissionRate`.
       - Payout chi trả cho Vendor: `vendorPayout = commissionBasisAmount - commissionAmount`.
     - **Voucher do Sàn tài trợ (`sponsor_type = PLATFORM`)**:
       - Sàn tài trợ giảm giá để kích cầu du lịch.
       - Cơ sở hoa hồng giữ nguyên giá niêm yết: `commissionBasisAmount = subtotal`.
       - Hoa hồng sàn thu: `commissionAmount = subtotal * commissionRate`.
       - Payout chi trả cho Vendor: `vendorPayout = subtotal - commissionAmount`. Vendor nhận đủ 100% doanh thu thuần như thể không có voucher.
       - Sàn tự bù đắp phần voucher tài trợ từ nguồn hoa hồng của sàn.
     - **Kịch bản Hoàn tiền 100% (`REFUNDED`)**:
       - `grossAmount = 0`, `commissionAmount = 0`, `netAmount = 0`, `refundAmount = customerPaid`.
     - **Kịch bản Hoàn tiền một phần (`PARTIALLY_REFUNDED`)**:
       - Đảo ngược trợ giá của sàn theo đúng tỷ lệ tiền khách thực nhận hoàn:
         ```java
         BigDecimal reversedBasis = customerPaid.signum() > 0 
             ? basis.multiply(refund).divide(customerPaid, SCALE, ROUNDING) 
             : basis;
         BigDecimal netAfterRefund = basis.subtract(reversedBasis).max(BigDecimal.ZERO);
         ```
       - `grossAmount = netAfterRefund`, hoa hồng và payout được tính lại chính xác trên phần doanh thu thực còn giữ lại.
