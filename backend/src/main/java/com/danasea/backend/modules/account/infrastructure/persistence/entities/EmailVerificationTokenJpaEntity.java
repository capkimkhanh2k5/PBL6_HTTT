package com.danasea.backend.modules.account.infrastructure.persistence.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "email_verification_tokens")
public class EmailVerificationTokenJpaEntity extends BaseJpaEntity {

    private UUID userId;

    private String tokenHash;

    private OffsetDateTime expiresAt;

    private OffsetDateTime verifiedAt;

}
