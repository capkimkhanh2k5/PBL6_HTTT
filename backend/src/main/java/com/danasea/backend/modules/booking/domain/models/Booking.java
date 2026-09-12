package com.danasea.backend.modules.booking.domain.models;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.booking.domain.exceptions.BookingHoldExpiredException;
import com.danasea.backend.modules.booking.domain.exceptions.InvalidBookingStateException;
import com.danasea.backend.modules.booking.domain.exceptions.UnauthorizedBookingAccessException;
import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Booking extends BaseDomainModel {

    private UUID customerId;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private OffsetDateTime holdExpiresAt;

    @Builder.Default
    private List<BookingItem> items = new ArrayList<>();

    public static Booking createHold(UUID customerId, List<BookingItem> items, Duration holdDuration, OffsetDateTime now) {
        return createHold(UUID.randomUUID(), customerId, items, holdDuration, now);
    }

    public static Booking createHold(UUID bookingId, UUID customerId, List<BookingItem> items, Duration holdDuration, OffsetDateTime now) {
        if (bookingId == null) {
            bookingId = UUID.randomUUID();
        }
        if (customerId == null) {
            throw new IllegalArgumentException("customerId cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Booking must contain at least one item");
        }
        if (holdDuration == null || holdDuration.isNegative() || holdDuration.isZero()) {
            throw new IllegalArgumentException("holdDuration must be positive");
        }
        if (now == null) {
            now = OffsetDateTime.now();
        }

        items.forEach(BookingItem::validate);

        BigDecimal calculatedTotal = items.stream()
                .map(BookingItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        UUID finalBookingId = bookingId;
        items.forEach(item -> item.setBookingId(finalBookingId));

        return Booking.builder()
                .id(finalBookingId)
                .customerId(customerId)
                .status(BookingStatus.HOLD)
                .totalAmount(calculatedTotal)
                .holdExpiresAt(now.plus(holdDuration))
                .items(items)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean isHold() {
        return BookingStatus.HOLD.equals(this.status);
    }

    public boolean isPendingPayment() {
        return BookingStatus.PENDING_PAYMENT.equals(this.status);
    }

    public boolean isConfirmed() {
        return BookingStatus.CONFIRMED.equals(this.status);
    }

    public boolean isCancelled() {
        return BookingStatus.CANCELLED.equals(this.status);
    }

    public boolean isExpired(OffsetDateTime now) {
        if (holdExpiresAt == null) {
            return false;
        }
        return now.isAfter(holdExpiresAt);
    }

    public void confirm(OffsetDateTime now) {
        if (isExpired(now)) {
            throw new BookingHoldExpiredException(this.getId());
        }
        if (!isHold() && !isPendingPayment()) {
            throw new InvalidBookingStateException(
                    "Cannot confirm booking in status " + this.status + ". Must be HOLD or PENDING_PAYMENT.");
        }
        this.status = BookingStatus.CONFIRMED;
        setUpdatedAt(now);
    }

    public void cancel(OffsetDateTime now) {
        if (BookingStatus.CANCELLED.equals(this.status)) {
            throw new InvalidBookingStateException("Booking is already cancelled.");
        }
        this.status = BookingStatus.CANCELLED;
        setUpdatedAt(now);
    }

    public void validateOwner(UUID userId) {
        if (this.customerId == null || !this.customerId.equals(userId)) {
            throw new UnauthorizedBookingAccessException(this.getId(), userId);
        }
    }
}
