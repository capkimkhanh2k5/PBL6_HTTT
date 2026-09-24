package com.danasea.backend.modules.checkin.domain.exceptions;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class CheckinAlreadyUsedException extends RuntimeException {
    private final OffsetDateTime usedAt;
    private final UUID usedByVendorStaffId;

    public CheckinAlreadyUsedException(String message, OffsetDateTime usedAt, UUID usedByVendorStaffId) {
        super(message);
        this.usedAt = usedAt;
        this.usedByVendorStaffId = usedByVendorStaffId;
    }

    public CheckinAlreadyUsedException(String message) {
        this(message, null, null);
    }
}
