package com.danasea.backend.modules.settlement.application.usecases;

import com.danasea.backend.modules.booking.domain.ports.VendorLookupPort;
import com.danasea.backend.modules.settlement.application.dto.SettlementDetailResponse;
import com.danasea.backend.modules.settlement.domain.exceptions.SettlementNotFoundException;
import com.danasea.backend.modules.settlement.domain.exceptions.UnauthorizedSettlementAccessException;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementJpaEntity;
import com.danasea.backend.modules.settlement.infrastructure.persistence.entities.SettlementLineItemJpaEntity;
import com.danasea.backend.modules.settlement.infrastructure.persistence.mappers.SettlementMapper;
import com.danasea.backend.modules.settlement.infrastructure.persistence.repositories.JpaSettlementLineItemRepository;
import com.danasea.backend.modules.settlement.infrastructure.persistence.repositories.JpaSettlementRepository;
import com.danasea.backend.security.infrastructure.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetSettlementDetailUseCase {

    private final JpaSettlementRepository settlementRepository;
    private final JpaSettlementLineItemRepository lineItemRepository;
    private final VendorLookupPort vendorLookupPort;
    private final SettlementMapper mapper;

    @Transactional(readOnly = true)
    public SettlementDetailResponse getAdminSettlementDetail(UUID id) {
        SettlementJpaEntity settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Không tìm thấy kỳ đối soát: " + id));

        List<SettlementLineItemJpaEntity> lineItems = lineItemRepository.findBySettlementId(id);
        return mapper.toDetailResponse(settlement, lineItems);
    }

    @Transactional(readOnly = true)
    public SettlementDetailResponse getVendorSettlementDetail(UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSettlementAccessException("User chưa được xác thực"));
        UUID vendorId = vendorLookupPort.findVendorIdByUserId(currentUserId)
                .orElseThrow(() -> new UnauthorizedSettlementAccessException("Không tìm thấy hồ sơ Vendor liên kết với tài khoản này"));

        SettlementJpaEntity settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException("Không tìm thấy kỳ đối soát: " + id));

        if (!Objects.equals(settlement.getVendorId(), vendorId)) {
            log.warn("SECURITY_ALERT: Vendor {} cố ý truy cập trái phép kỳ đối soát {} của Vendor {}",
                    vendorId, settlement.getId(), settlement.getVendorId());
            throw new UnauthorizedSettlementAccessException(
                    String.format("Bạn không có quyền xem kỳ đối soát của Vendor khác (kỳ này thuộc vendor: %s)", settlement.getVendorId()));
        }

        List<SettlementLineItemJpaEntity> lineItems = lineItemRepository.findBySettlementId(id);
        return mapper.toDetailResponse(settlement, lineItems);
    }
}
