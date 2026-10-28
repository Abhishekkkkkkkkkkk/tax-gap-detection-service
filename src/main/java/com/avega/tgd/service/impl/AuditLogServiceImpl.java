package com.avega.tgd.service.impl;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import com.avega.tgd.repository.AuditLogRepository;
import com.avega.tgd.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of {@link AuditLogService} providing persistent, immutable audit logging.
 *
 * <p>Key Architectural Decisions:
 * <ul>
 *   <li><b>Propagation.REQUIRES_NEW:</b> The {@link #logEvent} method suspends any existing outer
 *       transaction and executes within an independent physical transaction. This ensures that
 *       even if an ingestion batch or business computation encounters an unhandled exception
 *       or rolls back, the audit record is committed and permanently visible to auditors.</li>
 *   <li><b>Fault-Tolerant Serialization:</b> Payloads are converted to JSON using Jackson's
 *       {@link ObjectMapper}. If reflection or serialization fails, the service gracefully falls
 *       back to {@code String.valueOf(detailPayload)} rather than throwing an exception that
 *       could interrupt the main processing pipeline.</li>
 *   <li><b>Stateless Queries:</b> Query methods are marked with {@code @Transactional(readOnly = true)}
 *       to optimize JDBC driver read locks and bypass Hibernate dirty-checking overhead.</li>
 * </ul>
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists an audit log event in an autonomous transaction boundary.
     *
     * <p>Executes with {@code Propagation.REQUIRES_NEW} so the database commit completes
     * independently of the caller's transaction status.
     *
     * @param eventType     the category of event (INGESTION, RULE_EXECUTION, TAX_COMPUTATION)
     * @param transactionId unique transaction reference; defaulted to "N/A" if null
     * @param detailPayload arbitrary payload object serialized to JSON
     * @return the saved {@link AuditLogEntity}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogEntity logEvent(AuditEventType eventType, String transactionId, Object detailPayload) {
        String detailJson = null;
        if (detailPayload != null) {
            try {
                // If the caller already provided a formatted JSON string, avoid double-serialization
                if (detailPayload instanceof String) {
                    detailJson = (String) detailPayload;
                } else {
                    detailJson = objectMapper.writeValueAsString(detailPayload);
                }
            } catch (Exception e) {
                // Fault isolation: do not fail audit logging if serialization encounters issues
                log.warn("Failed to serialize audit detail payload: {}", e.getMessage());
                detailJson = String.valueOf(detailPayload);
            }
        }

        AuditLogEntity auditLog = new AuditLogEntity(
                eventType,
                transactionId != null ? transactionId : "N/A",
                LocalDateTime.now(),
                detailJson
        );

        AuditLogEntity saved = auditLogRepository.save(auditLog);
        log.info("AUDIT [{}] for transaction '{}': {}", eventType, transactionId, detailJson);
        return saved;
    }

    /**
     * Finds all audit trail entries for a given transaction identifier.
     *
     * @param transactionId the unique transaction identifier
     * @return list of audit records ordered by generation
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntity> getAuditLogsForTransaction(String transactionId) {
        return auditLogRepository.findByTransactionId(transactionId);
    }

    /**
     * Retrieves audit entries based on optional transactionId and eventType criteria.
     *
     * @param transactionId optional transaction identifier filter
     * @param eventType     optional event type filter
     * @param pageable      page request metadata
     * @return paged audit logs
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogs(String transactionId, AuditEventType eventType, Pageable pageable) {
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            return auditLogRepository.findByTransactionId(transactionId, pageable);
        }
        if (eventType != null) {
            return auditLogRepository.findByEventType(eventType, pageable);
        }
        return auditLogRepository.findAll(pageable);
    }
}
