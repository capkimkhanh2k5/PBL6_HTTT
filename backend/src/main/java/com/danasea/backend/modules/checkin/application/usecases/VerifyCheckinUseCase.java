package com.danasea.backend.modules.checkin.application.usecases;

import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;
import com.danasea.backend.modules.checkin.domain.exceptions.CheckinTokenNotFoundException;
import com.danasea.backend.modules.checkin.domain.exceptions.InvalidQrSignatureException;
import com.danasea.backend.modules.checkin.domain.exceptions.InvalidQrTokenException;
import com.danasea.backend.modules.checkin.domain.exceptions.InvalidSubOrderStateException;
import com.danasea.backend.modules.checkin.domain.exceptions.QrTokenAlreadyUsedException;
import com.danasea.backend.modules.checkin.domain.exceptions.QrTokenExpiredException;
import com.danasea.backend.modules.checkin.domain.exceptions.UnauthorizedVendorCheckinException;
import com.danasea.backend.modules.checkin.domain.models.QrTokenPayload;
import com.danasea.backend.modules.checkin.domain.services.QrTokenSigner;
import com.danasea.backend.modules.checkin.infrastructure.persistence.entities.CheckinTokenJpaEntity;
import com.danasea.backend.modules.checkin.infrastructure.persistence.repositories.JpaCheckinTokenRepository;
import com.danasea.backend.modules.checkin.presentation.dtos.VerifyCheckinRequest;
import com.danasea.backend.modules.checkin.presentation.dtos.VerifyCheckinResponse;
import com.danasea.backend.modules.order.domain.exceptions.OrderNotFoundException;
import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
import com.danasea.backend.modules.order.infrastructure.persistence.entities.SubOrderJpaEntity;
import com.danasea.backend.modules.order.infrastructure.persistence.repositories.JpaSubOrderRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCheckinUseCase {

    private final JpaCheckinTokenRepository checkinTokenRepository;
    private final JpaSubOrderRepository subOrderRepository;
    private final VendorLookupPort vendorLookupPort;
    private final QrTokenSigner qrTokenSigner;

    @Transactional
    public VerifyCheckinResponse execute(VerifyCheckinRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedVendorCheckinException("User is not authenticated"));
        return execute(request, currentUserId);
    }

    @Transactional
    public VerifyCheckinResponse execute(VerifyCheckinRequest request, UUID staffId) {
        // 1. Kiểm tra đầu vào (Null/Blank check)
        if (request == null || request.qrToken() == null || request.qrToken().isBlank()) {
            throw new InvalidQrTokenException("Mã QR token không được để trống");
        }

        String rawToken = request.qrToken().trim();

        // 2. Kiểm tra định dạng cú pháp (Malformed check)
        int dotIdx = rawToken.lastIndexOf('.');
        if (dotIdx <= 0 || dotIdx >= rawToken.length() - 1) {
            throw new InvalidQrTokenException("Cấu trúc QR Token không đúng định dạng (malformed)");
        }

        String payloadStr = rawToken.substring(0, dotIdx);
        if (!payloadStr.contains(":")) {
            throw new InvalidQrTokenException("Cấu trúc QR Token không đúng định dạng (thiếu dấu phân cách hai chấm)");
        }

        // 3. Xác thực chữ ký HMAC-SHA256 bằng thời gian hằng số (Constant-Time Verification)
        if (!qrTokenSigner.verifySignature(rawToken)) {
            throw new InvalidQrSignatureException("Chữ ký mã QR không hợp lệ hoặc dữ liệu vé đã bị can thiệp");
        }

        // 4. Giải mã Payload
        QrTokenPayload payload = qrTokenSigner.parsePayload(rawToken);

        // 5. Kiểm tra thời hạn vé (Expiration check)
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isAfter(payload.getExpiresAt())) {
            throw new QrTokenExpiredException(
                    String.format("Vé QR đã hết hạn lúc %s (thời điểm quét: %s)", payload.getExpiresAt(), now));
        }

        // 6. Tính Hash và tra cứu CheckinToken trong DB
        String tokenHash = QrTokenSigner.hashToken(rawToken);
        CheckinTokenJpaEntity tokenEntity = checkinTokenRepository.findByQrTokenHash(tokenHash)
                .orElseThrow(() -> new CheckinTokenNotFoundException("Vé check-in không tồn tại trong hệ thống"));

        // 7. Kiểm tra trạng thái đã sử dụng trước đó (Fast check)
        if (tokenEntity.getUsedAt() != null) {
            throw new QrTokenAlreadyUsedException(
                    String.format("Vé đã được sử dụng lúc %s bởi nhân viên có ID: %s",
                            tokenEntity.getUsedAt(), tokenEntity.getUsedByVendorStaffId()),
                    tokenEntity.getUsedAt(),
                    tokenEntity.getUsedByVendorStaffId()
            );
        }

        // 8. Tải thông tin SubOrder và kiểm tra trạng thái
        SubOrderJpaEntity subOrder = subOrderRepository.findById(payload.getSubOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy thông tin đơn dịch vụ với mã: " + payload.getSubOrderId()));

        if (subOrder.getStatus() == SubOrderStatus.CANCELLED || subOrder.getStatus() == SubOrderStatus.REFUNDED) {
            throw new InvalidSubOrderStateException("Đơn hàng đã ở trạng thái hủy/hoàn tiền, không thể check-in");
        }
        if (subOrder.getStatus() == SubOrderStatus.PENDING) {
            throw new InvalidSubOrderStateException("Đơn hàng chưa thanh toán thành công, không thể check-in");
        }

        // 9. Kiểm soát cô lập đa Vendor (Multi-tenant Vendor Isolation)
        boolean isAdmin = checkIsAdmin();
        if (!isAdmin) {
            UUID staffVendorId = resolveStaffVendorId(staffId);
            if (!Objects.equals(subOrder.getVendorId(), staffVendorId)) {
                log.warn("VENDOR_ISOLATION_VIOLATION: Staff {} (Vendor {}) cố tình check-in đơn {} của Vendor {}",
                        staffId, staffVendorId, subOrder.getId(), subOrder.getVendorId());
                throw new UnauthorizedVendorCheckinException(
                        String.format("Từ chối check-in: Đơn hàng thuộc nhà cung cấp %s, tài khoản của bạn thuộc nhà cung cấp %s",
                                subOrder.getVendorId(), staffVendorId));
            }
        }

        // 10. Cập nhật điều kiện nguyên tử (Atomic Conditional Update chống Race Condition)
        int rowsUpdated = 0;
        if (tokenEntity.getId() != null) {
            rowsUpdated = checkinTokenRepository.markAsUsedAtomic(tokenEntity.getId(), now, staffId);
        } else {
            rowsUpdated = checkinTokenRepository.markAsUsedIfUnused(tokenHash, staffId, now, now);
        }

        if (rowsUpdated == 0) {
            // Luồng khác đã chiếm quyền check-in trước
            CheckinTokenJpaEntity used = checkinTokenRepository.findByQrTokenHash(tokenHash).orElse(tokenEntity);
            throw new QrTokenAlreadyUsedException(
                    String.format("Vé đã được sử dụng lúc %s bởi nhân viên có ID: %s",
                            used.getUsedAt(), used.getUsedByVendorStaffId()),
                    used.getUsedAt(),
                    used.getUsedByVendorStaffId()
            );
        }

        // 11. Cập nhật trạng thái SubOrder sang CHECKED_IN
        subOrder.setStatus(SubOrderStatus.CHECKED_IN);
        subOrder.setCheckedInAt(now);
        subOrderRepository.save(subOrder);

        log.info("CHECKIN_SUCCESS: SubOrder {} check-in thành công bởi Staff {}", subOrder.getId(), staffId);

        return VerifyCheckinResponse.builder()
                .subOrderId(subOrder.getId())
                .vendorId(subOrder.getVendorId())
                .serviceId(subOrder.getServiceId())
                .slotId(subOrder.getSlotId())
                .quantity(subOrder.getQuantity())
                .status(SubOrderStatus.CHECKED_IN)
                .checkedInAt(now)
                .verifiedByStaffId(staffId)
                .message("Xác thực check-in thành công!")
                .build();
    }

    private UUID resolveStaffVendorId(UUID staffId) {
        if (vendorLookupPort == null) {
            return staffId;
        }
        Optional<UUID> vendorIdOpt = vendorLookupPort.findVendorIdByUserId(staffId);
        if (vendorIdOpt.isPresent()) {
            return vendorIdOpt.get();
        }
        throw new UnauthorizedVendorCheckinException("Không tìm thấy hồ sơ nhà cung cấp hợp lệ cho tài khoản: " + staffId);
    }

    private boolean checkIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
