package com.avega.tgd.service;

import com.avega.tgd.domain.entity.ExceptionEntity;
import com.avega.tgd.domain.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing and filtering tax compliance exceptions.
 */
public interface ExceptionService {

    /**
     * Records a new compliance or calculation exception.
     */
    ExceptionEntity createException(String transactionId, String customerId, String ruleName, Severity severity, String message);

    /**
     * Filters exception records by customerId, severity, and ruleName.
     */
    Page<ExceptionEntity> getExceptions(String customerId, Severity severity, String ruleName, Pageable pageable);

    /**
     * Retrieves an exception by ID.
     */
    ExceptionEntity getExceptionById(Long id);

    /**
     * Retrieves all exceptions for a given transactionId.
     */
    List<ExceptionEntity> getExceptionsByTransactionId(String transactionId);
}
