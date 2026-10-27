package com.avega.tgd.dto.response;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;

import java.time.LocalDateTime;

/**
 * Response DTO for an audit log entry.
 */
public class AuditLogResponseDto {

    private Long id;
    private AuditEventType eventType;
    private String transactionId;
    private LocalDateTime timestamp;
    private String detailJson;

    public AuditLogResponseDto() {
    }

    public static AuditLogResponseDto fromEntity(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        AuditLogResponseDto dto = new AuditLogResponseDto();
        dto.setId(entity.getId());
        dto.setEventType(entity.getEventType());
        dto.setTransactionId(entity.getTransactionId());
        dto.setTimestamp(entity.getTimestamp());
        dto.setDetailJson(entity.getDetailJson());
        return dto;
    }

    // Getters and Setters

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
