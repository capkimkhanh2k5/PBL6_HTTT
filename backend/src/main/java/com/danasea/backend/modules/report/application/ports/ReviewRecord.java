package com.danasea.backend.modules.report.application.ports;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection record chứa thông tin đánh giá Review phục vụ thống kê chất lượng Vendor. */
public record ReviewRecord(
        UUID id,
        UUID subOrderId,
        UUID vendorId,
        UUID serviceId,
        Short rating,
        OffsetDateTime createdAt) {}
