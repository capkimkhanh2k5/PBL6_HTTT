package com.danasea.backend.modules.communication.infrastructure.persistence.repositories;

import com.danasea.backend.modules.communication.infrastructure.persistence.entities.ConversationJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaConversationRepository extends JpaRepository<ConversationJpaEntity, UUID> {

    Optional<ConversationJpaEntity> findByCustomerIdAndVendorIdAndMasterOrderId(UUID customerId, UUID vendorId, UUID masterOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConversationJpaEntity c WHERE c.id = :id")
    Optional<ConversationJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<ConversationJpaEntity> findByMasterOrderId(UUID masterOrderId);

    Page<ConversationJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);

    Page<ConversationJpaEntity> findByVendorId(UUID vendorId, Pageable pageable);

    Page<ConversationJpaEntity> findByCustomerIdOrVendorId(UUID customerId, UUID vendorId, Pageable pageable);
}
