package com.danasea.backend.modules.service.application.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiItinerarySlotReader implements AiItinerarySlotReadApi {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> futureSlotIds(Set<UUID> slotIds) {
        if (slotIds.isEmpty()) return Set.of();
        return new HashSet<>(entityManager.createQuery("select s.id from ServiceSlotJpaEntity s where s.id in :ids and "
                + "(s.date>:today or (s.date=:today and s.startTime>:now))", UUID.class)
                .setParameter("ids", slotIds).setParameter("today", LocalDate.now(ZONE)).setParameter("now", LocalTime.now(ZONE)).getResultList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Signal> unavailableSignals(Set<UUID> slotIds) {
        if (slotIds.isEmpty()) return List.of();
        List<ServiceSlotJpaEntity> slots = entityManager.createQuery(
                "select s from ServiceSlotJpaEntity s where s.id in :ids and s.status<>com.danasea.backend.modules.service.domain.models.SlotStatus.OPEN and "
                + "(s.date>:today or (s.date=:today and s.startTime>:now))", ServiceSlotJpaEntity.class)
                .setParameter("ids", slotIds).setParameter("today", LocalDate.now(ZONE)).setParameter("now", LocalTime.now(ZONE)).getResultList();
        return slots.stream().map(s -> new Signal(s.getId(), "SLOT:" + s.getId() + ":" + s.getStatus() + ":" + s.getUpdatedAt(),
                "CANONICAL_SLOT_UNAVAILABLE", s.getUpdatedAt())).toList();
    }
}
