package com.danasea.backend.modules.ai.infrastructure.persistence.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "ai_chat_requests", uniqueConstraints = @UniqueConstraint(
        name = "uq_ai_chat_request_actor_key", columnNames = { "actor_id", "request_key" }))
public class AiChatRequestJpaEntity extends BaseJpaEntity {
    @Column(nullable = false) private UUID actorId;
    @Column(nullable = false, length = 128) private String requestKey;
    @Column(nullable = false, length = 64) private String requestFingerprint;
    private UUID conversationId;
    @Column(nullable = false, length = 16) private String status;
    @Column(columnDefinition = "TEXT") private String responseJson;
    @Column(nullable = false) private OffsetDateTime expiresAt;
}
