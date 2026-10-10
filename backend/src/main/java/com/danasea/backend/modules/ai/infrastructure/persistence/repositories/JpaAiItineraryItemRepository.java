package com.danasea.backend.modules.ai.infrastructure.persistence.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.danasea.backend.modules.ai.infrastructure.persistence.entities.AiItineraryItemJpaEntity;


public interface JpaAiItineraryItemRepository extends JpaRepository<AiItineraryItemJpaEntity, UUID> {
    void deleteByItineraryId(UUID itineraryId);
    @Query("select distinct i.itineraryId from AiItineraryItemJpaEntity i, AiItineraryJpaEntity p where i.itineraryId=p.id and i.slotId=:slotId and p.lifecycle in ('ACCEPTED','STALE') and i.activityDate>=:today")
    List<UUID> trackedForSlot(@Param("slotId") UUID slotId, @Param("today") LocalDate today);
    @Query("select distinct i.slotId from AiItineraryItemJpaEntity i, AiItineraryJpaEntity p where i.itineraryId=p.id and p.lifecycle in ('ACCEPTED','STALE') and i.activityDate>=:today order by i.slotId")
    List<UUID> trackedSlots(@Param("today") LocalDate today, Pageable page);
}
