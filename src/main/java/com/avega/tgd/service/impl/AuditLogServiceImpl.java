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
 * Implementation of {@link AuditLogService} providing persistent audit logging.
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

    // Executes in an independent physical transaction so audit entries persist even if outer transaction rolls back
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLogEntity logEvent(AuditEventType eventType, String transactionId, Object detailPayload) {
        String detailJson = null;
        if (detailPayload != null) {
            try {
                if (detailPayload instanceof String) {
                    detailJson = (String) detailPayload;
                } else {
                    detailJson = objectMapper.writeValueAsString(detailPayload);
                }
            } catch (Exception e) {
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

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntity> getAuditLogsForTransaction(String transactionId) {
        return auditLogRepository.findByTransactionId(transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogs(String transactionId, AuditEventType eventType, Pageable pageable) {
        if (transactionId != null && !transactionId.trim().isEmpty()) {
            return auditLogRepository.findByTransactionId(transactionId.trim(), pageable);
        }
        if (eventType != null) {
            return auditLogRepository.findByEventType(eventType, pageable);
        }
        return auditLogRepository.findAll(pageable);
    }
}
