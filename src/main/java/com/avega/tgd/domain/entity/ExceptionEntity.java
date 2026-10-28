package com.avega.tgd.domain.entity;

import com.avega.tgd.domain.enums.Severity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a compliance exception record.
 *
 * <p>Directly satisfies Requirement 4 ("Exception Management"):
 * <i>"When any rule or tax calculation fails, create an exception record with:
 * transactionId, customerId, ruleName, severity (HIGH / MEDIUM / LOW), message, timestamp"</i>.
 *
 * <p>Database Indexing Strategy:
 * <ul>
 *   <li>{@code idx_ex_transaction_id}: Enables immediate lookup of all exceptions generated for a transaction.</li>
 *   <li>{@code idx_ex_customer_id}: Accelerates customer-level risk filtering and summary aggregation.</li>
 *   <li>{@code idx_ex_rule_name}: Supports analytics on specific compliance rule failure rates.</li>
 *   <li>{@code idx_ex_severity}: Enables efficient filtering and counting by severity level in SQL.</li>
 *   <li>{@code idx_ex_timestamp}: Facilitates timeline queries and default descending sort.</li>
 * </ul>
 */
@Entity
@Table(name = "exceptions", indexes = {
        @Index(name = "idx_ex_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_ex_customer_id", columnList = "customer_id"),
        @Index(name = "idx_ex_rule_name", columnList = "rule_name"),
        @Index(name = "idx_ex_severity", columnList = "severity"),
        @Index(name = "idx_ex_timestamp", columnList = "timestamp")
})
public class ExceptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public ExceptionEntity() {
    }

    public ExceptionEntity(String transactionId, String customerId, String ruleName, Severity severity, String message, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.ruleName = ruleName;
        this.severity = severity;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback guaranteeing timestamp initialization prior to persistence.
     */
    @PrePersist
    public void onPrePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
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
