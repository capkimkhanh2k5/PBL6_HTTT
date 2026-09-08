package com.danasea.backend.modules.systemconfig.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.systemconfig.infrastructure.persistence.entities.SystemConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaSystemConfigRepository extends JpaRepository<SystemConfigJpaEntity, UUID> {
}
