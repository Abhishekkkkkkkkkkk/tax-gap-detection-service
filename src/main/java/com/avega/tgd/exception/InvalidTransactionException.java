package com.avega.tgd.exception;

/**
 * Thrown when a transaction fails validation checks.
 */
public class InvalidTransactionException extends RuntimeException {

    public InvalidTransactionException(String message) {
        super(message);
    }
}
