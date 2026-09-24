package com.danasea.backend.modules.ai.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationCard {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private String id;
    private UUID conversationId;
    private UUID serviceId;
    private UUID slotId;
    private BigDecimal price; // The price when the card was generated
    private String date; // Booking date/slot
    private Integer quantity;
    private String status; // PENDING, CONFIRMED, EXPIRED, CANCELLED
    private LocalDateTime createdAt;
    private String reason;

    @Builder.Default
    private int retryCount = 0; // Number of times this card has been re-generated

    public void markConfirmed() {
        this.status = STATUS_CONFIRMED;
    }

    public void markCancelled() {
        this.status = STATUS_CANCELLED;
    }
}
