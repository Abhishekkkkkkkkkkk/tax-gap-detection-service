package com.avega.tgd.dto.request;

import com.avega.tgd.domain.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating a configurable tax compliance rule.
 */
public class TaxRuleRequestDto {

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    private String description;

    private boolean enabled = true;

    @NotNull(message = "Severity is required")
    private Severity severity;

    private String configJson;

    public TaxRuleRequestDto() {
    }

    public TaxRuleRequestDto(String ruleName, String description, boolean enabled, Severity severity, String configJson) {
        this.ruleName = ruleName;
        this.description = description;
        this.enabled = enabled;
        this.severity = severity;
        this.configJson = configJson;
    }

    // Getters and Setters

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
}
