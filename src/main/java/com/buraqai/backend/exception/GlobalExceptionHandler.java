package com.buraqai.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles duplicate email → 409 Conflict
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 409);
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Handles validation errors (@NotBlank, @Email, etc.) → 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("message", "Validation failed");
        error.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Handles user not found → 404 Not Found
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Handles admin trying to deactivate themselves → 400 Bad Request
    @ExceptionHandler(CannotDeactivateSelfException.class)
    public ResponseEntity<Map<String, Object>> handleCannotDeactivateSelf(CannotDeactivateSelfException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyInactive(UserAlreadyInactiveException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Handles deactivated user attempting to log in → 403 Forbidden
    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountDeactivated(AccountDeactivatedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 403);
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}