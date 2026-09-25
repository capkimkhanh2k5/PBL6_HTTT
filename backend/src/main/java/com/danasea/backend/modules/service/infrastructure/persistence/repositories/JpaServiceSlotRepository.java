package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

import com.danasea.backend.modules.service.domain.models.SlotStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceSlotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaServiceSlotRepository extends JpaRepository<ServiceSlotJpaEntity, UUID> {

    List<ServiceSlotJpaEntity> findByServiceIdAndStatusAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            UUID serviceId,
            SlotStatus status,
            LocalDate date);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ServiceSlotJpaEntity s SET s.bookedCount = s.bookedCount + :quantity WHERE s.id = :slotId AND (s.capacity - s.bookedCount) >= :quantity")
    int incrementBookedCount(@Param("slotId") UUID slotId, @Param("quantity") int quantity);

    java.util.List<ServiceSlotJpaEntity> findByDateBetween(java.time.LocalDate start, java.time.LocalDate end);
    java.util.List<ServiceSlotJpaEntity> findByDate(java.time.LocalDate date);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ServiceSlotJpaEntity s SET s.bookedCount = GREATEST(0, s.bookedCount - :quantity) WHERE s.id = :slotId")
    int decrementBookedCount(@Param("slotId") UUID slotId, @Param("quantity") int quantity);
}
