package com.danasea.backend.modules.vendor.presentation.advices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasea.backend.modules.vendor.domain.exceptions.InvalidDocTypeException;
import com.danasea.backend.modules.vendor.domain.exceptions.UserLockedException;
import com.danasea.backend.modules.vendor.domain.exceptions.VendorAlreadyExistsException;
import com.danasea.backend.modules.vendor.domain.exceptions.VendorNotFoundException;
import com.danasea.backend.shared.presentation.ErrorResponse;
import com.danasea.backend.shared.presentation.LocalizedExceptionHandlerSupport;

@RestControllerAdvice(basePackages = "com.danasea.backend.modules.vendor")
public class VendorExceptionHandler extends LocalizedExceptionHandlerSupport {

    @ExceptionHandler(VendorAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleVendorAlreadyExists(VendorAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error("VENDOR_ALREADY_EXISTS"));
    }

    @ExceptionHandler(VendorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVendorNotFound(VendorNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error("VENDOR_NOT_FOUND"));
    }

    @ExceptionHandler(UserLockedException.class)
    public ResponseEntity<ErrorResponse> handleUserLocked(UserLockedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error("USER_LOCKED"));
    }

    @ExceptionHandler(InvalidDocTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocType(InvalidDocTypeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error("INVALID_DOC_TYPE"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(validationError(ex));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error("INVALID_INPUT"));
    }
}
