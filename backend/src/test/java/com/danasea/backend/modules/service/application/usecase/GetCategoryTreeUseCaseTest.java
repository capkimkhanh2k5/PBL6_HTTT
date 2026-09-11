package com.danasea.backend.modules.service.application.usecase;

import java.util.List;
import java.util.UUID;

import com.danasea.backend.modules.service.application.port.output.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.presentation.dto.CategoryTreeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetCategoryTreeUseCaseTest {

    private CategoryRepositoryPort categoryRepositoryPort;
    private GetCategoryTreeUseCase useCase;

    @BeforeEach
    void setUp() {
        categoryRepositoryPort = mock(CategoryRepositoryPort.class);
        useCase = new GetCategoryTreeUseCase(categoryRepositoryPort);
    }

    @Test
    @DisplayName("Should return multi-level tree structure (root -> child -> grandchild)")
    void shouldReturnMultiLevelTreeStructure() {
        // Given
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID grandChildId = UUID.randomUUID();

        Category root = Category.builder()
                .id(rootId)
                .name("Root Category")
                .nameEn("Root Category En")
                .slug("root-category")
                .parentId(null)
                .isActive(true)
                .build();

        Category child = Category.builder()
                .id(childId)
                .name("Child Category")
                .nameEn("Child Category En")
                .slug("child-category")
                .parentId(rootId)
                .isActive(true)
                .build();

        Category grandChild = Category.builder()
                .id(grandChildId)
                .name("Grandchild Category")
                .nameEn("Grandchild Category En")
                .slug("grandchild-category")
                .parentId(childId)
                .isActive(true)
                .build();

        List<Category> allCategories = List.of(root, child, grandChild);
        when(categoryRepositoryPort.findAllActive()).thenReturn(allCategories);
        when(categoryRepositoryPort.findAll()).thenReturn(allCategories);

        // When
        List<CategoryTreeResponse> tree = useCase.execute(false);

        // Then
        assertNotNull(tree);
        assertEquals(1, tree.size());

        CategoryTreeResponse rootNode = tree.get(0);
        assertEquals(rootId, rootNode.id());
        assertEquals("Root Category", rootNode.name());
        assertEquals(1, rootNode.children().size());

        CategoryTreeResponse childNode = rootNode.children().get(0);
        assertEquals(childId, childNode.id());
        assertEquals("Child Category", childNode.name());
        assertEquals(1, childNode.children().size());

        CategoryTreeResponse grandChildNode = childNode.children().get(0);
        assertEquals(grandChildId, grandChildNode.id());
        assertEquals("Grandchild Category", grandChildNode.name());
        assertEquals(0, grandChildNode.children().size());
    }

    @Test
    @DisplayName("Should filter inactive categories for public but include them for admin")
    void shouldFilterInactiveCategoriesForPublic_ButIncludeThemForAdmin() {
        // Given
        UUID rootId = UUID.randomUUID();
        UUID activeChildId = UUID.randomUUID();
        UUID inactiveChildId = UUID.randomUUID();

        Category root = Category.builder()
                .id(rootId)
                .name("Root")
                .slug("root")
                .parentId(null)
                .isActive(true)
                .build();

        Category activeChild = Category.builder()
                .id(activeChildId)
                .name("Active Child")
                .slug("active-child")
                .parentId(rootId)
                .isActive(true)
                .build();

        Category inactiveChild = Category.builder()
                .id(inactiveChildId)
                .name("Inactive Child")
                .slug("inactive-child")
                .parentId(rootId)
                .isActive(false)
                .build();

        when(categoryRepositoryPort.findAllActive()).thenReturn(List.of(root, activeChild));
        when(categoryRepositoryPort.findAll()).thenReturn(List.of(root, activeChild, inactiveChild));

        // When
        List<CategoryTreeResponse> publicTree = useCase.execute(false);
        List<CategoryTreeResponse> adminTree = useCase.execute(true);

        // Then for public: inactive child should NOT be present
        assertNotNull(publicTree);
        assertEquals(1, publicTree.size());
        CategoryTreeResponse publicRoot = publicTree.get(0);
        assertEquals(1, publicRoot.children().size());
        assertEquals(activeChildId, publicRoot.children().get(0).id());

        // Then for admin: both active and inactive children should be present
        assertNotNull(adminTree);
        assertEquals(1, adminTree.size());
        CategoryTreeResponse adminRoot = adminTree.get(0);
        assertEquals(2, adminRoot.children().size());
        List<UUID> childIds = adminRoot.children().stream().map(CategoryTreeResponse::id).toList();
        assertTrue(childIds.contains(activeChildId));
        assertTrue(childIds.contains(inactiveChildId));
    }
}
