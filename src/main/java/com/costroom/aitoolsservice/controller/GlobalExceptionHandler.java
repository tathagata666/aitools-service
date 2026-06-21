package com.costroom.aitoolsservice.controller;

import com.costroom.aitoolsservice.dto.ErrorResponse;
import com.costroom.aitoolsservice.exception.DuplicateToolException;
import com.costroom.aitoolsservice.exception.OrgResolutionException;
import com.costroom.aitoolsservice.exception.ToolNotFoundException;
import com.costroom.aitoolsservice.exception.UnsupportedProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ToolNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ToolNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateToolException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateToolException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedProviderException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedProvider(UnsupportedProviderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Validation Failed", message));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex) {
        log.warn("Security violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred"));
    }

    @ExceptionHandler(OrgResolutionException.class)
    public ResponseEntity<ErrorResponse> handleOrgResolution(OrgResolutionException ex) {
        log.warn("Org resolution failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Organization Not Resolved", ex.getMessage()));
    }
}
