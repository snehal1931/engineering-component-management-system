package com.edcms.exception;

/**
 * Thrown when a username or email already exists in the system.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 409 CONFLICT.
 */
public class DuplicateUserException extends EdcmsException {

    public DuplicateUserException(String field, String value) {
        super("A user with " + field + " '" + value + "' already exists.");
    }
}
