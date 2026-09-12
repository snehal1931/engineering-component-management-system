package com.edcms.exception;

/**
 * Thrown when attempting to add a DRAFT or DEPRECATED component version
 * to a production assembly.
 *
 * <p>Only APPROVED versions may be referenced in assemblies.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 400 BAD REQUEST.
 */
public class InvalidVersionStatusException extends EdcmsException {

    public InvalidVersionStatusException(String versionNumber, String currentStatus) {
        super("Cannot add version '" + versionNumber + "' to assembly — status is " + currentStatus
                + ". Only APPROVED versions may be added to assemblies.");
    }
}
