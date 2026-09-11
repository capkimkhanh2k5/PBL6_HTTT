package com.danasea.backend.modules.service.application.usecase;

import java.util.UUID;

import com.danasea.backend.modules.service.application.port.output.ActiveServiceCheckPort;
import com.danasea.backend.modules.service.application.port.output.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.exception.CategoryHasActiveServicesException;
import com.danasea.backend.modules.service.domain.exception.CategoryNotFoundException;
import com.danasea.backend.modules.service.domain.models.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeactivateCategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ActiveServiceCheckPort activeServiceCheckPort;

    public Category execute(UUID id) {
        Category category = categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        if (activeServiceCheckPort.hasActiveServices(id)) {
            throw new CategoryHasActiveServicesException(id);
        }

        category.setIsActive(false);
        return categoryRepositoryPort.save(category);
    }
}
