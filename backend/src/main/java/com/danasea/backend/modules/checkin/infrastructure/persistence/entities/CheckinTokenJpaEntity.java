package com.danasea.backend.modules.checkin.infrastructure.persistence.entities;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity(name = "CheckinTokenEntity")
@Table(name = "checkin_tokens")
@Access(AccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckinTokenJpaEntity extends BaseJpaEntity {

    @Column(name = "sub_order_id", nullable = false)
    private UUID subOrderId;

    @Column(name = "qr_token_hash", length = 64, nullable = false, unique = true)
    private String qrTokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "used_by_vendor_staff_id")
    private UUID usedByVendorStaffId;
}
