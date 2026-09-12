package com.danasea.backend.modules.service.application.dtos;

import java.util.UUID;

public record RejectServiceCommand(
        UUID adminUserId,
        UUID serviceId,
        String reason
) {}
