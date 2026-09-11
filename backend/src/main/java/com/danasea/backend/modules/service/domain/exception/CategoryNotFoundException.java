package com.danasea.backend.modules.service.domain.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String message) {
        super(message);
    }

    public CategoryNotFoundException(UUID id) {
        super("Category not found with id: " + id);
    }
}
