package com.avega.tgd.service;

import com.avega.tgd.domain.entity.ExceptionEntity;
import com.avega.tgd.domain.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service governing compliance exception management and dynamic query filtering.
 */
public interface ExceptionService {

    ExceptionEntity createException(String transactionId, String customerId, String ruleName, Severity severity, String message);

    Page<ExceptionEntity> getExceptions(String customerId, Severity severity, String ruleName, Pageable pageable);

    ExceptionEntity getExceptionById(Long id);

    List<ExceptionEntity> getExceptionsByTransactionId(String transactionId);
}
