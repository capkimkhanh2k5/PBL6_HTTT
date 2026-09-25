package com.danasea.backend.modules.settlement.infrastructure.persistence.repositories;

import com.danasea.backend.modules.settlement.domain.models.SettlementStatus;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository("settlementJpaSettlementRepository")
public interface JpaSettlementRepository extends JpaRepository<SettlementJpaEntity, UUID>, JpaSpecificationExecutor<SettlementJpaEntity> {

    Optional<SettlementJpaEntity> findByVendorIdAndPeriodStartAndPeriodEnd(
            UUID vendorId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SettlementJpaEntity s WHERE s.vendorId = :vendorId "
            + "AND s.periodStart = :periodStart AND s.periodEnd = :periodEnd")
    Optional<SettlementJpaEntity> findByVendorIdAndPeriodForUpdate(
            @Param("vendorId") UUID vendorId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SettlementJpaEntity s WHERE s.id = :id")
    Optional<SettlementJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    Page<SettlementJpaEntity> findByVendorId(UUID vendorId, Pageable pageable);

    Page<SettlementJpaEntity> findByVendorIdAndStatus(UUID vendorId, SettlementStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM SettlementJpaEntity s WHERE " +
           "(:vendorId IS NULL OR s.vendorId = :vendorId) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(cast(:from as date) IS NULL OR s.periodStart >= :from) AND " +
           "(cast(:to as date) IS NULL OR s.periodEnd <= :to)")
    Page<SettlementJpaEntity> findFiltered(
            @org.springframework.data.repository.query.Param("vendorId") UUID vendorId,
            @org.springframework.data.repository.query.Param("status") SettlementStatus status,
            @org.springframework.data.repository.query.Param("from") LocalDate from,
            @org.springframework.data.repository.query.Param("to") LocalDate to,
            Pageable pageable);
}
