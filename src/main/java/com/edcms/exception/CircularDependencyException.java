package com.edcms.exception;

/**
 * Thrown when a circular dependency is detected in the component dependency graph.
 *
 * <p>Example of a circular dependency:
 * <pre>
 *   Motor v2.0 → requires → Controller v3.0
 *   Controller v3.0 → requires → Firmware v1.0
 *   Firmware v1.0 → requires → Motor v2.0   ← CYCLE DETECTED
 * </pre>
 *
 * <p>Detection is performed using a Depth-First Search (DFS) algorithm in
 * {@link com.edcms.service.DependencyService} before any dependency is persisted.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 400 BAD REQUEST.
 */
public class CircularDependencyException extends EdcmsException {

    public CircularDependencyException(String cycleDescription) {
        super("Circular dependency detected — this would create a dependency cycle: " + cycleDescription);
    }
}
