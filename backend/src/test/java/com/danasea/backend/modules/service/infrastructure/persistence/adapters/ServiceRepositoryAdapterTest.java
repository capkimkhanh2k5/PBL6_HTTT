package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.mapper.ServiceMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ServiceRepositoryAdapterTest {

    private JpaServiceRepository jpaServiceRepository;
    private ServiceMapper serviceMapper;
    private ServiceRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaServiceRepository = mock(JpaServiceRepository.class);
        serviceMapper = mock(ServiceMapper.class);
        adapter = new ServiceRepositoryAdapter(jpaServiceRepository, serviceMapper);
    }

    @Test
    void findById_whenFound_returnsMappedDomain() {
        UUID id = UUID.randomUUID();
        ServiceJpaEntity entity = new ServiceJpaEntity();
        Service domain = new Service();
        domain.setId(id);

        when(jpaServiceRepository.findById(id)).thenReturn(Optional.of(entity));
        when(serviceMapper.toDomain(entity)).thenReturn(domain);

        Optional<Service> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void findPublishedById_whenPublished_returnsMappedDomain() {
        UUID id = UUID.randomUUID();
        ServiceJpaEntity entity = new ServiceJpaEntity();
        entity.setStatus(ServiceStatus.PUBLISHED);
        Service domain = new Service();
        domain.setId(id);
        domain.setStatus(ServiceStatus.PUBLISHED);

        when(jpaServiceRepository.findByIdAndStatus(id, ServiceStatus.PUBLISHED)).thenReturn(Optional.of(entity));
        when(serviceMapper.toDomain(entity)).thenReturn(domain);

        Optional<Service> result = adapter.findPublishedById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals(ServiceStatus.PUBLISHED, result.get().getStatus());
    }

    @Test
    void incrementViewCount_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        when(jpaServiceRepository.incrementViewCount(id, ServiceStatus.PUBLISHED)).thenReturn(1);

        int count = adapter.incrementViewCount(id, ServiceStatus.PUBLISHED);

        assertEquals(1, count);
        verify(jpaServiceRepository).incrementViewCount(id, ServiceStatus.PUBLISHED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchPublishedServices_executesSpecificationAndReturnsList() {
        ServiceJpaEntity entity = new ServiceJpaEntity();
        Service domain = new Service();
        when(jpaServiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(serviceMapper.toDomain(entity)).thenReturn(domain);

        List<Service> results = adapter.searchPublishedServices(
                null, "test", BigDecimal.ONE, BigDecimal.TEN, null, null, null, 0, 10
        );

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void countPublishedServices_executesSpecificationAndReturnsCount() {
        when(jpaServiceRepository.count(any(Specification.class))).thenReturn(5L);

        long count = adapter.countPublishedServices(null, null, null, null, null, null, null);

        assertEquals(5L, count);
    }

    @Test
    void save_mapsAndSaves() {
        Service domain = new Service();
        ServiceJpaEntity entity = new ServiceJpaEntity();
        when(serviceMapper.toEntity(domain)).thenReturn(entity);
        when(jpaServiceRepository.save(entity)).thenReturn(entity);
        when(serviceMapper.toDomain(entity)).thenReturn(domain);

        Service saved = adapter.save(domain);

        assertNotNull(saved);
        verify(jpaServiceRepository).save(entity);
    }

    @Test
    void existsById_delegates() {
        UUID id = UUID.randomUUID();
        when(jpaServiceRepository.existsById(id)).thenReturn(true);

        assertTrue(adapter.existsById(id));
    }

    @Test
    void deleteById_delegates() {
        UUID id = UUID.randomUUID();
        adapter.deleteById(id);

        verify(jpaServiceRepository).deleteById(id);
    }
}
