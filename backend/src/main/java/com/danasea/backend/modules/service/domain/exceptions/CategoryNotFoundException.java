package com.danasea.backend.modules.service.domain.exceptions;

import java.util.UUID;

public class CategoryNotFoundException extends ServiceDomainException {

    public CategoryNotFoundException(UUID categoryId) {
        super("Category not found with ID: " + categoryId);
    }

    public CategoryNotFoundException(String message) {
        super(message);
    }
}
