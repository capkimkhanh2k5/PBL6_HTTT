
# BÁO CÁO NGHIÊN CỨU TOÀN DIỆN VỀ ĐIỀU KIỆN AN TOÀN THỜI TIẾT CHO CÁC TRÒ CHƠI BIỂN (DANASEA SYSTEM)

---

## PHẦN 1: TỔNG HỢP CÁC DANH MỤC TRÒ CHƠI BIỂN TRONG HỆ THỐNG


Hệ thống **DANASEA** hiện quy định **10 trò chơi/hoạt động trải nghiệm biển** trọng tâm (được phân thành 6 nhóm danh mục chính trên cơ sở dữ liệu và giao diện):

| STT | Tên trò chơi / Trải nghiệm | Mã danh mục (`slug`) | Yêu cầu chứng chỉ an toàn (`requires_safety_cert`) | Đặc thù vận hành tại vùng biển Đà Nẵng |
| :--- | :--- | :--- | :---: | :--- |
| **1** | **Chèo ván đứng (SUP)** | `cheo-sup-kayak` | Có | Khu vực Bãi Bụt, Mân Thái, Mỹ Khê. Thường tổ chức sáng sớm (đón bình minh) hoặc chiều mát. |
| **2** | **Chèo thuyền Kayak** | `cheo-sup-kayak` | Có | Vùng vịnh lặng sóng quanh Bán đảo Sơn Trà. |
| **3** | **Lặn ngắm san hô (Snorkeling & Scuba Diving)** | `lan-ngam-san-ho` | Có | Khu vực Hòn Sụp, Mũi Nghê, Bãi Rạng (Sơn Trà) hoặc Cù Lao Chàm. Cần nước trong, dòng chảy yếu. |
| **4** | **Đi bộ dưới biển (Seawalker)** | `lan-ngam-san-ho` | Có | Đi trên thềm san hô với mũ dưỡng khí đặc chủng, yêu cầu độ sâu nông và đáy biển ổn định. |
| **5** | **Dù bay biển / Dù kéo bằng cano (Parasailing)** | `cano-du-bay` | Có | Hoạt động ở độ cao 50m – 80m trên mặt biển; cực kỳ nhạy cảm với gió giật và đối lưu khí quyển. |
| **6** | **Cano cao tốc / Cano lướt sóng (Speedboat)** | `cano-du-bay` | Có | Vận chuyển khách tham quan quanh bán đảo, Mũi Nghê, lướt sóng tốc độ cao. |
| **7** | **Mô tô nước (Jetski)** | `mo-to-nuoc-jetski` | Có | Chạy tự do trong vùng nước giới hạn phao tiêu tại Bãi Bụt, Mỹ Khê; công suất động cơ lớn. |
| **8** | **Phao chuối / Phao trượt cảm giác mạnh (Banana Boat)** | `truot-phao-chuoi` | Có | Phao bơm hơi do cano kéo với tốc độ cao, rủi ro lật văng hành khách xuống biển lớn. |
| **9** | **Du thuyền ngắm hoàng hôn vịnh (Catamaran / Yacht)** | `du-thuyen-ngam-hoang-hon` | Có | Xuất phát từ bến DHC Marina du ngoạn sông Hàn ra Vịnh Đà Nẵng; phục vụ ngắm cảnh, tiệc nhẹ. |
| **10** | **Câu cá & Câu mực đêm** | `cau-ca-cau-muc` *(tài liệu docx)* | Có | Diễn ra vào ban đêm ở vùng biển mở gần bán đảo; tầm nhìn hạn chế, rủi ro mưa giông cục bộ cao. |

---

## PHẦN 2: DỮ LIỆU ĐƯỢC MODULE THỜI TIẾT TRẢ VỀ TRONG BACKEND

Qua kiểm tra lớp tích hợp thời tiết tại:
- [`OpenMeteoApiClient.java`]
- [`WeatherInfoDto.java`]
- [`FetchWeatherJob.java`](định kỳ 30 phút quét dữ liệu tọa độ Biển Mân Thái: `16.0890°N, 108.2496°E`)

Hệ thống đang thu thập **2 tập dữ liệu chuyên sâu từ Open-Meteo**:

### 1. Dữ liệu khí tượng đất liền/bờ biển (`WeatherData`):
*   `temperature`: Nhiệt độ không khí (°C)
*   `precipitation`: Lượng mưa tức thời (mm)
*   `precipitationProbability`: Xác suất mưa (%)
*   `windSpeed`: Tốc độ gió duy trì (km/h) ở độ cao 10m
*   `windGust`: **Tốc độ gió giật (km/h)** *(thông số cực kỳ nguy hiểm cho dù lượn, cano, SUP)*
*   `windDirection`: Hướng gió (độ góc 0-360°)
*   `visibility`: Tầm nhìn ngang (mét)
*   `cloudCover`: Tỷ lệ che phủ của mây (%)
*   `weatherCode`: **Mã thời tiết WMO** *(cho phép phát hiện giông bão mã 95, 96, 99 có sấm sét)*
*   `uvIndex`: Chỉ số cực tím

### 2. Dữ liệu hải văn / đại dương (`MarineData`):
*   `waveHeight`: **Chiều cao sóng biển có ý nghĩa ($H_s$) tính bằng mét (m)**
*   `waveDirection`: Hướng sóng (độ)
*   `wavePeriod`: Chu kỳ sóng (giây - chu kỳ dài biểu hiện sóng lừng nguy hiểm)
*   `swellHeight`: Chiều cao sóng lừng ngầm (m)
*   `swellDirection` & `swellPeriod`: Hướng và chu kỳ sóng lừng
*   `oceanCurrentVelocity`: **Vận tốc dòng chảy / hải lưu (m/s)**
*   `oceanCurrentDirection`: Hướng dòng chảy (độ)
*   `seaLevelHeight`: Mực nước biển / thủy triều (m)
*   `seaSurfaceTemperature`: Nhiệt độ bề mặt nước biển (°C)


## PHẦN 3: NGHIÊN CỨU QUY CHUẨN AN TOÀN QUỐC TẾ & VIỆT NAM CHO TỪNG TRÒ CHƠI

Để đưa ra các con số chính xác, minh bạch và có kiểm chứng pháp lý/khoa học, chúng tôi đã tổng hợp từ các nguồn tiêu chuẩn sau:
1. **Tiêu chuẩn Lặn biển quốc tế:** PADI Safety Standards & Procedures, Divers Alert Network (DAN), NOAA Diving Manual.
2. **Tiêu chuẩn Dù bay biển quốc tế:** **ASTM F3099** (*Standard Practices for Parasailing*), Hiệp hội Thể thao Dưới nước Hoa Kỳ (**WSIA**), Đạo luật An toàn Dù bay Florida (White-Miskell Act).
3. **Tiêu chuẩn Chèo thuyền thể thao:** American Canoe Association (**ACA**), International Surfing Association (**ISA**).
4. **Quy chuẩn Pháp luật Việt Nam:**
   - **Nghị định số 48/2019/NĐ-CP**: Quản lý hoạt động của phương tiện phục vụ vui chơi, giải trí dưới nước.
   - **Thông tư số 17/2018/TT-BVHTTDL**: Quy định điều kiện hoạt động môn Mô tô nước trên biển.
   - **QCVN 72:2013/BGTVT**: Quy chuẩn kỹ thuật quốc gia về phân cấp và đóng tàu thủy cao tốc.
   - **Quy chế phối hợp của Cảng vụ Hàng hải Đà Nẵng & Ban Quản lý Bán đảo Sơn Trà & các bãi biển du lịch Đà Nẵng**: Quy định điều kiện cấm biển khi có thời tiết nguy hiểm.
5. **Thang đo quốc tế:** Thang sức gió Beaufort (Cấp 0 – 12) và Thang trạng thái biển Douglas Sea State (Cấp 0 – 9).

---

### MA TRẬN QUY TẮC AN TOÀN CHI TIẾT THEO TỪNG TRÒ CHƠI (KIỂM CHỨNG)

Dưới đây là ma trận đối chiếu 3 cấp độ an toàn:
- 🟢 **GREEN (An toàn vận hành bình thường):** Điều kiện lý tưởng cho cả người mới bắt đầu.
- 🟡 **YELLOW (Cảnh báo thận trọng):** Chỉ người có kinh nghiệm/sức khỏe tốt, bắt buộc có huấn luyện viên 1 kèm 1 và phao cứu sinh chuyên dụng.
- 🔴 **RED (Cấm vận hành / Tự động hủy đơn):** Vượt ngưỡng an toàn hàng hải, nguy hiểm đến tính mạng.

---

### 1. Lặn ngắm san hô (Snorkeling, Scuba Diving) & Đi bộ dưới biển (Seawalker)
*Nguồn đối chiếu: PADI General Standards, DAN Safety Guidelines, NOAA Marine Conditions.*
*   **Đặc thù:** Cần mặt biển êm để tàu cano thả neo/đón khách an toàn; sóng ngầm và dòng chảy mạnh là nguyên nhân số 1 gây trôi dạt, say sóng dưới nước và hoảng loạn cho du khách.
*   **Các ngưỡng kỹ thuật:**
    *   🟢 **GREEN (Lý tưởng):** Sóng $H_s \le 0.8\text{m}$; Gió duy trì $< 15\text{km/h}$; Dòng chảy $< 0.25\text{m/s}$ (< 0.5 knot); Tầm nhìn nước $> 8\text{m}$.
    *   🟡 **YELLOW (Cảnh báo):** Sóng $H_s$ từ $0.8\text{m} - 1.2\text{m}$; Gió $15 - 25\text{km/h}$ (Beaufort cấp 3-4); Dòng chảy $0.25 - 0.5\text{m/s}$ (cần hướng dẫn viên 1 kèm 1, hạn chế lặn cho người mới/trẻ em).
    *   🔴 **RED (Cấm lặn):** 
        *   Sóng biển $H_s > 1.2\text{m}$ (đối với Snorkeling và Seawalker) hoặc $H_s > 1.5\text{m}$ (đối với Scuba Diving).
        *   Tốc độ dòng chảy hải lưu $> 0.5\text{m/s}$ (xấp xỉ 1 knot).
        *   Gió duy trì $> 28\text{km/h}$ hoặc gió giật $> 35\text{km/h}$.
        *   Tầm nhìn dưới nước kém $< 3\text{m}$.
        *   Có hiện tượng sấm sét hoặc mưa giông (WMO Code $\ge 95$).

---

### 2. Dù bay biển kéo bằng cano (Parasailing)
*Nguồn đối chiếu: Tiêu chuẩn quốc tế ASTM F3099, WSIA Parasail Safety Regulations.*
*   **Đặc thù:** Đây là trò chơi có rủi ro khí động học cao nhất. Dù bay biến người tham gia thành một cánh buồm lớn ở độ cao 80m. Gió giật bất ngờ có thể làm đứt dây kéo (towline), giật lật cano hoặc cuốn du khách vào bờ/vách đá Sơn Trà.
*   **Các ngưỡng kỹ thuật:**
    *   🟢 **GREEN (An toàn):** Sóng $H_s \le 0.6\text{m}$; Gió duy trì $10 - 20\text{km/h}$ (đủ sức nâng dù nhưng êm); Gió giật $\le 25\text{km/h}$; Tầm nhìn $> 4000\text{m}$.
    *   🟡 **YELLOW (Cảnh báo):** Sóng $H_s$ từ $0.6\text{m} - 0.9\text{m}$; Gió duy trì $20 - 28\text{km/h}$; Gió giật $28 - 35\text{km/h}$ (chỉ bay dù đơn, giảm chiều cao dây kéo).
    *   🔴 **RED (Cấm bay hoàn toàn):**
        *   **Gió duy trì $> 28\text{km/h}$ (15 knots) HOẶC gió giật (wind gust) $> 37\text{km/h}$ (20 knots)** *(Quy chuẩn bắt buộc theo ASTM F3099)*.
        *   Chiều cao sóng biển $H_s > 1.0\text{m}$ (cano không thể duy trì tốc độ và độ ổn định kéo dù).
        *   Tầm nhìn ngang $< 1800\text{m}$ (1 hải lý).
        *   Mưa hoặc mây đối lưu tích điện có khả năng giông sét trong bán kính 10km (sét đánh vào dây kéo kim loại/dù ẩm ướt).

---

### 3. Chèo ván đứng (SUP) & Chèo thuyền Kayak
*Nguồn đối chiếu: American Canoe Association (ACA) Paddlesports Safety, ISA Guidelines.*
*   **Đặc thù:** Thuyền nhẹ, vận tốc di chuyển bằng sức người rất chậm (chỉ khoảng 3 - 5 km/h). Gió ngoài khơi (offshore wind - thổi từ bờ ra biển) là "kẻ giết người thầm lặng" đẩy người chèo ra xa bờ mà không thể bơi ngược vào.
*   **Các ngưỡng kỹ thuật:**
    *   🟢 **GREEN (An toàn):** Sóng $H_s \le 0.4\text{m}$; Gió duy trì $\le 12\text{km/h}$ (dưới 7 knots, mặt nước phẳng lặng); Gió giật $\le 18\text{km/h}$.
    *   🟡 **YELLOW (Cảnh báo):** Sóng $H_s$ từ $0.4\text{m} - 0.7\text{m}$; Gió duy trì $12 - 20\text{km/h}$ (bắt buộc mặc áo phao, gắn dây leashes buộc chân, không chèo cách bờ quá 100m).
    *   🔴 **RED (Cấm hoạt động):**
        *   Sóng biển $H_s > 0.8\text{m}$ (sóng vỗ bờ liên tục làm lật SUP, không thể đứng chèo).
        *   Tốc độ gió duy trì $> 20\text{km/h}$ hoặc gió giật $> 28\text{km/h}$.
        *   Gió thổi từ đất liền ra biển (hướng gió Tây/Tây Nam tại bãi biển phía Đông Đà Nẵng) với tốc độ $> 15\text{km/h}$ (nguy cơ bị cuốn ra biển mở).
        *   Dòng chảy hải lưu ven bờ (longshore/rip current) $> 0.3\text{m/s}$.

---

### 4. Mô tô nước (Jetski) & Trượt phao chuối (Banana Boat)
*Nguồn đối chiếu: Thông tư 17/2018/TT-BVHTTDL, Nghị định 48/2019/NĐ-CP, US Coast Guard Small Craft Guidelines.*
*   **Đặc thù:** Tốc độ cao (40 - 70 km/h). Khi sóng lớn, mô tô nước dễ mất lái, lật úp; đối với phao chuối, lực ly tâm khi va vào mặt sóng nhấp nhô có thể gây chấn thương đốt sống hoặc va đập giữa các hành khách.
*   **Các ngưỡng kỹ thuật:**
    *   🟢 **GREEN (An toàn):** Sóng $H_s \le 0.6\text{m}$; Gió duy trì $\le 20\text{km/h}$; Tầm nhìn $> 3000\text{m}$.
    *   🟡 **YELLOW (Cảnh báo):** Sóng $H_s$ từ $0.6\text{m} - 1.0\text{m}$; Gió $20 - 30\text{km/h}$ (hạn chế tốc độ cano dưới 25 km/h, giảm số lượng người trên phao chuối).
    *   🔴 **RED (Cấm hoạt động):**
        *   Sóng biển $H_s > 1.0\text{m}$ (đối với phao chuối) hoặc $H_s > 1.2\text{m}$ (đối với mô tô nước).
        *   Gió duy trì $> 30\text{km/h}$ hoặc gió giật $> 40\text{km/h}$ (Beaufort cấp 5).
        *   Tầm nhìn hạn chế $< 1000\text{m}$ (mưa lớn cản trở việc quan sát người tắm biển).

---

### 5. Cano cao tốc / Cano vận chuyển khách tham quan (Speedboat)
*Nguồn đối chiếu: QCVN 72:2013/BGTVT, Quy chế an toàn Cảng vụ Hàng hải Đà Nẵng.*
*   **Đặc thù:** Phương tiện chuyên chở nhóm khách du lịch (12 - 36 chỗ), chở khách ra bán đảo Sơn Trà hoặc sang Cù Lao Chàm.
*   **Các ngưỡng kỹ thuật:**
    *   🟢 **GREEN (An toàn):** Sóng $H_s \le 1.0\text{m}$; Gió duy trì $\le 25\text{km/h}$ (Beaufort $\le$ cấp 4); Tầm nhìn $> 5000\text{m}$.
    *   🟡 **YELLOW (Cảnh báo):** Sóng $H_s$ từ $1.0\text{m} - 1.5\text{m}$; Gió $25 - 35\text{km/h}$ (Beaufort cấp 4-5) — cano phải giảm tải trọng hành khách, đi đúng luồng hàng hải, yêu cầu tất cả hành khách cài áo phao đúng chuẩn.
    *   🔴 **RED (Cấm xuất bến):**
        *   Sóng biển $H_s > 1.5\text{m}$ (với cano mạn hở nhỏ) hoặc $H_s > 2.0\text{m}$ (cano du lịch tiêu chuẩn).
        *   Gió duy trì $> 38\text{km/h}$ (Beaufort $\ge$ cấp 6 - ngưỡng cơ quan chức năng ra **Lệnh Cấm Biển**).
        *   Mưa bão lớn, sương mù dày làm tầm nhìn $< 1000\text{m}$.

---

### 6. Du thuyền ngắm hoàng hôn / Catamaran
*Nguồn đối chiếu: Tiêu chuẩn tàu chở khách đường thủy nội địa & ven biển nhóm VR-SI.*
*   **Đặc thù:** Thân tàu lớn, độ ổn định cao hơn cano nhỏ, chở đông khách (thường 20 - 50 người). Hoạt động chủ yếu vùng vịnh Đà Nẵng kín gió và cửa sông Hàn.
*   **Các ngưỡng kỹ thuật:**
    *   🟢 **GREEN (An toàn):** Sóng $H_s \le 0.8\text{m}$; Gió $\le 25\text{km/h}$; Không mưa.
    *   🟡 **YELLOW (Cảnh báo):** Sóng $H_s$ từ $0.8\text{m} - 1.4\text{m}$; Gió $25 - 35\text{km/h}$ (khuyến cáo khách say sóng, không cho khách ra đứng sát mạn boong tàu khi quay đầu).
    *   🔴 **RED (Cấm hành trình):**
        *   Sóng biển $H_s > 1.5\text{m}$.
        *   Gió duy trì $> 38\text{km/h}$ (gió cấp 6) hoặc giật $> 50\text{km/h}$.
        *   Mưa to kèm giông lốc sông Hàn / Vịnh Đà Nẵng.

---

### 7. Câu cá biển & Câu mực đêm
*Nguồn đối chiếu: An toàn tàu cá dân gian và du lịch biển ven bờ Đà Nẵng.*
*   **Đặc thù:** Tàu neo một chỗ vào ban đêm, máy phát điện bật đèn công suất cao thu hút mực. Sóng lắc ngang liên tục gây say sóng nặng; ban đêm nếu gặp mưa giông cục bộ thì việc cứu nạn cứu hộ là cực kỳ khó khăn.
*   **Các ngưỡng kỹ thuật:**
    *   🟢 **GREEN (An toàn):** Sóng $H_s \le 0.6\text{m}$; Gió duy trì $\le 15\text{km/h}$; Tầm nhìn ban đêm quang đãng; Không có mây dông.
    *   🟡 **YELLOW (Cảnh báo):** Sóng $H_s$ từ $0.6\text{m} - 1.0\text{m}$; Gió $15 - 22\text{km/h}$.
    *   🔴 **RED (Cấm xuất bến):**
        *   Sóng biển $H_s > 1.0\text{m}$ (nguy cơ lật ghe câu nhỏ).
        *   Gió duy trì $> 25\text{km/h}$ hoặc có cảnh báo giông đêm.
        *   Sương mù biển che khuất đèn hải đăng và tầm nhìn $< 1500\text{m}$.

---

### 8. Lướt ván (Surfing)
*Nguồn đối chiếu: International Surfing Association (ISA) Water Safety Guidelines.*
*   **Đặc thù:** Đây là bộ môn **nghịch lý**: Cần có sóng thì mới chơi được (sóng quá phẳng lặng = không thể lướt). Nguy cơ chính đến từ sóng quá cao vượt kỹ năng của người học, dòng rip (dòng chảy xa bờ) và gió thổi onshore (từ biển vào bờ) quá mạnh làm vỡ mặt sóng.
*   **Các ngưỡng kỹ thuật:**
    *   🟢 **GREEN (Lý tưởng cho người học):** Sóng $H_s$ từ $0.5\text{m} - 1.2\text{m}$; Chu kỳ sóng $8 - 12\text{s}$; Gió nhẹ dưới $15\text{km/h}$ hoặc gió từ bờ thổi ra (offshore).
    *   🟡 **YELLOW (Dành cho người lướt trung cấp/chuyên nghiệp):** Sóng $H_s$ từ $1.2\text{m} - 2.0\text{m}$; Dòng chảy $0.3 - 0.6\text{m/s}$ (cấm người mới bắt đầu).
    *   🔴 **RED (Cấm toàn bộ du khách):**
        *   Sóng biển $H_s > 2.0\text{m}$ (sóng cấp 4 nguy hiểm, dòng cuốn cực mạnh).
        *   Sóng biển $H_s < 0.3\text{m}$ (không đủ điều kiện tối thiểu để tạo sóng lướt).
        *   Gió giật $> 35\text{km/h}$.
        *   Có giông sét (WMO Code $\ge 95$).

---

## BẢNG TỔNG HỢP MA TRẬN NGƯỠNG AN TOÀN TOÀN HỆ THỐNG

| Danh mục trò chơi | Sóng tối đa cho phép ($H_s$ đỏ) | Gió duy trì tối đa | Gió giật tối đa (Gust) | Dòng hải lưu tối đa | Điều kiện WMO cấm tuyệt đối |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **Chèo SUP** | **$> 0.8\text{ m}$** | $> 20\text{ km/h}$ | $> 28\text{ km/h}$ | $> 0.3\text{ m/s}$ | Giông sét, mưa rào, gió ngược chiều bờ |
| **Chèo Kayak** | **$> 0.9\text{ m}$** | $> 22\text{ km/h}$ | $> 30\text{ km/h}$ | $> 0.4\text{ m/s}$ | Giông sét, sương mù dày |
| **Lặn ngắm san hô (Snorkeling/Seawalker)** | **$> 1.2\text{ m}$** | $> 25\text{ km/h}$ | $> 35\text{ km/h}$ | $> 0.4\text{ m/s}$ | Nước đục tầm nhìn < 3m, giông sét |
| **Lặn bình khí (Scuba Diving)** | **$> 1.5\text{ m}$** | $> 28\text{ km/h}$ | $> 38\text{ km/h}$ | $> 0.5\text{ m/s}$ | Giông sét, dòng xoáy ngầm |
| **Dù bay biển (Parasailing)** | **$> 1.0\text{ m}$** | **$> 28\text{ km/h}$** | **$> 37\text{ km/h}$** | Không áp dụng | **Bắt buộc dừng khi có mây đối lưu / giông trong 10km** |
| **Mô tô nước (Jetski)** | **$> 1.2\text{ m}$** | $> 30\text{ km/h}$ | $> 40\text{ km/h}$ | $> 0.6\text{ m/s}$ | Tầm nhìn < 1000m, giông bão |
| **Phao chuối (Banana Boat)** | **$> 1.0\text{ m}$** | $> 25\text{ km/h}$ | $> 35\text{ km/h}$ | Không áp dụng | Biển động, có sóng vỡ đầu bạc |
| **Cano cao tốc tham quan đảo** | **$> 1.8\text{ m}$** | $> 38\text{ km/h}$ | $> 45\text{ km/h}$ | $> 0.8\text{ m/s}$ | Cảng vụ phát lệnh cấm biển (Gió cấp 6) |
| **Du thuyền Catamaran ngắm hoàng hôn** | **$> 1.5\text{ m}$** | $> 38\text{ km/h}$ | $> 45\text{ km/h}$ | $> 0.7\text{ m/s}$ | Lốc xoáy sông Hàn / Vịnh Đà Nẵng |
| **Câu cá / Câu mực đêm** | **$> 1.0\text{ m}$** | $> 25\text{ km/h}$ | $> 30\text{ km/h}$ | $> 0.4\text{ m/s}$ | Mưa giông đêm, sương mù hạn chế tầm nhìn |
| **Lướt ván (Surfing)** | **$> 2.0\text{ m}$** (hoặc $< 0.3\text{m}$) | $> 30\text{ km/h}$ | $> 40\text{ km/h}$ | $> 0.6\text{ m/s}$ | Sóng dồn dập, giông sét nguy hiểm |

---
