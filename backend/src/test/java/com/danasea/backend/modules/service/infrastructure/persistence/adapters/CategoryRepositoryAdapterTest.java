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

import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;
import com.danasea.backend.modules.service.infrastructure.mapper.CategoryMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaCategoryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryRepositoryAdapterTest {

    @Mock
    private JpaCategoryRepository jpaCategoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryRepositoryAdapter adapter;

    private UUID categoryId;
    private CategoryJpaEntity jpaEntity;
    private Category domainCategory;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        jpaEntity = new CategoryJpaEntity();
        jpaEntity.setId(categoryId);
        domainCategory = Category.builder().id(categoryId).name("Kayaking").build();
    }

    @Test
    @DisplayName("findById should map entity to domain")
    void findById_found() {
        when(jpaCategoryRepository.findById(categoryId)).thenReturn(Optional.of(jpaEntity));
        when(categoryMapper.toDomain(jpaEntity)).thenReturn(domainCategory);

        Optional<Category> result = adapter.findById(categoryId);

        assertTrue(result.isPresent());
        assertEquals(domainCategory, result.get());
    }

    @Test
    @DisplayName("findById should return empty when not found")
    void findById_notFound() {
        when(jpaCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        Optional<Category> result = adapter.findById(categoryId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("existsById should delegate")
    void existsById_delegates() {
        when(jpaCategoryRepository.existsById(categoryId)).thenReturn(true);
        assertTrue(adapter.existsById(categoryId));
    }

    @Test
    @DisplayName("findAll should map list")
    void findAll_delegates() {
        when(jpaCategoryRepository.findAll()).thenReturn(List.of(jpaEntity));
        when(categoryMapper.toDomainList(List.of(jpaEntity))).thenReturn(List.of(domainCategory));

        List<Category> list = adapter.findAll();
        assertEquals(1, list.size());
        assertEquals(domainCategory, list.get(0));
    }
}
