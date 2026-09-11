package com.danasea.backend.modules.service.application.dto;

import java.util.UUID;

public record ServiceImageResult(
        UUID id,
        UUID serviceId,
        String url,
        Short sortOrder
) {}
