package com.danasea.backend.modules.service.application.usecases;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.application.ports.output.ActiveServiceCheckPort;
import com.danasea.backend.modules.service.application.ports.output.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.exceptions.CategoryHasActiveServicesException;
import com.danasea.backend.modules.service.domain.exceptions.CategoryNotFoundException;
import com.danasea.backend.modules.service.domain.models.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeactivateCategoryUseCaseTest {

    private CategoryRepositoryPort categoryRepositoryPort;
    private ActiveServiceCheckPort activeServiceCheckPort;
    private DeactivateCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        categoryRepositoryPort = mock(CategoryRepositoryPort.class);
        activeServiceCheckPort = mock(ActiveServiceCheckPort.class);
        useCase = new DeactivateCategoryUseCase(categoryRepositoryPort, activeServiceCheckPort);
    }

    @Test
    @DisplayName("Should throw CategoryHasActiveServicesException when category has active services")
    void shouldThrowCategoryHasActiveServicesException_WhenCategoryHasActiveServices() {
        // Given
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Tours")
                .slug("tours")
                .isActive(true)
                .build();

        when(categoryRepositoryPort.findById(categoryId)).thenReturn(Optional.of(category));
        when(activeServiceCheckPort.hasActiveServices(categoryId)).thenReturn(true);

        // When & Then
        CategoryHasActiveServicesException exception = assertThrows(
                CategoryHasActiveServicesException.class,
                () -> useCase.execute(categoryId)
        );
        assertTrue(exception.getMessage().contains(categoryId.toString()));
        verify(categoryRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should deactivate category successfully when category has no active services")
    void shouldDeactivateCategorySuccessfully_WhenNoActiveServices() {
        // Given
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Tours")
                .slug("tours")
                .isActive(true)
                .build();

        when(categoryRepositoryPort.findById(categoryId)).thenReturn(Optional.of(category));
        when(activeServiceCheckPort.hasActiveServices(categoryId)).thenReturn(false);
        when(categoryRepositoryPort.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Category result = useCase.execute(categoryId);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsActive());
        verify(categoryRepositoryPort).save(category);
    }

    @Test
    @DisplayName("Should throw CategoryNotFoundException when category does not exist")
    void shouldThrowCategoryNotFoundException_WhenCategoryDoesNotExist() {
        // Given
        UUID categoryId = UUID.randomUUID();
        when(categoryRepositoryPort.findById(categoryId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CategoryNotFoundException.class, () -> useCase.execute(categoryId));
        verify(categoryRepositoryPort, never()).save(any());
        verify(activeServiceCheckPort, never()).hasActiveServices(any());
    }
}
