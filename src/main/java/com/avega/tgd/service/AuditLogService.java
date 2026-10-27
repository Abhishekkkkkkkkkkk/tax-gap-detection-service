package com.avega.tgd.service;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for logging and querying audit records.
 */
public interface AuditLogService {

    /**
     * Logs an audit event with detail JSON payload.
     */
    AuditLogEntity logEvent(AuditEventType eventType, String transactionId, Object detailPayload);

    /**
     * Retrieves audit logs for a transaction.
     */
    List<AuditLogEntity> getAuditLogsForTransaction(String transactionId);

    /**
     * Retrieves paged audit logs filtered optionally by transactionId or eventType.
     */
    Page<AuditLogEntity> getAuditLogs(String transactionId, AuditEventType eventType, Pageable pageable);
}
