package com.danasea.backend.modules.service.presentation.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasea.backend.modules.service.domain.exceptions.CategoryInactiveException;
import com.danasea.backend.modules.service.domain.exceptions.CategoryNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.InvalidServiceStateException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceImagesRequiredException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.modules.service.domain.exceptions.VendorNotApprovedException;
import com.danasea.backend.modules.service.domain.exceptions.WeatherRequirementsMissingException;
import com.danasea.backend.shared.presentation.ErrorResponse;

@RestControllerAdvice
public class ServiceExceptionHandler {

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleServiceNotFound(ServiceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SERVICE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("CATEGORY_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CategoryInactiveException.class)
    public ResponseEntity<ErrorResponse> handleCategoryInactive(CategoryInactiveException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("CATEGORY_INACTIVE", ex.getMessage()));
    }

    @ExceptionHandler(VendorNotApprovedException.class)
    public ResponseEntity<ErrorResponse> handleVendorNotApproved(VendorNotApprovedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("VENDOR_NOT_APPROVED", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedServiceAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedServiceAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("UNAUTHORIZED_SERVICE_ACCESS", ex.getMessage()));
    }

    @ExceptionHandler(InvalidServiceStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidServiceStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_SERVICE_STATE", ex.getMessage()));
    }

    @ExceptionHandler(ServiceImagesRequiredException.class)
    public ResponseEntity<ErrorResponse> handleImagesRequired(ServiceImagesRequiredException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("SERVICE_IMAGES_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(WeatherRequirementsMissingException.class)
    public ResponseEntity<ErrorResponse> handleWeatherMissing(WeatherRequirementsMissingException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("WEATHER_REQUIREMENTS_MISSING", ex.getMessage()));
    }

    @ExceptionHandler(com.danasea.backend.modules.service.domain.exceptions.ImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleImageNotFound(com.danasea.backend.modules.service.domain.exceptions.ImageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("IMAGE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(com.danasea.backend.modules.service.domain.exceptions.MaxImagesExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxImagesExceeded(com.danasea.backend.modules.service.domain.exceptions.MaxImagesExceededException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("MAX_IMAGES_EXCEEDED", ex.getMessage()));
    }

    @ExceptionHandler(com.danasea.backend.modules.service.domain.exceptions.InvalidFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileType(com.danasea.backend.modules.service.domain.exceptions.InvalidFileTypeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_FILE_TYPE", ex.getMessage()));
    }

    @ExceptionHandler(com.danasea.backend.modules.service.domain.exceptions.SafetyDocumentRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSafetyDocumentRequired(com.danasea.backend.modules.service.domain.exceptions.SafetyDocumentRequiredException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("SAFETY_DOCUMENT_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }
}
