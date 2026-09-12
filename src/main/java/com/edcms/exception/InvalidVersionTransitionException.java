package com.edcms.exception;

/**
 * Thrown when an invalid version status transition is attempted.
 *
 * <p>Valid transitions:
 * <ul>
 *   <li>DRAFT → APPROVED ✅</li>
 *   <li>APPROVED → DEPRECATED ✅</li>
 *   <li>DRAFT → DEPRECATED ✅</li>
 *   <li>DEPRECATED → APPROVED ❌</li>
 *   <li>DEPRECATED → DRAFT ❌</li>
 *   <li>APPROVED → DRAFT ❌</li>
 * </ul>
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to HTTP 400 BAD REQUEST.
 */
public class InvalidVersionTransitionException extends EdcmsException {

    public InvalidVersionTransitionException(String fromStatus, String toStatus) {
        super("Invalid version status transition: " + fromStatus + " → " + toStatus
                + ". This transition is not permitted by the version lifecycle rules.");
    }
}
