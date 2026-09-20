package com.danasea.backend.modules.weather.presentation.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.danasea.backend.modules.weather.domain.exceptions.CategorySafetyRuleNotFoundException;
import com.danasea.backend.modules.weather.domain.exceptions.InvalidSafetyRuleThresholdException;
import com.danasea.backend.shared.presentation.ErrorResponse;

@RestControllerAdvice
public class WeatherExceptionHandler {

    @ExceptionHandler(CategorySafetyRuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRuleNotFound(CategorySafetyRuleNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("CATEGORY_SAFETY_RULE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidSafetyRuleThresholdException.class)
    public ResponseEntity<ErrorResponse> handleInvalidThreshold(InvalidSafetyRuleThresholdException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_SAFETY_RULE_THRESHOLD", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid input data");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_INPUT", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }
}
