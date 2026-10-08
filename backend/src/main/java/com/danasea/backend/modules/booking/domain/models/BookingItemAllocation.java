package com.danasea.backend.modules.booking.domain.models;

import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookingItemAllocation extends BaseDomainModel {
    private UUID bookingItemId;
    private UUID slotId;
    private Integer unitNumber;
    private Integer allocatedSeats;
    private Boolean isPrivateLock;
}
