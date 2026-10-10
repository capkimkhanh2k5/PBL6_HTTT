package com.danasea.backend.modules.order.presentation.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubOrderWaiverAcceptanceResponse(
        UUID subOrderId,
        UUID masterOrderId,
        boolean accepted,
        int waiverVersion,
        String language,
        OffsetDateTime acceptedAt) {}
