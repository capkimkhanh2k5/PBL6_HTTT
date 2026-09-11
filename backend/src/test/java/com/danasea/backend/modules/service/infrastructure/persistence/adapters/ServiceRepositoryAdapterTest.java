package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.mappers.ServiceMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRepositoryAdapterTest {

    @Mock
    private JpaServiceRepository jpaServiceRepository;

    @Mock
    private ServiceMapper serviceMapper;

    @InjectMocks
    private ServiceRepositoryAdapter adapter;

    private Service domainService;
    private ServiceJpaEntity jpaEntity;
    private UUID serviceId;

    @BeforeEach
    void setUp() {
        serviceId = UUID.randomUUID();
        domainService = Service.builder().id(serviceId).name("Paddleboard").build();
        jpaEntity = new ServiceJpaEntity();
        jpaEntity.setId(serviceId);
        jpaEntity.setName("Paddleboard");
    }

    @Test
    @DisplayName("save should convert domain to entity, persist and convert back")
    void save_delegatesCorrectly() {
        when(serviceMapper.toEntity(domainService)).thenReturn(jpaEntity);
        when(jpaServiceRepository.save(jpaEntity)).thenReturn(jpaEntity);
        when(serviceMapper.toDomain(jpaEntity)).thenReturn(domainService);

        Service result = adapter.save(domainService);

        assertEquals(domainService, result);
        verify(jpaServiceRepository).save(jpaEntity);
    }

    @Test
    @DisplayName("findById should find entity and map to domain")
    void findById_found() {
        when(jpaServiceRepository.findById(serviceId)).thenReturn(Optional.of(jpaEntity));
        when(serviceMapper.toDomain(jpaEntity)).thenReturn(domainService);

        Optional<Service> result = adapter.findById(serviceId);

        assertTrue(result.isPresent());
        assertEquals(domainService, result.get());
    }

    @Test
    @DisplayName("findById should return empty when entity not found")
    void findById_notFound() {
        when(jpaServiceRepository.findById(serviceId)).thenReturn(Optional.empty());

        Optional<Service> result = adapter.findById(serviceId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findByVendorId should return mapped list sorted by createdAt desc")
    void findByVendorId_delegates() {
        UUID vendorId = UUID.randomUUID();
        when(jpaServiceRepository.findByVendorIdOrderByCreatedAtDesc(vendorId)).thenReturn(List.of(jpaEntity));
        when(serviceMapper.toDomainList(List.of(jpaEntity))).thenReturn(List.of(domainService));

        List<Service> result = adapter.findByVendorId(vendorId);

        assertEquals(1, result.size());
        assertEquals(domainService, result.get(0));
    }

    @Test
    @DisplayName("findByStatus should return mapped list sorted by createdAt desc")
    void findByStatus_delegates() {
        when(jpaServiceRepository.findByStatusOrderByCreatedAtDesc(ServiceStatus.PENDING_REVIEW)).thenReturn(List.of(jpaEntity));
        when(serviceMapper.toDomainList(List.of(jpaEntity))).thenReturn(List.of(domainService));

        List<Service> result = adapter.findByStatus(ServiceStatus.PENDING_REVIEW);

        assertEquals(1, result.size());
        assertEquals(domainService, result.get(0));
    }

    @Test
    @DisplayName("deleteById should delegate to repository")
    void deleteById_delegates() {
        adapter.deleteById(serviceId);
        verify(jpaServiceRepository).deleteById(serviceId);
    }

    @Test
    @DisplayName("existsById should delegate to repository")
    void existsById_delegates() {
        when(jpaServiceRepository.existsById(serviceId)).thenReturn(true);
        assertTrue(adapter.existsById(serviceId));
    }
}
