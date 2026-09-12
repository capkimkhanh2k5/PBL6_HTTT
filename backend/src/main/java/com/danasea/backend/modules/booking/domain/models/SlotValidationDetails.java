package com.danasea.backend.modules.booking.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotValidationDetails {
    private UUID slotId;
    private UUID serviceId;
    private UUID vendorId;
    private String serviceName;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private Integer capacity;
    private Integer bookedCount;
    private SlotStatus status;
    private BigDecimal price;
    private boolean servicePublished;

    public boolean isOpen() {
        return SlotStatus.OPEN.equals(this.status);
    }

    public int getAvailableCapacity() {
        int cap = capacity != null ? capacity : 0;
        int booked = bookedCount != null ? bookedCount : 0;
        return Math.max(0, cap - booked);
    }
}
