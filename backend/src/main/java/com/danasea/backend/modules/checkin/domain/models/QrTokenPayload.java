package com.danasea.backend.modules.checkin.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object biểu diễn nội dung payload của vé QR Check-in.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrTokenPayload {

    private UUID subOrderId;
    private OffsetDateTime expiresAt;

    public String serialize() {
        Objects.requireNonNull(subOrderId, "subOrderId không được null");
        Objects.requireNonNull(expiresAt, "expiresAt không được null");
        return subOrderId.toString() + ":" + expiresAt.toString();
    }

    public static QrTokenPayload deserialize(String payloadStr) {
        if (payloadStr == null || payloadStr.isBlank()) {
            throw new IllegalArgumentException("Payload string không được để trống");
        }
        int colonIdx = payloadStr.indexOf(':');
        if (colonIdx <= 0 || colonIdx >= payloadStr.length() - 1) {
            throw new IllegalArgumentException("Cấu trúc payload không hợp lệ: " + payloadStr);
        }
        UUID subOrderId = UUID.fromString(payloadStr.substring(0, colonIdx));
        OffsetDateTime expiresAt = OffsetDateTime.parse(payloadStr.substring(colonIdx + 1));
        return new QrTokenPayload(subOrderId, expiresAt);
    }
}
