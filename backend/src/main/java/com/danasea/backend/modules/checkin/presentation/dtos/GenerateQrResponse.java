package com.danasea.backend.modules.checkin.presentation.dtos;

import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

public class GenerateQrResponse extends QrCodeResponse {

    public GenerateQrResponse(UUID subOrderId, String qrToken, OffsetDateTime expiresAt) {
        super(subOrderId, qrToken, expiresAt);
    }
}
