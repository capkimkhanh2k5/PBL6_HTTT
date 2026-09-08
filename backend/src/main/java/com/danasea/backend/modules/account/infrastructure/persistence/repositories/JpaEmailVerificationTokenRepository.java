package com.danasea.backend.modules.account.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.account.infrastructure.persistence.entities.EmailVerificationTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaEmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenJpaEntity, UUID> {
}
