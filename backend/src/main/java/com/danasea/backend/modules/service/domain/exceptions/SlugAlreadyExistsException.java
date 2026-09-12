package com.danasea.backend.modules.service.domain.exceptions;

public class SlugAlreadyExistsException extends RuntimeException {

    public SlugAlreadyExistsException(String message) {
        super(message);
    }

    public static SlugAlreadyExistsException ofSlug(String slug) {
        return new SlugAlreadyExistsException("Category with slug '" + slug + "' already exists");
    }
}
