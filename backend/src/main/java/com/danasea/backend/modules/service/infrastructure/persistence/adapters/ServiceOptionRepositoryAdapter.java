package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.OptionStatus;
import com.danasea.backend.modules.service.domain.models.ServiceOption;
import com.danasea.backend.modules.service.domain.ports.ServiceOptionRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceOptionJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ServiceOptionRepositoryAdapter implements ServiceOptionRepositoryPort {

    private final JpaServiceOptionRepository jpaServiceOptionRepository;

    @Override
    @Transactional
    public ServiceOption save(ServiceOption option) {
        ServiceOptionJpaEntity entity = toEntity(option);
        ServiceOptionJpaEntity saved = jpaServiceOptionRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceOption> findById(UUID id) {
        return jpaServiceOptionRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceOption> findByServiceId(UUID serviceId) {
        return jpaServiceOptionRepository.findByServiceIdOrderByCreatedAtAsc(serviceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceOption> findByServiceIdAndStatus(UUID serviceId, OptionStatus status) {
        return jpaServiceOptionRepository.findByServiceIdAndStatusOrderByCreatedAtAsc(serviceId, status).stream()
                .map(this::toDomain)
                .toList();
    }

    private ServiceOptionJpaEntity toEntity(ServiceOption domain) {
        if (domain == null) return null;
        ServiceOptionJpaEntity entity = ServiceOptionJpaEntity.builder()
                .serviceId(domain.getServiceId())
                .name(domain.getName())
                .optionType(domain.getOptionType())
                .pricingUnit(domain.getPricingUnit())
                .price(domain.getPrice())
                .maxPaxPerPackage(domain.getMaxPaxPerPackage())
                .benefits(domain.getBenefits())
                .status(domain.getStatus() != null ? domain.getStatus() : OptionStatus.ACTIVE)
                .build();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private ServiceOption toDomain(ServiceOptionJpaEntity entity) {
        if (entity == null) return null;
        ServiceOption domain = ServiceOption.builder()
                .serviceId(entity.getServiceId())
                .name(entity.getName())
                .optionType(entity.getOptionType())
                .pricingUnit(entity.getPricingUnit())
                .price(entity.getPrice())
                .maxPaxPerPackage(entity.getMaxPaxPerPackage())
                .benefits(entity.getBenefits())
                .status(entity.getStatus())
                .build();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }
}
