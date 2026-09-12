package com.edcms.exception;

/**
 * Thrown when a requested User does not exist in the database.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 404 NOT FOUND.
 */
public class UserNotFoundException extends EdcmsException {

    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }

    public UserNotFoundException(String username) {
        super("User not found with username: " + username);
    }
}
