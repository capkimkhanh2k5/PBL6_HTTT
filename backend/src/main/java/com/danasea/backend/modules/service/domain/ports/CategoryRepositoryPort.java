package com.danasea.backend.modules.service.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.domain.models.Category;

public interface CategoryRepositoryPort {
    Optional<Category> findById(UUID id);

    boolean existsById(UUID id);

    List<Category> findAll();
}
