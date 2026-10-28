package com.avega.tgd.repository;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for persisting and querying immutable audit records ({@link AuditLogEntity}).
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByTransactionId(String transactionId);

    Page<AuditLogEntity> findByTransactionId(String transactionId, Pageable pageable);

    Page<AuditLogEntity> findByEventType(AuditEventType eventType, Pageable pageable);
}
