# 📖 TÓM TẮT CÁC SƠ ĐỒ — MODULE DISPUTE (KHIẾU NẠI ĐƠN HÀNG)

> Module Dispute quản lý toàn bộ quy trình tiếp nhận khiếu nại từ khách hàng sau khi trải nghiệm dịch vụ và hỗ trợ quản trị viên (Admin) thẩm định, đưa ra quyết định xử lý hoàn tiền hoặc bác bỏ khiếu nại.

| # | Sơ đồ | Loại | Nội dung |
|---|-------|------|----------|
| 1 | `Dispute_CustomerCreate` | Sequence Diagram | Khách hàng khởi tạo khiếu nại: Kiểm tra IDOR, xác thực đơn thuộc sở hữu, kiểm tra trạng thái hợp lệ (`CONFIRMED`/`COMPLETED`), giới hạn 7 ngày sau trải nghiệm và ngăn ngừa tạo khiếu nại trùng lặp |
| 2 | `Dispute_AdminResolve` | Sequence Diagram | Admin thẩm định và giải quyết khiếu nại: Áp dụng Pessimistic Write Lock, phân nhánh tài chính (hoàn toàn phần, hoàn một phần hoặc bác bỏ), tự động tạo bản ghi Refund bất biến kèm Idempotency Key |
| 3 | `Dispute_StateMachine` | State Machine | Sơ đồ chuyển đổi trạng thái khiếu nại từ `OPEN` qua `UNDER_REVIEW` đến các trạng thái kết thúc (`RESOLVED_REFUND`, `RESOLVED_PARTIAL`, `RESOLVED_REJECTED`) |
| 4 | `Dispute_VendorResponse_And_AdminResolve` | Sequence Diagram | Đối tác gửi giải trình & bằng chứng đối chất, Admin tra cứu hồ sơ toàn diện: Khóa bi quan chống ghi đè phán quyết, kiểm soát Vendor Isolation chặt chẽ |
| 5 | `Dispute_ControlledEvidenceUpload` | Sequence Diagram | Tải lên bằng chứng an toàn: Kiểm tra magic bytes thực tế, giới hạn định dạng (JPEG, PNG, WEBP, PDF) và dung lượng <= 5MB qua FileStoragePort |

---

## 1. Dispute_CustomerCreate.png — Khách Hàng Mở Khiếu Nại

**Lớp xử lý chính:** `com.danasea.backend.modules.dispute.application.usecases.CreateDisputeUseCase`

**Luồng nghiệp vụ chi tiết:**
1. Khách hàng gửi yêu cầu mở khiếu nại qua API `POST /api/orders/{orderId}/disputes` với thông tin `{subOrderId, reason, description, evidenceUrls}`.
2. Hệ thống kiểm tra sự tồn tại của đơn hàng `MasterOrder` (ném lỗi `404` nếu không tìm thấy).
3. **IDOR Check:** Xác minh người gửi yêu cầu chính là chủ sở hữu đơn hàng (`masterOrder.customerId == customerId`). Nếu không phải, chặn với `403 UnauthorizedDisputeAccessException`.
4. Xác minh đơn phụ `SubOrder` thuộc về `MasterOrder` được chỉ định.
5. **Ràng buộc trạng thái:** Chỉ cho phép khiếu nại các đơn đã hoàn thành hoặc đã được xác nhận (`COMPLETED` hoặc `CONFIRMED`). Nếu đơn đang ở trạng thái khác, trả về `400 InvalidSubOrderStateException`.
6. **Thời hạn khiếu nại:** Kiểm tra thời điểm khiếu nại không vượt quá **7 ngày** tính từ ngày trải nghiệm thực tế (`slot.date`). Nếu quá hạn, trả về `400 DisputePeriodExpiredException`.
7. **Chống trùng lặp (Anti-duplicate):** Kiểm tra xem đơn phụ này đã có khiếu nại nào đang ở trạng thái `OPEN` hoặc `UNDER_REVIEW` hay chưa. Nếu đã có, trả về `409 DuplicateDisputeException`.
8. Khởi tạo và lưu bản ghi khiếu nại mới với trạng thái ban đầu là `OPEN`.

---

## 2. Dispute_AdminResolve.png — Admin Giải Quyết Khiếu Nại

**Lớp xử lý chính:** `com.danasea.backend.modules.dispute.application.usecases.ResolveDisputeUseCase`

**Luồng nghiệp vụ chi tiết:**
1. Admin gửi quyết định qua `PATCH /api/admin/disputes/{disputeId}/resolve` kèm `{resolution, refundPercentage, adminNote}`.
2. Áp dụng **Pessimistic Lock** (`findByIdForUpdate`) để đảm bảo tính an toàn đồng thời trong cơ sở dữ liệu.
3. Kiểm tra nếu khiếu nại đã được giải quyết trước đó, ném ngoại lệ `409 DisputeAlreadyResolvedException`.
4. **Phân nhánh xử lý tài chính theo quyết định:**
   - **`RESOLVED_REFUND` (Hoàn 100%):** Tính số tiền hoàn = toàn bộ giá trị đơn phụ (`subtotal`). Ủy quyền qua `financialRefundPort.initiateRefund(...)` tạo bản ghi `Refund (PENDING)` với lý do `DISPUTE` và kích hoạt luồng hoàn tiền thực tế qua cổng thanh toán gốc.
   - **`RESOLVED_PARTIAL` (Hoàn một phần):** Tính số tiền hoàn theo tỷ lệ phần trăm được chỉ định (`subtotal * percentage / 100`). Ủy quyền qua `financialRefundPort.initiateRefund(...)` tạo bản ghi `Refund (PENDING)` chuyển sang `RefundProcessingService`.
   - **`RESOLVED_REJECTED` (Bác bỏ):** Giữ nguyên đơn hàng, không tạo bản ghi hoàn tiền.
5. Tạo khóa chống lặp hoàn tiền (**Idempotency Key:** `"dispute-{disputeId}"`) để đảm bảo không bị trừ tiền nhiều lần.
6. Cập nhật trạng thái Dispute thành trạng thái giải quyết tương ứng, ghi nhận `adminId` và thời điểm phê duyệt.

---

## 3. Dispute_StateMachine.png — Vòng Đời Trạng Thái Khiếu Nại

- `OPEN`: Trạng thái ban đầu sau khi khách hàng tạo khiếu nại thành công.
- `UNDER_REVIEW`: Trạng thái khi Admin hoặc bộ phận CSKH tiếp nhận hồ sơ xem xét.
- `RESOLVED_REFUND`: Admin chấp thuận khiếu nại và đồng ý hoàn tiền 100%. Tự động sinh `Refund` sang cổng thanh toán.
- `RESOLVED_PARTIAL`: Admin chấp thuận bồi thường một phần giá trị đơn. Tự động sinh `Refund` theo tỷ lệ đã thỏa thuận.
- `RESOLVED_REJECTED`: Admin từ chối khiếu nại do không đủ bằng chứng hoặc không vi phạm điều khoản dịch vụ.

> 💡 **Tích hợp liên module:** Trong module **Settlement**, bất kỳ đơn hàng nào đang có Dispute ở trạng thái `OPEN` hoặc `UNDER_REVIEW` sẽ bị **loại trừ tự động (EXCLUDED)** khỏi bảng quyết toán của Vendor để phòng tránh thất thoát tài chính.

---

## 4. Dispute_VendorResponse_And_AdminResolve — Đối Tác Phản Hồi & Admin Thẩm Định Hồ Sơ

**Lớp xử lý chính:**
- `com.danasea.backend.modules.dispute.application.usecases.SubmitVendorDisputeResponseUseCase`
- `com.danasea.backend.modules.dispute.application.usecases.GetAdminDisputeDetailUseCase`
- `com.danasea.backend.modules.dispute.application.usecases.ResolveDisputeUseCase`

### Sơ đồ tuần tự (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor Vendor as Đối Tác (Vendor)
    actor Admin as Quản Trị Viên (Admin)
    participant VCfg as VendorDisputeController
    participant ACfg as AdminDisputeController
    participant VUC as SubmitVendorDisputeResponseUseCase
    participant AUC as GetAdminDisputeDetailUseCase
    participant RUC as ResolveDisputeUseCase
    participant VPort as VendorInternalApi
    participant OPort as OrderInternalApi
    participant DRepo as JpaDisputeRepository
    participant DB as PostgreSQL DB

    rect rgb(240, 248, 255)
        note over Vendor,DB: 1. Vendor gửi giải trình & bằng chứng đối chất
        Vendor->>VCfg: POST /api/vendor/disputes/{id}/responses { response, evidenceUrls }
        VCfg->>VUC: execute(disputeId, request, vendorUserId)
        VUC->>VPort: findByUserId(vendorUserId)
        VPort-->>VUC: vendorProfile (403 nếu không tồn tại)
        
        VUC->>DRepo: findByIdForUpdate(disputeId)
        DRepo->>DB: SELECT * FROM disputes WHERE id = ? FOR UPDATE
        DB-->>DRepo: dispute (Locked)
        DRepo-->>VUC: dispute
        
        VUC->>OPort: findSubOrderById(dispute.subOrderId)
        OPort-->>VUC: subOrder
        VUC->>VUC: Xác minh subOrder.vendorId == vendorProfile.id (403 nếu Vendor Isolation vi phạm)
        
        alt Tranh chấp đã được Admin chốt giải quyết
            VUC-->>VCfg: 409 Conflict (Dispute đã chốt kết quả, không thể phản hồi)
        else Tranh chấp đang mở/chờ xử lý
            VUC->>VUC: Cập nhật vendorResponse, vendorEvidenceUrls, vendorRespondedAt
            VUC->>DRepo: save(dispute)
            DRepo->>DB: UPDATE disputes SET vendor_response = ?, vendor_evidence_urls = ?, vendor_responded_at = ? WHERE id = ?
            DB-->>DRepo: OK
            VUC-->>VCfg: 200 OK (DisputeResponse)
            VCfg-->>Vendor: 200 OK
        end
    end

    rect rgb(255, 250, 240)
        note over Admin,DB: 2. Admin thẩm tra toàn bộ hồ sơ tranh chấp
        Admin->>ACfg: GET /api/admin/disputes/{id}
        ACfg->>AUC: execute(disputeId)
        AUC->>DRepo: findById(disputeId)
        DRepo-->>AUC: dispute
        AUC-->>ACfg: 200 OK (DisputeResponse chứa đầy đủ Customer evidence + Vendor response/evidence)
        ACfg-->>Admin: 200 OK
    end

    rect rgb(240, 255, 240)
        note over Admin,DB: 3. Admin đưa ra phán quyết giải quyết (Pessimistic Locking)
        Admin->>ACfg: PATCH /api/admin/disputes/{id}/resolve { resolution, refundPercentage, adminNote }
        ACfg->>RUC: execute(disputeId, request, adminId)
        RUC->>DRepo: findByIdForUpdate(disputeId)
        DRepo->>DB: SELECT * FROM disputes WHERE id = ? FOR UPDATE
        DB-->>DRepo: dispute (Locked)
        DRepo-->>RUC: dispute
        
        RUC->>RUC: Kiểm tra trạng thái tranh chấp (409 Conflict nếu đã resolved)
        RUC->>RUC: Xử lý hoàn tiền PENDING (nếu hoàn 100% hoặc hoàn một phần)
        RUC->>DRepo: save(dispute)
        RUC-->>ACfg: 200 OK (DisputeResponse)
        ACfg-->>Admin: 200 OK
    end
```

---

## 5. Dispute_ControlledEvidenceUpload — Tải Lên Bằng Chứng An Toàn

**Lớp xử lý chính:** `com.danasea.backend.modules.dispute.presentation.controllers.DisputeEvidenceController` & `UploadDisputeEvidenceUseCase`

### Sơ đồ tuần tự (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor User as Client (Customer/Vendor)
    participant Ctrl as DisputeEvidenceController
    participant UC as UploadDisputeEvidenceUseCase
    participant Val as FileSignatureValidator
    participant Storage as FileStoragePort (Cloudinary)
    participant Cloud as Remote Storage / CDN

    User->>Ctrl: POST /api/disputes/evidence (multipart/form-data: file)
    Ctrl->>UC: execute(file, currentUserId)

    rect rgb(255, 245, 245)
        note over UC,Val: 1. Kiểm tra dung lượng & định dạng tệp (Magic Bytes)
        UC->>UC: Kiểm tra kích thước file (file.getSize() <= 5MB)
        alt Vượt quá 5MB
            UC-->>Ctrl: 400 Bad Request (File size exceeds 5MB limit)
        end
        
        UC->>Val: validateMagicBytes(file.getBytes(), allowedTypes)
        alt Định dạng không hợp lệ (Không phải JPEG, PNG, WEBP, PDF)
            Val-->>UC: Invalid file signature
            UC-->>Ctrl: 400 Bad Request (File format not supported)
        end
        Val-->>UC: Valid signature
    end

    rect rgb(240, 255, 240)
        note over UC,Cloud: 2. Lưu trữ tệp lên hệ thống Cloud Storage
        UC->>Storage: uploadFile(file.getBytes(), originalFilename, folder="disputes")
        Storage->>Cloud: Tải tệp lên hạ tầng đám mây
        Cloud-->>Storage: secureUrl (https://res.cloudinary.com/...)
        Storage-->>UC: secureUrl
    end

    UC-->>Ctrl: EvidenceUploadResponse(fileUrl=secureUrl)
    Ctrl-->>User: 200 OK { "fileUrl": "https://res.cloudinary.com/..." }
```

### Đặc tính an toàn:
- **Magic Bytes Validation:** Không phụ thuộc vào phần mở rộng file (extension) của client gửi lên mà đọc trực tiếp các byte đầu tiên (file signature) để ngăn chặn việc tải lên file độc hại giả mạo (executable, script).
- **Hạn mức dung lượng:** Cố định tối đa 5MB mỗi tệp tải lên.
- **Tích hợp phi lưu trữ:** Tách biệt việc lưu trữ vật lý qua interface `FileStoragePort`, dễ dàng chuyển đổi linh hoạt giữa Cloudinary và S3/MinIO mà không ảnh hưởng tới code nghiệp vụ.

