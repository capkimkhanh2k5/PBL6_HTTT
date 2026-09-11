package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.mappers.ServiceImageMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceImageRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ServiceImageRepositoryAdapter implements ServiceImageRepositoryPort {

    private final JpaServiceImageRepository jpaServiceImageRepository;
    private final ServiceImageMapper serviceImageMapper;

    @Override
    public List<ServiceImage> findByServiceId(UUID serviceId) {
        List<ServiceImageJpaEntity> entities = jpaServiceImageRepository.findByServiceIdOrderBySortOrderAsc(serviceId);
        return serviceImageMapper.toDomainList(entities);
    }

    @Override
    public List<ServiceImage> saveAll(List<ServiceImage> images) {
        List<ServiceImageJpaEntity> entities = serviceImageMapper.toEntityList(images);
        List<ServiceImageJpaEntity> saved = jpaServiceImageRepository.saveAll(entities);
        return serviceImageMapper.toDomainList(saved);
    }

    @Override
    public void deleteByServiceId(UUID serviceId) {
        jpaServiceImageRepository.deleteByServiceId(serviceId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaServiceImageRepository.deleteById(id);
    }

    @Override
    public boolean existsByServiceId(UUID serviceId) {
        return jpaServiceImageRepository.existsByServiceId(serviceId);
    }
}
