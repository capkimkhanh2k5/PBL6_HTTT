package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.ServiceImageJpaEntity;
import com.danasea.backend.modules.service.infrastructure.persistence.mappers.ServiceImageMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaServiceImageRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceImageRepositoryAdapterTest {

    @Mock
    private JpaServiceImageRepository jpaServiceImageRepository;

    @Mock
    private ServiceImageMapper serviceImageMapper;

    @InjectMocks
    private ServiceImageRepositoryAdapter adapter;

    private UUID serviceId;
    private ServiceImage domainImage;
    private ServiceImageJpaEntity jpaEntity;

    @BeforeEach
    void setUp() {
        serviceId = UUID.randomUUID();
        domainImage = ServiceImage.builder().serviceId(serviceId).url("https://img.jpg").sortOrder((short) 1).build();
        jpaEntity = new ServiceImageJpaEntity();
        jpaEntity.setServiceId(serviceId);
        jpaEntity.setUrl("https://img.jpg");
        jpaEntity.setSortOrder((short) 1);
    }

    @Test
    @DisplayName("findByServiceId should return mapped entities ordered by sortOrder asc")
    void findByServiceId_delegates() {
        when(jpaServiceImageRepository.findByServiceIdOrderBySortOrderAsc(serviceId)).thenReturn(List.of(jpaEntity));
        when(serviceImageMapper.toDomainList(List.of(jpaEntity))).thenReturn(List.of(domainImage));

        List<ServiceImage> images = adapter.findByServiceId(serviceId);

        assertEquals(1, images.size());
        assertEquals(domainImage, images.get(0));
    }

    @Test
    @DisplayName("saveAll should convert domains to entities, saveAll and convert back")
    void saveAll_delegates() {
        List<ServiceImage> domains = List.of(domainImage);
        List<ServiceImageJpaEntity> entities = List.of(jpaEntity);

        when(serviceImageMapper.toEntityList(domains)).thenReturn(entities);
        when(jpaServiceImageRepository.saveAll(entities)).thenReturn(entities);
        when(serviceImageMapper.toDomainList(entities)).thenReturn(domains);

        List<ServiceImage> result = adapter.saveAll(domains);

        assertEquals(1, result.size());
        assertEquals(domainImage, result.get(0));
    }

    @Test
    @DisplayName("deleteByServiceId should delegate")
    void deleteByServiceId_delegates() {
        adapter.deleteByServiceId(serviceId);
        verify(jpaServiceImageRepository).deleteByServiceId(serviceId);
    }

    @Test
    @DisplayName("deleteById should delegate")
    void deleteById_delegates() {
        UUID imageId = UUID.randomUUID();
        adapter.deleteById(imageId);
        verify(jpaServiceImageRepository).deleteById(imageId);
    }

    @Test
    @DisplayName("existsByServiceId should delegate")
    void existsByServiceId_delegates() {
        when(jpaServiceImageRepository.existsByServiceId(serviceId)).thenReturn(true);
        assertTrue(adapter.existsByServiceId(serviceId));
    }
}
