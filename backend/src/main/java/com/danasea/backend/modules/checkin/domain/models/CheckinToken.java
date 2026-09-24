package com.danasea.backend.modules.checkin.domain.models;

import com.danasea.backend.modules.checkin.domain.exceptions.CheckinAlreadyUsedException;
import com.danasea.backend.modules.checkin.domain.exceptions.QrTokenExpiredException;
import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Model quản lý thực thể vé Check-in Token cho dịch vụ du lịch biển.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CheckinToken extends BaseDomainModel {

    private UUID subOrderId;
    private String qrTokenHash;
    private OffsetDateTime expiresAt;
    private OffsetDateTime usedAt;
    private UUID usedByVendorStaffId;

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(OffsetDateTime now) {
        Objects.requireNonNull(now, "Thời điểm kiểm tra không được null");
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public void markUsed(UUID staffId, OffsetDateTime timestamp) {
        Objects.requireNonNull(staffId, "Staff ID không được null");
        Objects.requireNonNull(timestamp, "Timestamp không được null");

        if (isUsed()) {
            throw new CheckinAlreadyUsedException(
                    String.format("Vé check-in cho SubOrder %s đã được sử dụng lúc %s bởi nhân viên %s",
                            subOrderId, usedAt, usedByVendorStaffId),
                    usedAt,
                    usedByVendorStaffId
            );
        }

        if (isExpired(timestamp)) {
            throw new QrTokenExpiredException(
                    String.format("Vé check-in cho SubOrder %s đã hết hạn lúc %s (thời điểm quét: %s)",
                            subOrderId, expiresAt, timestamp)
            );
        }

        this.usedAt = timestamp;
        this.usedByVendorStaffId = staffId;
    }

    public static CheckinToken create(UUID subOrderId, String qrTokenHash, OffsetDateTime expiresAt) {
        Objects.requireNonNull(subOrderId, "subOrderId không được null");
        Objects.requireNonNull(qrTokenHash, "qrTokenHash không được null");
        Objects.requireNonNull(expiresAt, "expiresAt không được null");

        if (qrTokenHash.length() != 64) {
            throw new IllegalArgumentException("qrTokenHash phải có đúng 64 ký tự hex (SHA-256)");
        }

        return CheckinToken.builder()
                .subOrderId(subOrderId)
                .qrTokenHash(qrTokenHash.toLowerCase())
                .expiresAt(expiresAt)
                .usedAt(null)
                .usedByVendorStaffId(null)
                .build();
    }
}
