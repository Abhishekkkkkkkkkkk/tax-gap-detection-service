package com.avega.tgd.exception;

/**
 * Thrown when a requested domain entity is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
