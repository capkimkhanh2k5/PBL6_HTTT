# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE WEATHER SAFETY

> Folder này chứa **5 sơ đồ** mô tả toàn bộ cơ chế kiểm tra an toàn thời tiết, vòng đời cảnh báo, và quy trình hoàn tiền tự động cho hệ thống DANASEA.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `AdvanceBookSafetyCheck` | Sequence Diagram | Kiểm tra an toàn thời tiết trước đặt chỗ (Cache → Open-Meteo API song song → WeatherRuleEngine → GREEN/YELLOW/RED) |
| 2 | `DynamicSafety_FallBack500` | Sequence Diagram | Admin cập nhật ngưỡng an toàn + Zero-500 Fallback (Cache → DB → Built-in Default Registry ASTM/PADI) |
| 3 | `SlidingWindows_SafetyAsset` | Flowchart | Quét chu kỳ 30 phút, leo thang cảnh báo YELLOW → RED, auto-escalation hủy + hoàn tiền khi T ≤ 60 phút |
| 4 | `WeatherAlertLifeCycle` | State Machine | Vòng đời cảnh báo: `MONITORING_YELLOW` → `RESOLVED_NORMAL` / `AWAITING_ADMIN_RESOLUTION` → 4 trạng thái kết thúc |
| 5 | `Weather_ReturnMoney` | Flowchart | Cron Job T-24h & T-2h → đánh giá vi phạm → Admin duyệt hủy → hoàn tiền 100% |

---

## 1. AdvanceBookSafetyCheck.png — Kiểm tra an toàn trước khi đặt chỗ

**Thành phần:** `Khách Hàng` → `WeatherController` → `WeatherForecastCacheService` → `OpenMeteoApiClient` → `Open-Meteo API` → `CategorySafetyRuleService` → `WeatherRuleEngine`

**Luồng:**
1. Khách gọi `GET /api/v1/weather/advance-safety-check?latitude=...&longitude=...&categoryId=...&targetDateTime=...`
2. Kiểm tra **Cache 2 tầng** (Redis + Local):
   - **Hit:** Trả dữ liệu cache ngay.
   - **Miss:** Gọi **song song** 2 API Open-Meteo:
     - API Khí tượng: `forecast_days=16` (nhiệt độ, mưa, gió giật, tầm nhìn, mã WMO)
     - API Hải văn: `forecast_days=8` (chiều cao sóng, chu kỳ, sóng lừng, dòng hải lưu)
   - Hợp nhất → `WeatherInfoDto` → lưu Cache.
3. Lấy `CategorySafetyRule` theo `categoryId`.
4. `WeatherRuleEngine.evaluateSafety()` → trả `SafetyEvaluationResult`:
   - Mức an toàn: **GREEN / YELLOW / RED**
   - Chi tiết sóng, gió, tầm nhìn
   - Khuyến cáo: *"Dự báo xa mang tính tham khảo, sẽ tái kiểm tra tại T-24h & T-2h"*

---

## 2. DynamicSafety_FallBack500.png — Ngưỡng an toàn động & Fallback Zero-500

**2 luồng chính:**

### Luồng 1 — Admin cập nhật ngưỡng:
1. `PATCH /api/admin/category-safety-rules/{categoryId}` (sóng, gió, tầm nhìn...)
2. Validate nghiệp vụ (không âm, Caution ≤ Max)
3. `UPDATE` PostgreSQL → `@CacheEvict` xóa cache cũ
4. HTTP 200 → ngưỡng có hiệu lực **tức thì** trên toàn hệ thống

### Luồng 2 — Truy vấn ngưỡng (Zero-500 Fallback):
1. `getRuleByCategoryId()` → kiểm tra Cache trước
2. **Cache Hit:** trả Rule ngay
3. **Cache Miss:** query PostgreSQL
   - DB OK → lưu Cache
   - **DB lỗi / mất kết nối / rỗng** → kích hoạt **Zero-500 Fallback**: lấy Rule cứng mặc định từ `Built-in Default Registry` (ASTM/PADI/ACA)
4. Kết quả: **luôn trả Rule, không bao giờ ném lỗi 500**

---

## 3. SlidingWindows_SafetyAsset.png — Quét chu kỳ & Leo thang cảnh báo

**Luồng (mỗi 30 phút):**
1. Quét tất cả Booking Slot sắp diễn ra
2. Phân luồng theo thời gian khởi hành T:
   - `T ∈ [now+23h, now+25h]` → mốc **T-24h**
   - `T ∈ [now+1h, now+3h]` → mốc **T-2h**
3. Lấy dự báo (Cache/API) + Rule an toàn (Cache/DB/Fallback)
4. `WeatherRuleEngine` đánh giá → phân nhánh:

| Kết quả | Xử lý |
|---------|-------|
| **GREEN** | Cập nhật trạng thái an toàn, không cảnh báo |
| **YELLOW** (chưa có alert) | Tạo `MONITORING_YELLOW` + gửi Early Warning cho Khách & Vendor |
| **YELLOW** (đã có alert) | Cập nhật in-place, **không spam** thông báo |
| **RED** (chưa có / đang YELLOW) | Leo thang → `AWAITING_ADMIN_RESOLUTION` + thông báo khẩn cấp 3 bên |
| **RED** (đã chờ duyệt) | Cập nhật in-place, giữ trạng thái chờ |

5. **Auto-Escalation:** Nếu Alert RED mà `T ≤ 60 phút` & Admin chưa xử lý:
   - Hủy Slot: `SubOrderStatus.CANCELLED`
   - Hoàn tiền 100%: `RefundReason:WEATHER`
   - Đánh dấu: `AUTO_CANCELLED_FOR_SAFETY`
   - Thông báo khẩn cấp 3 bên

---

## 4. WeatherAlertLifeCycle.png — Vòng đời cảnh báo thời tiết

**Máy trạng thái:**

```
[Thời tiết chạm ngưỡng vàng / đỏ ngay từ đầu]
              │
              ▼
    MONITORING_YELLOW  ◀── quét lại 30p vẫn vàng (in-place)
         │          │
     Tốt lên     Xấu đi
         │          │
         ▼          ▼
  RESOLVED_NORMAL   AWAITING_ADMIN_RESOLUTION  ◀── quét lại 30p vẫn đỏ (in-place)
                         │         │         │
                    Admin HỦY   Admin BỎ QUA  T≤60p chưa xử lý
                         │         │         │
                         ▼         ▼         ▼
               RESOLVED_CANCELLED  RESOLVED_OVERRIDDEN  AUTO_CANCELLED_FOR_SAFETY
               (Hủy + Hoàn 100%)  (Cho phép tiếp tục)  (Auto hủy + Hoàn 100%)
```

| Trạng thái kết thúc | Ý nghĩa |
|---|---|
| `RESOLVED_NORMAL` | Thời tiết cải thiện → GREEN, hủy cảnh báo |
| `RESOLVED_CANCELLED` | Admin duyệt hủy tour + hoàn tiền 100% |
| `RESOLVED_OVERRIDDEN` | Admin đánh giá thực địa an toàn, cho phép tiếp tục |
| `AUTO_CANCELLED_FOR_SAFETY` | Hệ thống tự hủy khi T ≤ 60 phút mà Admin không phản hồi |

---

## 5. Weather_ReturnMoney.png — Hoàn tiền khi thời tiết nguy hiểm

**Luồng:**
1. **Cron Job** chạy tại T-24h và T-2h
2. Trích xuất dự báo khung giờ `[startTime, endTime]`
3. `WeatherRuleEngine` đánh giá vi phạm:
   - **GREEN/YELLOW:** Cho phép hoạt động → gửi cảnh báo Vendor & Khách (nếu YELLOW)
   - **RED (Nguy hiểm):**
     1. Tạo `SafetyRuleEvaluation (isSafe=false)`
     2. Hiển thị cảnh báo trên Admin Portal
     3. Admin xem xét:
        - **Cho phép:** Gửi cảnh báo → kết thúc
        - **Duyệt HỦY:**
          - Chuyển SubOrder → `CANCELLED`
          - Tạo `RefundJpaEntity`: hoàn tiền 100% (Reason: WEATHER)
          - Gửi email/in-app xác nhận hoàn tiền

---

## 🔗 Mối liên hệ giữa các sơ đồ

- **Sơ đồ 1** → Khách kiểm tra an toàn **trước khi đặt chỗ** (on-demand)
- **Sơ đồ 2** → Đảm bảo bộ ngưỡng luôn khả dụng (cung cấp Rule cho sơ đồ 1 & 3)
- **Sơ đồ 3** → Quét **sau khi đã đặt chỗ**, giám sát liên tục & leo thang cảnh báo
- **Sơ đồ 4** → Vòng đời trạng thái của mỗi cảnh báo
- **Sơ đồ 5** → Luồng nghiệp vụ khi Admin duyệt hủy + hoàn tiền tự động
