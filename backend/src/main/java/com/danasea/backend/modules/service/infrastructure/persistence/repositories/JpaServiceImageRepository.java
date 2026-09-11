package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaServiceImageRepository extends JpaRepository<ServiceImageJpaEntity, UUID> {

    long countByServiceId(UUID serviceId);

    List<ServiceImageJpaEntity> findByServiceIdOrderBySortOrderAsc(UUID serviceId);

    @Query("SELECT MAX(i.sortOrder) FROM ServiceImageJpaEntity i WHERE i.serviceId = :serviceId")
    Optional<Short> findMaxSortOrderByServiceId(@Param("serviceId") UUID serviceId);

    Optional<ServiceImageJpaEntity> findByIdAndServiceId(UUID id, UUID serviceId);

    void deleteByIdAndServiceId(UUID id, UUID serviceId);
}
