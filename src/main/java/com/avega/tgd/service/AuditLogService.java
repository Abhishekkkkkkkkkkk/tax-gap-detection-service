package com.avega.tgd.service;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service for recording and querying immutable audit trail records.
 */
public interface AuditLogService {

    AuditLogEntity logEvent(AuditEventType eventType, String transactionId, Object detailPayload);

    List<AuditLogEntity> getAuditLogsForTransaction(String transactionId);

    Page<AuditLogEntity> getAuditLogs(String transactionId, AuditEventType eventType, Pageable pageable);
}
