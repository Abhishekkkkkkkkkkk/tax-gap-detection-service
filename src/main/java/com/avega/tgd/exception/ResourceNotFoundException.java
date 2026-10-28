package com.avega.tgd.exception;

/**
 * Business runtime exception thrown when a requested domain entity cannot be located.
 *
 * <p>Mapped by {@link GlobalExceptionHandler#handleResourceNotFound(ResourceNotFoundException)}
 * to an HTTP {@code 404 NOT FOUND} response envelope.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new exception with the specified descriptive error message.
     *
     * @param message description of the missing entity and lookup key
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
