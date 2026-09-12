package com.edcms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base exception for all EDCMS domain exceptions.
 *
 * <p>Having a common base class for domain exceptions serves two purposes:
 * <ol>
 *   <li>The GlobalExceptionHandler can catch all domain exceptions in a
 *       single handler if needed.</li>
 *   <li>It provides a consistent place to attach domain-specific metadata
 *       (like an error code) in the future without changing all subclasses.</li>
 * </ol>
 *
 * <p>OOP: This is an abstract base class demonstrating Abstraction and
 * Inheritance in the exception hierarchy. It extends RuntimeException
 * because EDCMS domain errors are unchecked — callers should not be
 * forced to declare or catch them in business logic.
 */
public abstract class EdcmsException extends RuntimeException {

    protected EdcmsException(String message) {
        super(message);
    }

    protected EdcmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
