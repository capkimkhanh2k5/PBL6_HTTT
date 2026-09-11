package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.mappers.ServiceMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ServiceRepositoryAdapter implements ServiceRepositoryPort {

    private final JpaServiceRepository jpaServiceRepository;
    private final ServiceMapper serviceMapper;

    @Override
    public Service save(Service service) {
        ServiceJpaEntity entity = serviceMapper.toEntity(service);
        ServiceJpaEntity saved = jpaServiceRepository.save(entity);
        return serviceMapper.toDomain(saved);
    }

    @Override
    public Optional<Service> findById(UUID id) {
        return jpaServiceRepository.findById(id).map(serviceMapper::toDomain);
    }

    @Override
    public List<Service> findByVendorId(UUID vendorId) {
        List<ServiceJpaEntity> entities = jpaServiceRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
        return serviceMapper.toDomainList(entities);
    }

    @Override
    public List<Service> findByStatus(ServiceStatus status) {
        List<ServiceJpaEntity> entities = jpaServiceRepository.findByStatusOrderByCreatedAtDesc(status);
        return serviceMapper.toDomainList(entities);
    }

    @Override
    public List<Service> findAll() {
        return serviceMapper.toDomainList(jpaServiceRepository.findAll());
    }

    @Override
    public void deleteById(UUID id) {
        jpaServiceRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaServiceRepository.existsById(id);
    }
}
