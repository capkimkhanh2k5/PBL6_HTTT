package com.danasea.backend.modules.settlement.application.usecases;

import com.danasea.backend.modules.settlement.application.dto.SettlementResponse;
import com.danasea.backend.modules.settlement.domain.exceptions.SettlementAlreadyFinalizedException;
import com.danasea.backend.modules.settlement.domain.exceptions.SettlementNotFoundException;
import com.danasea.backend.modules.settlement.domain.models.SettlementStatus;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementJpaEntity;
import com.danasea.backend.modules.settlement.infrastructure.persistence.mappers.SettlementMapper;
import com.danasea.backend.modules.settlement.infrastructure.persistence.repositories.JpaSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinalizeSettlementUseCase {

    private final JpaSettlementRepository settlementRepository;
    private final SettlementMapper mapper;

    @Transactional
    public SettlementResponse execute(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Mã kỳ đối soát không được để trống");
        }

        SettlementJpaEntity settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Không tìm thấy kỳ đối soát với ID: " + id));

        if (settlement.getStatus() == SettlementStatus.FINALIZED || settlement.getStatus() == SettlementStatus.PAID) {
            throw new SettlementAlreadyFinalizedException("Kỳ đối soát này đã được chốt (FINALIZED/PAID) trước đó and cannot be finalized again.");
        }

        settlement.setStatus(SettlementStatus.FINALIZED);
        settlement.setUpdatedAt(OffsetDateTime.now());
        SettlementJpaEntity saved = settlementRepository.save(settlement);

        log.info("Settlement {} finalized successfully", id);
        return mapper.toResponse(saved);
    }
}
