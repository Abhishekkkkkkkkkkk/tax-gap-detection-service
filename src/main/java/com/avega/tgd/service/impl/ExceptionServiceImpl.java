package com.avega.tgd.service.impl;

import com.avega.tgd.domain.entity.ExceptionEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.exception.ResourceNotFoundException;
import com.avega.tgd.repository.ExceptionRepository;
import com.avega.tgd.service.ExceptionService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link ExceptionService} providing exception persistence
 * and dynamic query filtering through Spring Data JPA {@link Specification}.
 */
@Service
public class ExceptionServiceImpl implements ExceptionService {

    private static final Logger log = LoggerFactory.getLogger(ExceptionServiceImpl.class);

    private final ExceptionRepository exceptionRepository;

    public ExceptionServiceImpl(ExceptionRepository exceptionRepository) {
        this.exceptionRepository = exceptionRepository;
    }

    /**
     * Persists a new exception record with timestamp.
     *
     * @param transactionId Target transaction identifier
     * @param customerId    Associated customer
     * @param ruleName      Rule or validation name
     * @param severity      Severity level (defaults to MEDIUM if null)
     * @param message       Diagnostic message
     * @return Saved entity
     */
    @Override
    @Transactional
    public ExceptionEntity createException(String transactionId, String customerId, String ruleName, Severity severity, String message) {
        ExceptionEntity exception = new ExceptionEntity(
                transactionId,
                customerId,
                ruleName,
                severity != null ? severity : Severity.MEDIUM,
                message,
                LocalDateTime.now()
        );

        ExceptionEntity saved = exceptionRepository.save(exception);
        log.warn("EXCEPTION CREATED [{}] Severity: {}, Tx: '{}', Customer: '{}', Message: {}",
                ruleName, severity, transactionId, customerId, message);
        return saved;
    }

    /**
     * Builds a dynamic JPA Criteria Specification combining optional filters via SQL {@code AND}.
     * If a filter parameter is null or blank, it is omitted from the WHERE clause entirely.
     *
     * @param customerId Optional customer filter
     * @param severity   Optional severity filter
     * @param ruleName   Optional rule name filter
     * @param pageable   Pagination parameters
     * @return Page of matching exceptions
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ExceptionEntity> getExceptions(String customerId, Severity severity, String ruleName, Pageable pageable) {
        Specification<ExceptionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Dynamic filter: Customer ID
            if (customerId != null && !customerId.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("customerId"), customerId.trim()));
            }

            // Dynamic filter: Severity
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }

            // Dynamic filter: Rule Name
            if (ruleName != null && !ruleName.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("ruleName"), ruleName.trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return exceptionRepository.findAll(spec, pageable);
    }

    /**
     * Retrieves an exception by ID or throws ResourceNotFoundException.
     */
    @Override
    @Transactional(readOnly = true)
    public ExceptionEntity getExceptionById(Long id) {
        return exceptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exception record not found with id: " + id));
    }

    /**
     * Fetches all exceptions generated for a specific transaction ID.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExceptionEntity> getExceptionsByTransactionId(String transactionId) {
        return exceptionRepository.findByTransactionId(transactionId);
    }
}
