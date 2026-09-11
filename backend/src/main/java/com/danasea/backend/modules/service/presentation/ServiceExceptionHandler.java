package com.danasea.backend.modules.service.presentation;

import com.danasea.backend.modules.service.domain.exceptions.ImageNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.InvalidFileTypeException;
import com.danasea.backend.modules.service.domain.exceptions.MaxImagesExceededException;
import com.danasea.backend.modules.service.domain.exceptions.SafetyDocumentRequiredException;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.exceptions.UnauthorizedServiceAccessException;
import com.danasea.backend.shared.presentation.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ServiceExceptionHandler {

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleServiceNotFound(ServiceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SERVICE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedServiceAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedServiceAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleImageNotFound(ImageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("IMAGE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MaxImagesExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxImagesExceeded(MaxImagesExceededException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("MAX_IMAGES_EXCEEDED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileType(InvalidFileTypeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_FILE_TYPE", ex.getMessage()));
    }

    @ExceptionHandler(SafetyDocumentRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSafetyDocumentRequired(SafetyDocumentRequiredException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("SAFETY_DOCUMENT_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }
}
