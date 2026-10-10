package com.danasea.backend.modules.communication.application.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "{validation.communication.message_content.required}")
        @Size(max = 5000, message = "{validation.communication.message_content.max}")
        String content,
        String attachmentUrl
) {
    public SendMessageRequest(String content) {
        this(content, null);
    }
}
