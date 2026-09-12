package com.edcms.exception;

/**
 * Thrown when a requested Assembly does not exist in the database.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 404 NOT FOUND.
 */
public class AssemblyNotFoundException extends EdcmsException {

    public AssemblyNotFoundException(Long id) {
        super("Assembly not found with id: " + id);
    }
}
