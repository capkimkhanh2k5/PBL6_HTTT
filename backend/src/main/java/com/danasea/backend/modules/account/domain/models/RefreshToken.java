package com.danasea.backend.modules.account.domain.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RefreshToken extends BaseDomainModel {
    private UUID userId;
    private String tokenHash;
    private UUID familyId;
    private UUID replacedById;
    private OffsetDateTime expiresAt;
    private OffsetDateTime revokedAt;
}
