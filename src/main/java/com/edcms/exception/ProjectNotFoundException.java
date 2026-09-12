package com.edcms.exception;

/**
 * Thrown when a requested Project does not exist in the database.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 404 NOT FOUND.
 */
public class ProjectNotFoundException extends EdcmsException {

    public ProjectNotFoundException(Long id) {
        super("Project not found with id: " + id);
    }
}
