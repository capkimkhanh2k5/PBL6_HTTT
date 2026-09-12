package com.danasea.backend.modules.booking.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookingItem extends BaseDomainModel {
    private UUID bookingId;
    private UUID serviceId;
    private UUID vendorId;
    private UUID slotId;
    private Integer quantity;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private BigDecimal price;

    public BigDecimal calculateSubtotal() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public void validate() {
        if (serviceId == null) {
            throw new IllegalArgumentException("serviceId cannot be null");
        }
        if (vendorId == null) {
            throw new IllegalArgumentException("vendorId cannot be null");
        }
        if (slotId == null) {
            throw new IllegalArgumentException("slotId cannot be null");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price cannot be negative");
        }
        if (bookingDate == null) {
            throw new IllegalArgumentException("bookingDate cannot be null");
        }
        if (bookingTime == null) {
            throw new IllegalArgumentException("bookingTime cannot be null");
        }
    }
}
