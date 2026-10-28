package com.avega.tgd.service;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for creating and querying immutable audit trail records.
 *
 * <p>Requirement 5 (Audit Logging) mandates maintaining a complete audit trail for:
 * <ul>
 *   <li>{@code INGESTION}: When raw financial transaction records are ingested and validated.</li>
 *   <li>{@code RULE_EXECUTION}: When dynamic compliance rules evaluate transactions and flag violations.</li>
 *   <li>{@code TAX_COMPUTATION}: When expected tax and tax gap formulas are evaluated.</li>
 * </ul>
 *
 * <p>Design Decision:
 * Audit logging is decoupled from the primary business transaction via independent physical
 * transactions (REQUIRES_NEW) to guarantee non-repudiation and forensic visibility even when
 * processing encounters failures or rollbacks.
 */
public interface AuditLogService {

    /**
     * Records an immutable audit log entry capturing an event, the affected transaction,
     * and a structured JSON payload of the event details.
     *
     * @param eventType      the category of audit event (INGESTION, RULE_EXECUTION, TAX_COMPUTATION)
     * @param transactionId  the unique identifier of the transaction, or "N/A" if unassigned
     * @param detailPayload  an object or JSON string containing before/after state or rule diagnostics
     * @return the persisted {@link AuditLogEntity} record
     */
    AuditLogEntity logEvent(AuditEventType eventType, String transactionId, Object detailPayload);

    /**
     * Retrieves all chronological audit log records associated with a specific transaction ID.
     *
     * @param transactionId the unique transaction identifier to inspect
     * @return list of audit log entities ordered by persistence timestamp
     */
    List<AuditLogEntity> getAuditLogsForTransaction(String transactionId);

    /**
     * Retrieves a paginated list of audit records with optional filtering by transaction ID or event type.
     *
     * @param transactionId optional transaction identifier filter; if provided, filters by transaction
     * @param eventType     optional event type filter (INGESTION, RULE_EXECUTION, TAX_COMPUTATION)
     * @param pageable      pagination and sorting metadata
     * @return paged audit log records matching the criteria
     */
    Page<AuditLogEntity> getAuditLogs(String transactionId, AuditEventType eventType, Pageable pageable);
}
