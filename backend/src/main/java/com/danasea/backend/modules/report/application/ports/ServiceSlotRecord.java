package com.danasea.backend.modules.report.application.ports;

import com.danasea.backend.modules.service.domain.models.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Projection record chứa thông tin ServiceSlot phục vụ thống kê công suất và tỷ lệ lấp đầy. */
public record ServiceSlotRecord(
        UUID id,
        UUID serviceId,
        UUID vendorId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        Integer bookedCount,
        SlotStatus status) {}
