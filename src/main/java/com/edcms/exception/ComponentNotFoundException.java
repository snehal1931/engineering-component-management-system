package com.edcms.exception;

/**
 * Thrown when a requested Component does not exist in the database.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 404 NOT FOUND.
 */
public class ComponentNotFoundException extends EdcmsException {

    public ComponentNotFoundException(Long id) {
        super("Component not found with id: " + id);
    }

    public ComponentNotFoundException(String code) {
        super("Component not found with code: " + code);
    }
}
