package com.danasea.backend.modules.order.infrastructure.persistence.repositories;

import java.util.UUID;

import com.danasea.backend.modules.order.infrastructure.persistence.entities.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JpaPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {
}
