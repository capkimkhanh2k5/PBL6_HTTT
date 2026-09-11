package com.danasea.backend.modules.service.application.dto;

import java.util.UUID;

public record RejectServiceCommand(
        UUID adminUserId,
        UUID serviceId,
        String reason
) {}
