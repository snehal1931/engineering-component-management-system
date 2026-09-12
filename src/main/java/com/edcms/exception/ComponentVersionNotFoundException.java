package com.edcms.exception;

/**
 * Thrown when a requested ComponentVersion does not exist in the database.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 404 NOT FOUND.
 */
public class ComponentVersionNotFoundException extends EdcmsException {

    public ComponentVersionNotFoundException(Long id) {
        super("ComponentVersion not found with id: " + id);
    }

    public ComponentVersionNotFoundException(Long componentId, String versionNumber) {
        super("Version '" + versionNumber + "' not found for component id: " + componentId);
    }
}
