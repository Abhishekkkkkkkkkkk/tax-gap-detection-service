package com.avega.tgd.exception;

/**
 * Exception thrown when a requested resource (transaction, rule, exception) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
