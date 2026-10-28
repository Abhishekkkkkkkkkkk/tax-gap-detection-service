package com.avega.tgd.domain.entity;

import com.avega.tgd.domain.enums.Severity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a database-driven, configurable tax compliance rule.
 *
 * <p>Directly satisfies Requirement 3 ("Configurable Rule Engine Stored in Database"):
 * <ul>
 *   <li><b>Rule Persistence:</b> Rules are stored in the database with their metadata and severity level.</li>
 *   <li><b>Dynamic JSON Configuration:</b> The {@code config_json} column stores rule parameters
 *       (e.g., threshold limits, GST slab boundaries, unlinked refund policies) as JSON text,
 *       allowing policy adjustments without recompiling or redeploying code.</li>
 *   <li><b>Runtime Activation:</b> The {@code enabled} flag permits instantly toggling rules on or off.</li>
 * </ul>
 *
 * <p>Indexing Strategy:
 * <ul>
 *   <li>{@code idx_rule_name}: Unique index preventing duplicate rule definitions.</li>
 *   <li>{@code idx_rule_enabled}: Optimizes retrieval of active rules by {@code findByEnabledTrue()}.</li>
 * </ul>
 */
@Entity
@Table(name = "tax_rules", indexes = {
        @Index(name = "idx_rule_name", columnList = "rule_name", unique = true),
        @Index(name = "idx_rule_enabled", columnList = "enabled")
})
public class TaxRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, unique = true, length = 100)
    private String ruleName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TaxRuleEntity() {
    }

    public TaxRuleEntity(String ruleName, String description, boolean enabled, Severity severity, String configJson) {
        this.ruleName = ruleName;
        this.description = description;
        this.enabled = enabled;
        this.severity = severity;
        this.configJson = configJson;
    }

    /**
     * JPA lifecycle hook setting creation and update timestamps before insertion.
     */
    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle hook refreshing modification timestamp on update.
     */
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
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
