package com.danasea.backend.modules.checkin.presentation.dtos;

import com.danasea.backend.modules.order.domain.models.SubOrderStatus;
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
public class VerifyCheckinResponse {

    private UUID subOrderId;
    private UUID vendorId;
    private UUID serviceId;
    private UUID slotId;
    private Integer quantity;
    private SubOrderStatus status;
    private OffsetDateTime checkedInAt;
    private UUID verifiedByStaffId;
    private String message;

    // Record-style accessors for compatibility
    public UUID subOrderId() {
        return subOrderId;
    }

    public UUID vendorId() {
        return vendorId;
    }

    public UUID serviceId() {
        return serviceId;
    }

    public UUID slotId() {
        return slotId;
    }

    public Integer quantity() {
        return quantity;
    }

    public SubOrderStatus status() {
        return status;
    }

    public OffsetDateTime checkedInAt() {
        return checkedInAt;
    }

    public UUID verifiedByStaffId() {
        return verifiedByStaffId;
    }

    public String message() {
        return message;
    }
}
