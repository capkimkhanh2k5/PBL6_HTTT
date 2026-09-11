package com.danasea.backend.modules.service.domain.exception;

import java.util.UUID;

public class CategoryHasActiveServicesException extends RuntimeException {

    public CategoryHasActiveServicesException(String message) {
        super(message);
    }

    public CategoryHasActiveServicesException(UUID categoryId) {
        super("Cannot deactivate category with active services. Category ID: " + categoryId);
    }
}
