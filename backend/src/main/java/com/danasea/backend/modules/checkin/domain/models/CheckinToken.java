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
        Objects.requireNonNull(now, "Validation time must not be null");
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public void markUsed(UUID staffId, OffsetDateTime timestamp) {
        Objects.requireNonNull(staffId, "Staff ID must not be null");
        Objects.requireNonNull(timestamp, "Timestamp must not be null");

        if (isUsed()) {
            throw new CheckinAlreadyUsedException(
                    String.format("The check-in ticket for sub-order %s was used at %s by staff member %s",
                            subOrderId, usedAt, usedByVendorStaffId),
                    usedAt,
                    usedByVendorStaffId
            );
        }

        if (isExpired(timestamp)) {
            throw new QrTokenExpiredException(
                    String.format("The check-in ticket for sub-order %s expired at %s (scan time: %s)",
                            subOrderId, expiresAt, timestamp)
            );
        }

        this.usedAt = timestamp;
        this.usedByVendorStaffId = staffId;
    }

    public static CheckinToken create(UUID subOrderId, String qrTokenHash, OffsetDateTime expiresAt) {
        Objects.requireNonNull(subOrderId, "subOrderId must not be null");
        Objects.requireNonNull(qrTokenHash, "qrTokenHash must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        if (qrTokenHash.length() != 64) {
            throw new IllegalArgumentException("qrTokenHash must contain exactly 64 hexadecimal characters (SHA-256)");
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
