package com.danasea.backend.modules.service.domain.exceptions;

public class CategoryHierarchyLoopException extends RuntimeException {

    public CategoryHierarchyLoopException(String message) {
        super(message);
    }
}
