package com.danasea.backend.modules.service.infrastructure.persistence.entities;

import com.danasea.backend.modules.service.domain.models.SlotStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "service_slots")
public class ServiceSlotJpaEntity extends BaseJpaEntity {

    private UUID serviceId;

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer capacity;

    private Integer bookedCount;

    @Enumerated(EnumType.STRING)
    private SlotStatus status;

}
