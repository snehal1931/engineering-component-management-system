package com.edcms.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handling for all EDCMS REST controllers.
 *
 * <p>This class uses Spring's {@code @RestControllerAdvice} to intercept
 * exceptions thrown anywhere in the controller or service layers and
 * convert them into structured JSON error responses.
 *
 * <p>Design Rationale (SOLID — Single Responsibility Principle):
 * Without this, every controller would need its own try-catch logic,
 * leading to massive code duplication. A single handler class centralizes
 * the responsibility of translating exceptions to HTTP responses.
 *
 * <p>Design Rationale (Open/Closed Principle):
 * Adding a new custom exception requires only adding one new @ExceptionHandler
 * method here — existing handlers don't change.
 *
 * <p>All error responses follow a consistent JSON structure:
 * <pre>
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Component not found with id: 42",
 *   "path": "/api/components/42"
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 404 NOT FOUND ─────────────────────────────────────────────────────────

    @ExceptionHandler({
            ComponentNotFoundException.class,
            ProjectNotFoundException.class,
            AssemblyNotFoundException.class,
            UserNotFoundException.class,
            ComponentVersionNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(EdcmsException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // ─── 409 CONFLICT ──────────────────────────────────────────────────────────

    @ExceptionHandler({
            DuplicateComponentException.class,
            DuplicateUserException.class,
            CompatibilityException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(EdcmsException ex, WebRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // ─── 400 BAD REQUEST ───────────────────────────────────────────────────────

    @ExceptionHandler({
            InvalidVersionTransitionException.class,
            InvalidVersionStatusException.class,
            CircularDependencyException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(EdcmsException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Handles @Valid failures on @RequestBody objects.
     * Returns a map of field names → validation error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            fieldErrors.put(fieldName, error.getDefaultMessage());
        });

        log.warn("Validation failed: {}", fieldErrors);

        ValidationErrorResponse response = new ValidationErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more request fields are invalid.",
                getPath(request),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles @Validated failures on path/query parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.warn("Constraint violation: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Catch-all for any unhandled exceptions.
     * Logs the full stack trace but returns a safe generic message to the client.
     */
    /**
     * Handles business rule violations thrown as {@code IllegalArgumentException}.
     *
     * <p>Several service methods throw {@code IllegalArgumentException} for:
     * <ul>
     *   <li>Self-dependency (version depending on itself)</li>
     *   <li>Duplicate dependency between the same pair</li>
     *   <li>Duplicate version number on the same component</li>
     *   <li>Removing a version that isn't in an assembly</li>
     * </ul>
     *
     * <p>These are client errors (invalid input), so they map to HTTP 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Bad request (illegal argument): {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact the system administrator.",
                request
        );
    }

    // ─── Helper Methods ────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, WebRequest request) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                getPath(request)
        );
        return ResponseEntity.status(status).body(body);
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    // ─── Inner Response Records ────────────────────────────────────────────────

    /**
     * Standard error response body.
     * Using a Java record (Java 16+) for an immutable, concise response object.
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}

    /**
     * Extended error response with field-level validation errors.
     */
    public record ValidationErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {}
}
