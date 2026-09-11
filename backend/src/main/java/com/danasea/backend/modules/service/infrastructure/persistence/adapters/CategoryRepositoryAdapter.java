package com.danasea.backend.modules.service.infrastructure.persistence.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.domain.ports.CategoryRepositoryPort;
import com.danasea.backend.modules.service.infrastructure.mapper.CategoryMapper;
import com.danasea.backend.modules.service.infrastructure.persistence.repositories.JpaCategoryRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final JpaCategoryRepository jpaCategoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaCategoryRepository.findById(id).map(categoryMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaCategoryRepository.existsById(id);
    }

    @Override
    public List<Category> findAll() {
        return categoryMapper.toDomainList(jpaCategoryRepository.findAll());
    }
}
