package com.danasea.backend.modules.communication.infrastructure.persistence.entities;

import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "messages")
public class MessageJpaEntity extends BaseJpaEntity {

    private UUID conversationId;

    private UUID senderId;

    private String content;

    private String attachmentUrl;

    private Boolean isRead;

}
