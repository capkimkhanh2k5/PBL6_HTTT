package com.danasea.backend.modules.checkin.presentation.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeResponse {
    private UUID subOrderId;
    private String qrToken;
    private OffsetDateTime expiresAt;

    public UUID subOrderId() {
        return subOrderId;
    }

    public String qrToken() {
        return qrToken;
    }

    public OffsetDateTime expiresAt() {
        return expiresAt;
    }
}
