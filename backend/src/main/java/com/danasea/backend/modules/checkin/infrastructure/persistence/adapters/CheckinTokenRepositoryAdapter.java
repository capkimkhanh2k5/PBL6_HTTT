package com.danasea.backend.modules.checkin.infrastructure.persistence.adapters;

import com.danasea.backend.modules.checkin.domain.models.CheckinToken;
import com.danasea.backend.modules.checkin.domain.ports.CheckinTokenRepositoryPort;
import com.danasea.backend.modules.checkin.infrastructure.persistence.entities.CheckinTokenJpaEntity;
import com.danasea.backend.modules.checkin.infrastructure.persistence.mappers.CheckinTokenMapper;
import com.danasea.backend.modules.checkin.infrastructure.persistence.repositories.JpaCheckinTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CheckinTokenRepositoryAdapter implements CheckinTokenRepositoryPort {

    private final JpaCheckinTokenRepository jpaRepository;
    private final CheckinTokenMapper mapper;

    @Override
    public CheckinToken save(CheckinToken checkinToken) {
        CheckinTokenJpaEntity entity = mapper.toEntity(checkinToken);
        CheckinTokenJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CheckinToken> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CheckinToken> findByQrTokenHash(String qrTokenHash) {
        return jpaRepository.findByQrTokenHash(qrTokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<CheckinToken> findBySubOrderId(UUID subOrderId) {
        return jpaRepository.findBySubOrderId(subOrderId).map(mapper::toDomain);
    }

    @Override
    public List<CheckinToken> findAllBySubOrderId(UUID subOrderId) {
        return jpaRepository.findAllBySubOrderId(subOrderId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean markAsUsedAtomic(String qrTokenHash, UUID staffId, OffsetDateTime usedAt) {
        int updated = jpaRepository.markAsUsedIfUnused(qrTokenHash, staffId, usedAt, OffsetDateTime.now());
        return updated > 0;
    }
}
