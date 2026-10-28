package com.avega.tgd.service;

import com.avega.tgd.domain.entity.ExceptionEntity;
import com.avega.tgd.domain.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface governing compliance and validation exception management.
 *
 * <p>Exception Lifecycle:
 * Exceptions are immutable records generated when:
 * <ul>
 *   <li>Syntax or structural validation fails (e.g. {@code SCHEMA_VALIDATION_FAILURE}).</li>
 *   <li>Tax underpayment or missing tax is identified (e.g. {@code TAX_GAP_UNDERPAID}, {@code TAX_REPORTED_MISSING}).</li>
 *   <li>A business rule triggers (e.g. {@code HIGH_VALUE_TRANSACTION}, {@code REFUND_VALIDATION}, {@code GST_SLAB_VIOLATION}).</li>
 * </ul>
 *
 * <p>Query Support:
 * Supports dynamic filtering using JPA Specifications allowing auditors to filter by any combination
 * of customer, severity, and rule name with pagination and sorting.
 */
public interface ExceptionService {

    /**
     * Persists an audit exception record.
     *
     * @param transactionId Unique business transaction ID associated with the exception
     * @param customerId    Identifier of the customer who submitted the transaction
     * @param ruleName      Name of the rule or validation check that triggered the exception
     * @param severity      Assigned severity level (HIGH, MEDIUM, LOW)
     * @param message       Human-readable explanation of why the check failed
     * @return The persisted {@link ExceptionEntity}
     */
    ExceptionEntity createException(String transactionId, String customerId, String ruleName, Severity severity, String message);

    /**
     * Retrieves a page of exception records matching optional filtering criteria.
     *
     * @param customerId Optional filter for customer identifier (null/blank to ignore)
     * @param severity   Optional filter for severity level (null to ignore)
     * @param ruleName   Optional filter for rule name (null/blank to ignore)
     * @param pageable   Pagination and sorting parameters
     * @return Page of matching {@link ExceptionEntity} records
     */
    Page<ExceptionEntity> getExceptions(String customerId, Severity severity, String ruleName, Pageable pageable);

    /**
     * Retrieves a single exception record by its primary key ID.
     *
     * @param id The database primary key of the exception
     * @return The matching {@link ExceptionEntity}
     * @throws com.avega.tgd.exception.ResourceNotFoundException if no record exists with the given ID
     */
    ExceptionEntity getExceptionById(Long id);

    /**
     * Retrieves all exception records associated with a specific transaction ID.
     * Useful for viewing all rule failures triggered by a single transaction.
     *
     * @param transactionId The business transaction ID
     * @return List of matching exceptions
     */
    List<ExceptionEntity> getExceptionsByTransactionId(String transactionId);
}
