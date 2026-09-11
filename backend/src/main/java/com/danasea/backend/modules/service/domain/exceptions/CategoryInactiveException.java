package com.danasea.backend.modules.service.domain.exceptions;

import java.util.UUID;

public class CategoryInactiveException extends ServiceDomainException {

    public CategoryInactiveException(UUID categoryId) {
        super("Category is inactive with ID: " + categoryId);
    }

    public CategoryInactiveException(String message) {
        super(message);
    }
}
