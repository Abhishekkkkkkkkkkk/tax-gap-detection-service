package com.avega.tgd.domain.entity;

import com.avega.tgd.domain.enums.AuditEventType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity maintaining an append-only audit trail of system events.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AuditEventType eventType;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    public AuditLogEntity() {
    }

    public AuditLogEntity(AuditEventType eventType, String transactionId, LocalDateTime timestamp, String detailJson) {
        this.eventType = eventType;
        this.transactionId = transactionId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.detailJson = detailJson;
    }

    @PrePersist
    public void onPrePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }
}
