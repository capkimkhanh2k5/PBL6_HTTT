package com.danasea.backend.modules.checkin.domain.ports;

import com.danasea.backend.modules.checkin.domain.models.CheckinToken;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinTokenRepositoryPort {

    CheckinToken save(CheckinToken checkinToken);

    Optional<CheckinToken> findById(UUID id);

    Optional<CheckinToken> findByQrTokenHash(String qrTokenHash);

    Optional<CheckinToken> findBySubOrderId(UUID subOrderId);

    List<CheckinToken> findAllBySubOrderId(UUID subOrderId);

    boolean markAsUsedAtomic(String qrTokenHash, UUID staffId, OffsetDateTime usedAt);
}
