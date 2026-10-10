package com.danasea.backend.modules.order.domain.models;

import java.util.UUID;

public record MissingWaiverItem(
        UUID subOrderId,
        UUID serviceId,
        String serviceName,
        Integer waiverVersion,
        boolean required,
        String waiverContent,
        String contentLanguage,
        boolean fallbackUsed) {}
