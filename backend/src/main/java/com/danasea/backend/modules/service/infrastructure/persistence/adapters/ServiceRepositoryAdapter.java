package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.mappers.ServiceMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import com.danasea.backend.modules.service.infrastructure.persistence.specifications.ServiceSpecifications;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ServiceRepositoryAdapter implements ServiceRepositoryPort {

    private final JpaServiceRepository jpaServiceRepository;
    private final ServiceMapper serviceMapper;

    @Override
    @Transactional
    public Service save(Service service) {
        ServiceJpaEntity entity = serviceMapper.toEntity(service);
        ServiceJpaEntity saved = jpaServiceRepository.save(entity);
        return serviceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Service> findById(UUID id) {
        return jpaServiceRepository.findById(id).map(serviceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Service> findByVendorId(UUID vendorId) {
        List<ServiceJpaEntity> entities = jpaServiceRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
        return serviceMapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Service> findByStatus(ServiceStatus status) {
        List<ServiceJpaEntity> entities = jpaServiceRepository.findByStatusOrderByCreatedAtDesc(status);
        return serviceMapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Service> findAll() {
        return serviceMapper.toDomainList(jpaServiceRepository.findAll());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaServiceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return jpaServiceRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Service> findPublishedById(UUID id) {
        return jpaServiceRepository.findByIdAndStatus(id, ServiceStatus.PUBLISHED)
                .map(serviceMapper::toDomain);
    }

    @Override
    @Transactional
    public int incrementViewCount(UUID id, ServiceStatus status) {
        return jpaServiceRepository.incrementViewCount(id, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Service> searchPublishedServices(
            UUID categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal lat,
            BigDecimal lng,
            Double radiusKm,
            int page,
            int size
    ) {
        Specification<ServiceJpaEntity> spec = ServiceSpecifications.filter(
                categoryId, keyword, minPrice, maxPrice, lat, lng, radiusKm
        );
        int pageIndex = Math.max(0, page);
        int pageSize = (size <= 0) ? 20 : size;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaServiceRepository.findAll(spec, pageable)
                .map(serviceMapper::toDomain)
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPublishedServices(
            UUID categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal lat,
            BigDecimal lng,
            Double radiusKm
    ) {
        Specification<ServiceJpaEntity> spec = ServiceSpecifications.filter(
                categoryId, keyword, minPrice, maxPrice, lat, lng, radiusKm
        );
        return jpaServiceRepository.count(spec);
    }
}
