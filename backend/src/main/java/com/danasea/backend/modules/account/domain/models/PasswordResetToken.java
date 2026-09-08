package com.danasea.backend.modules.account.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PasswordResetToken extends BaseDomainModel {
    private UUID userId;
    private String tokenHash;
    private OffsetDateTime expiresAt;
    private OffsetDateTime usedAt;
}
