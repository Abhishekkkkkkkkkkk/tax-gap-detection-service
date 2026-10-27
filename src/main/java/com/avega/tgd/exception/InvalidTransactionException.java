package com.avega.tgd.exception;

/**
 * Exception thrown when a transaction payload fails structural or validation requirements.
 */
public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
