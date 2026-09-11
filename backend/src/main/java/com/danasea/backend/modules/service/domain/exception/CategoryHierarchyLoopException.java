package com.danasea.backend.modules.service.domain.exception;

public class CategoryHierarchyLoopException extends RuntimeException {

    public CategoryHierarchyLoopException(String message) {
        super(message);
    }
}
