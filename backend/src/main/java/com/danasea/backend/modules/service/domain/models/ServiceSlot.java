package com.danasea.backend.modules.service.domain.models;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceSlot extends BaseDomainModel {
    private UUID serviceId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Integer bookedCount;
    private SlotStatus status;
}
