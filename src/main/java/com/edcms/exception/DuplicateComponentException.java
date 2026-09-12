package com.edcms.exception;

/**
 * Thrown when a component with the same component_code already exists.
 *
 * <p>Component codes must be globally unique across the system (e.g., "ELEC-MOTOR-001").
 * Caught by {@link GlobalExceptionHandler} and mapped to HTTP 409 CONFLICT.
 */
public class DuplicateComponentException extends EdcmsException {

    public DuplicateComponentException(String componentCode) {
        super("A component with code '" + componentCode + "' already exists.");
    }
}
