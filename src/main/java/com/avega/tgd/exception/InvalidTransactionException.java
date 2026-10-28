package com.avega.tgd.exception;

/**
 * Business runtime exception thrown when a financial transaction payload fails
 * business validation rules (e.g. non-positive amount, missing required fields).
 *
 * <p>Mapped by {@link GlobalExceptionHandler#handleInvalidTransaction(InvalidTransactionException)}
 * to an HTTP {@code 400 BAD REQUEST} response envelope.
 */
public class InvalidTransactionException extends RuntimeException {

    /**
     * Constructs a new exception with the specified validation failure reason.
     *
     * @param message description of the validation failure
     */
    public InvalidTransactionException(String message) {
        super(message);
    }
}
