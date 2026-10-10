package com.danasea.backend.modules.communication.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "messages", uniqueConstraints = @UniqueConstraint(name = "uq_messages_conversation_sequence", columnNames = {"conversation_id", "message_sequence"}))
public class MessageJpaEntity extends BaseJpaEntity {

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "sender_id")
    private UUID senderId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "message_sequence", nullable = false)
    private Long sequence;

}
