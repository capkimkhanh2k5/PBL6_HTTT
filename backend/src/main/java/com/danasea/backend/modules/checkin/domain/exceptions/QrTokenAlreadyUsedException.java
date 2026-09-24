package com.danasea.backend.modules.checkin.domain.exceptions;

import java.time.OffsetDateTime;
import java.util.UUID;

public class QrTokenAlreadyUsedException extends CheckinAlreadyUsedException {

    public QrTokenAlreadyUsedException(String message, OffsetDateTime usedAt, UUID usedByVendorStaffId) {
        super(message, usedAt, usedByVendorStaffId);
    }

    public QrTokenAlreadyUsedException(OffsetDateTime usedAt, UUID usedByVendorStaffId, String message) {
        super(message, usedAt, usedByVendorStaffId);
    }

    public QrTokenAlreadyUsedException(String message) {
        super(message, null, null);
    }
}
