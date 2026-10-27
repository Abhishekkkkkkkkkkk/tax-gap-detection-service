package com.avega.tgd.dto.response;

import com.avega.tgd.domain.entity.ExceptionEntity;
import com.avega.tgd.domain.enums.Severity;

import java.time.LocalDateTime;

/**
 * Response DTO for an individual exception record.
 */
public class ExceptionResponseDto {

    private Long id;
    private String transactionId;
    private String customerId;
    private String ruleName;
    private Severity severity;
    private String message;
    private LocalDateTime timestamp;

    public ExceptionResponseDto() {
    }

    public static ExceptionResponseDto fromEntity(ExceptionEntity entity) {
        if (entity == null) {
            return null;
        }
        ExceptionResponseDto dto = new ExceptionResponseDto();
        dto.setId(entity.getId());
        dto.setTransactionId(entity.getTransactionId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setRuleName(entity.getRuleName());
        dto.setSeverity(entity.getSeverity());
        dto.setMessage(entity.getMessage());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
