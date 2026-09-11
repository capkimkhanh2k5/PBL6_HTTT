package com.danasea.backend.modules.service.infrastructure.persistence.mappers;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.infrastructure.persistence.entities.CategoryJpaEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryMapperTest {

    private CategoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CategoryMapper();
    }

    @Test
    @DisplayName("toDomain with full entity")
    void toDomain_fullEntity() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(id);
        entity.setName("Water Sports");
        entity.setNameEn("Water Sports EN");
        entity.setSlug("water-sports");
        entity.setParentId(parentId);
        entity.setIconUrl("https://icon.png");
        entity.setIsActive(true);

        Category domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals("Water Sports", domain.getName());
        assertEquals("water-sports", domain.getSlug());
        assertEquals(parentId, domain.getParentId());
        assertEquals("https://icon.png", domain.getIconUrl());
        assertTrue(domain.getIsActive());
    }

    @Test
    @DisplayName("toDomain with null entity")
    void toDomain_null_returnsNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    @DisplayName("toEntity with full domain and defaults")
    void toEntity_fullDomain() {
        Category domain = Category.builder()
                .id(UUID.randomUUID())
                .name("Boating")
                .slug("boating")
                .build();

        CategoryJpaEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals("Boating", entity.getName());
        assertTrue(entity.getIsActive()); // default true
    }

    @Test
    @DisplayName("toEntity with null domain")
    void toEntity_null_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("List mapping")
    void listMapping() {
        assertTrue(mapper.toDomainList(null).isEmpty());
        assertTrue(mapper.toDomainList(Collections.emptyList()).isEmpty());
        assertTrue(mapper.toEntityList(null).isEmpty());
        assertTrue(mapper.toEntityList(Collections.emptyList()).isEmpty());

        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setName("Diving");
        List<Category> list = mapper.toDomainList(List.of(entity));
        assertEquals(1, list.size());
        assertEquals("Diving", list.get(0).getName());
    }
}
