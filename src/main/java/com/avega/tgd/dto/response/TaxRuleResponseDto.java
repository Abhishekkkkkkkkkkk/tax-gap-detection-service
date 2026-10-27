package com.avega.tgd.dto.response;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import com.avega.tgd.domain.enums.Severity;

import java.time.LocalDateTime;

/**
 * Response DTO for a tax rule configuration.
 */
public class TaxRuleResponseDto {

    private Long id;
    private String ruleName;
    private String description;
    private boolean enabled;
    private Severity severity;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TaxRuleResponseDto() {
    }

    public static TaxRuleResponseDto fromEntity(TaxRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        TaxRuleResponseDto dto = new TaxRuleResponseDto();
        dto.setId(entity.getId());
        dto.setRuleName(entity.getRuleName());
        dto.setDescription(entity.getDescription());
        dto.setEnabled(entity.isEnabled());
        dto.setSeverity(entity.getSeverity());
        dto.setConfigJson(entity.getConfigJson());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
