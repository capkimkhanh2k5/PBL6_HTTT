package com.danasea.backend.modules.service.presentation.handler;

import com.danasea.backend.modules.service.domain.exception.CategoryHasActiveServicesException;
import com.danasea.backend.modules.service.domain.exception.CategoryHierarchyLoopException;
import com.danasea.backend.modules.service.domain.exception.CategoryNotFoundException;
import com.danasea.backend.modules.service.domain.exception.SlugAlreadyExistsException;
import com.danasea.backend.shared.presentation.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CategoryExceptionHandler {

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("CATEGORY_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(SlugAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleSlugAlreadyExists(SlugAlreadyExistsException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SLUG_ALREADY_EXISTS", exception.getMessage()));
    }

    @ExceptionHandler(CategoryHasActiveServicesException.class)
    public ResponseEntity<ErrorResponse> handleCategoryHasActiveServices(CategoryHasActiveServicesException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CATEGORY_HAS_ACTIVE_SERVICES", exception.getMessage()));
    }

    @ExceptionHandler(CategoryHierarchyLoopException.class)
    public ResponseEntity<ErrorResponse> handleCategoryHierarchyLoop(CategoryHierarchyLoopException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("CATEGORY_HIERARCHY_LOOP", exception.getMessage()));
    }
}
