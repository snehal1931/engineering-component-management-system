package com.edcms.exception;

/**
 * Thrown when a compatibility check fails during assembly composition.
 *
 * <p>Example: Attempting to add Motor v2.0 to an assembly that already
 * contains Controller v2.0, when a rule states these versions are incompatible.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 409 CONFLICT.
 */
public class CompatibilityException extends EdcmsException {

    public CompatibilityException(String message) {
        super(message);
    }
}
