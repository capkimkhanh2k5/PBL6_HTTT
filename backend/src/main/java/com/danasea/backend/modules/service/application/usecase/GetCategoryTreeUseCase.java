package com.danasea.backend.modules.service.application.usecase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.danasea.backend.modules.service.application.port.output.CategoryRepositoryPort;
import com.danasea.backend.modules.service.domain.models.Category;
import com.danasea.backend.modules.service.presentation.dto.CategoryTreeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCategoryTreeUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;

    public List<CategoryTreeResponse> execute() {
        return execute(false);
    }

    public List<CategoryTreeResponse> execute(boolean includeInactive) {
        List<Category> categories;
        if (includeInactive) {
            categories = categoryRepositoryPort.findAll();
        } else {
            categories = categoryRepositoryPort.findAllActive();
            if (categories == null || categories.isEmpty()) {
                List<Category> all = categoryRepositoryPort.findAll();
                if (all != null && !all.isEmpty()) {
                    categories = all.stream()
                            .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                            .toList();
                } else {
                    categories = List.of();
                }
            } else {
                categories = categories.stream()
                        .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                        .toList();
            }
        }

        return buildTree(categories);
    }

    private List<CategoryTreeResponse> buildTree(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return new ArrayList<>();
        }

        Map<UUID, CategoryTreeResponse> nodeMap = new LinkedHashMap<>();
        Map<UUID, UUID> parentMap = new HashMap<>();

        for (Category cat : categories) {
            if (cat.getId() != null) {
                nodeMap.put(cat.getId(), new CategoryTreeResponse(
                        cat.getId(),
                        cat.getName(),
                        cat.getNameEn(),
                        cat.getSlug(),
                        cat.getParentId(),
                        cat.getIconUrl(),
                        cat.getIsActive(),
                        new ArrayList<>()
                ));
                parentMap.put(cat.getId(), cat.getParentId());
            }
        }

        List<CategoryTreeResponse> rootNodes = new ArrayList<>();

        for (Category cat : categories) {
            if (cat.getId() == null) {
                continue;
            }

            CategoryTreeResponse currentNode = nodeMap.get(cat.getId());
            UUID parentId = cat.getParentId();

            if (parentId != null && nodeMap.containsKey(parentId)) {
                if (!isAncestor(cat.getId(), parentId, parentMap)) {
                    CategoryTreeResponse parentNode = nodeMap.get(parentId);
                    parentNode.children().add(currentNode);
                    continue;
                }
            }
            rootNodes.add(currentNode);
        }

        return rootNodes;
    }

    private boolean isAncestor(UUID ancestorId, UUID childId, Map<UUID, UUID> parentMap) {
        UUID current = childId;
        Set<UUID> seen = new HashSet<>();

        while (current != null) {
            if (current.equals(ancestorId)) {
                return true;
            }
            if (!seen.add(current)) {
                break;
            }
            current = parentMap.get(current);
        }

        return false;
    }
}
