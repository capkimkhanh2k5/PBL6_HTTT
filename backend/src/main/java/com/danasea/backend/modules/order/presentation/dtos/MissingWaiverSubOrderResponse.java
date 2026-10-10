package com.danasea.backend.modules.order.presentation.dtos;

import java.util.UUID;

public record MissingWaiverSubOrderResponse(
        UUID subOrderId,
        UUID serviceId,
        String serviceName,
        Integer waiverVersion,
        boolean required,
        String waiverContent,
        String contentLanguage,
        boolean fallbackUsed) {}
