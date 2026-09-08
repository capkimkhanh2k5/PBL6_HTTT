package com.danasea.backend.modules.communication.domain.models;

import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Message extends BaseDomainModel {
    private UUID conversationId;
    private UUID senderId;
    private String content;
    private String attachmentUrl;
    private Boolean isRead;
}
