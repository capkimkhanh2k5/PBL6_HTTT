package com.danasea.backend.modules.service.application.usecase;

import java.util.Optional;
import java.util.UUID;

import com.danasea.backend.modules.service.application.port.output.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.exception.CategoryHierarchyLoopException;
import com.danasea.backend.modules.service.domain.exception.CategoryNotFoundException;
import com.danasea.backend.modules.service.domain.exception.SlugAlreadyExistsException;
import com.danasea.backend.modules.service.domain.models.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateCategoryUseCaseTest {

    private CategoryRepositoryPort categoryRepositoryPort;
    private CreateCategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        categoryRepositoryPort = mock(CategoryRepositoryPort.class);
        useCase = new CreateCategoryUseCase(categoryRepositoryPort);
    }

    @Test
    @DisplayName("Should create root category successfully when parentId is null")
    void shouldCreateRootCategorySuccessfully_WhenParentIdIsNull() {
        // Given
        String name = "Electronics";
        String nameEn = "Electronics En";
        String slug = "electronics";
        UUID parentId = null;
        String iconUrl = "https://example.com/electronics.png";
        CreateCategoryCommand command = new CreateCategoryCommand(name, nameEn, slug, parentId, iconUrl);

        when(categoryRepositoryPort.existsBySlug(slug)).thenReturn(false);
        when(categoryRepositoryPort.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Category result = useCase.execute(command);

        // Then
        assertNotNull(result);
        assertEquals("Electronics", result.getName());
        assertEquals("Electronics En", result.getNameEn());
        assertEquals("electronics", result.getSlug());
        assertNull(result.getParentId());
        assertEquals(iconUrl, result.getIconUrl());
        assertTrue(result.getIsActive());
        verify(categoryRepositoryPort).save(any(Category.class));
    }

    @Test
    @DisplayName("Should create child category successfully when parentId is valid")
    void shouldCreateChildCategorySuccessfully_WhenParentIdIsValid() {
        // Given
        UUID parentId = UUID.randomUUID();
        Category parent = Category.builder()
                .id(parentId)
                .name("Electronics")
                .slug("electronics")
                .isActive(true)
                .build();
        String slug = "laptops";
        CreateCategoryCommand command = new CreateCategoryCommand("Laptops", "Laptops En", slug, parentId, "laptop.png");

        when(categoryRepositoryPort.existsBySlug(slug)).thenReturn(false);
        when(categoryRepositoryPort.findById(parentId)).thenReturn(Optional.of(parent));
        when(categoryRepositoryPort.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Category result = useCase.execute(command);

        // Then
        assertNotNull(result);
        assertEquals("Laptops", result.getName());
        assertEquals(parentId, result.getParentId());
        assertTrue(result.getIsActive());
        verify(categoryRepositoryPort).findById(parentId);
        verify(categoryRepositoryPort).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw CategoryNotFoundException when parentId does not exist")
    void shouldThrowCategoryNotFoundException_WhenParentIdDoesNotExist() {
        // Given
        UUID nonExistentParentId = UUID.randomUUID();
        String slug = "phones";
        CreateCategoryCommand command = new CreateCategoryCommand("Phones", "Phones En", slug, nonExistentParentId, null);

        when(categoryRepositoryPort.existsBySlug(slug)).thenReturn(false);
        when(categoryRepositoryPort.findById(nonExistentParentId)).thenReturn(Optional.empty());

        // When & Then
        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> useCase.execute(command)
        );
        assertTrue(exception.getMessage().contains(nonExistentParentId.toString()));
        verify(categoryRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw SlugAlreadyExistsException when slug already exists")
    void shouldThrowSlugAlreadyExistsException_WhenSlugAlreadyExists() {
        // Given
        String existingSlug = "existing-slug";
        CreateCategoryCommand command = new CreateCategoryCommand("Existing", "Existing En", existingSlug, null, null);

        when(categoryRepositoryPort.existsBySlug(existingSlug)).thenReturn(true);

        // When & Then
        SlugAlreadyExistsException exception = assertThrows(
                SlugAlreadyExistsException.class,
                () -> useCase.execute(command)
        );
        assertTrue(exception.getMessage().contains(existingSlug));
        verify(categoryRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CategoryHierarchyLoopException when setting child as parent (A -> B -> A)")
    void shouldThrowCategoryHierarchyLoopException_WhenSettingChildAsParent() {
        // Given
        UUID catAId = UUID.randomUUID();
        UUID catBId = UUID.randomUUID();

        Category catA = Category.builder()
                .id(catAId)
                .name("Category A")
                .slug("cat-a")
                .isActive(true)
                .build();

        Category catB = Category.builder()
                .id(catBId)
                .name("Category B")
                .slug("cat-b")
                .parentId(catAId)
                .isActive(true)
                .build();

        when(categoryRepositoryPort.existsBySlug("cat-a")).thenReturn(false);
        when(categoryRepositoryPort.findById(catBId)).thenReturn(Optional.of(catB));
        when(categoryRepositoryPort.findById(catAId)).thenReturn(Optional.of(catA));

        CreateCategoryCommand command = new CreateCategoryCommand(catAId, "Category A", "Cat A En", "cat-a", catBId, null);

        // When & Then
        CategoryHierarchyLoopException exception = assertThrows(
                CategoryHierarchyLoopException.class,
                () -> useCase.execute(command)
        );
        assertNotNull(exception.getMessage());
        verify(categoryRepositoryPort, never()).save(any());
    }
}
