package com.danasea.backend.modules.service.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaServiceOptionRepository extends JpaRepository<ServiceOptionJpaEntity, UUID> {

    List<ServiceOptionJpaEntity> findByServiceIdOrderByCreatedAtAsc(UUID serviceId);

    List<ServiceOptionJpaEntity> findByServiceIdAndStatusOrderByCreatedAtAsc(UUID serviceId, OptionStatus status);

    Optional<ServiceOptionJpaEntity> findByIdAndServiceId(UUID id, UUID serviceId);
}
