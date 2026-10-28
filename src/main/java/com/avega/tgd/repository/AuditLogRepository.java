package com.avega.tgd.repository;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for persisting and querying immutable audit records ({@link AuditLogEntity}).
 *
 * <p>Supports Requirement 5 ("Audit Logging"):
 * <ul>
 *   <li>Stores chronological records of ingestion, rule evaluation, and tax computation.</li>
 *   <li>Leverages database indexes on {@code transaction_id}, {@code event_type}, and {@code timestamp}
 *       to support rapid forensic lookups.</li>
 *   <li>Provides paged queries to handle high-throughput event logging safely.</li>
 * </ul>
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * Retrieves all audit trail entries for a given transaction identifier.
     *
     * @param transactionId the unique transaction identifier
     * @return list of audit log entities ordered by insertion
     */
    List<AuditLogEntity> findByTransactionId(String transactionId);

    /**
     * Retrieves a paginated list of audit records for a given transaction identifier.
     *
     * @param transactionId the unique transaction identifier
     * @param pageable      pagination and sorting metadata
     * @return paged audit logs
     */
    Page<AuditLogEntity> findByTransactionId(String transactionId, Pageable pageable);

    /**
     * Retrieves a paginated list of audit records filtered by event type.
     *
     * @param eventType the audit event type (INGESTION, RULE_EXECUTION, TAX_COMPUTATION)
     * @param pageable  pagination and sorting metadata
     * @return paged audit logs
     */
    Page<AuditLogEntity> findByEventType(AuditEventType eventType, Pageable pageable);
}
