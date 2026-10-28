package com.avega.tgd.domain.entity;

import com.avega.tgd.domain.enums.ComplianceStatus;
import com.avega.tgd.domain.enums.TransactionType;
import com.avega.tgd.domain.enums.ValidationStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing a financial transaction record throughout its compliance lifecycle.
 *
 * <p>Requirements Addressed:
 * <ul>
 *   <li><b>Requirement 1 (Transaction Fields & Storage):</b> Persists all input fields (transactionId,
 *       date, customerId, amount, taxRate, reportedTax, transactionType) alongside raw payload,
 *       validation status (SUCCESS / FAILURE), and failure reasons.</li>
 *   <li><b>Requirement 2 (Tax Gap Storage):</b> Persists calculated {@code expectedTax}, {@code taxGap},
 *       and determined {@code complianceStatus} (COMPLIANT, UNDERPAID, OVERPAID, NON_COMPLIANT).</li>
 * </ul>
 *
 * <p>Database Indexing Strategy:
 * <ul>
 *   <li>{@code idx_tx_transaction_id}: Unique index ensuring external identifier uniqueness and fast lookups.</li>
 *   <li>{@code idx_tx_customer_id}: Speeds up customer history queries and SQL aggregation reports.</li>
 *   <li>{@code idx_tx_compliance_status}: Optimizes conditional compliance queries and counts.</li>
 *   <li>{@code idx_tx_date}: Facilitates temporal range filtering and audit reporting.</li>
 * </ul>
 *
 * <p>Precision & Scale Rationale:
 * Financial values use {@code precision = 18, scale = 2} to eliminate binary floating-point drift.
 * Tax rates use {@code precision = 8, scale = 4} to support fractional percentage rates.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_transaction_id", columnList = "transaction_id", unique = true),
        @Index(name = "idx_tx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_tx_compliance_status", columnList = "compliance_status"),
        @Index(name = "idx_tx_date", columnList = "transaction_date")
})
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    private String transactionId;

    @Column(name = "transaction_date")
    private LocalDate date;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_rate", precision = 8, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "reported_tax", precision = 18, scale = 2)
    private BigDecimal reportedTax;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20)
    private TransactionType transactionType;

    @Column(name = "original_transaction_id", length = 100)
    private String originalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 20)
    private ValidationStatus validationStatus;

    @Column(name = "failure_reasons", columnDefinition = "TEXT")
    private String failureReasons;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "expected_tax", precision = 18, scale = 2)
    private BigDecimal expectedTax;

    @Column(name = "tax_gap", precision = 18, scale = 2)
    private BigDecimal taxGap;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status", length = 20)
    private ComplianceStatus complianceStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TransactionEntity() {
    }

    /**
     * JPA lifecycle callback establishing creation and modification timestamps prior to insertion.
     */
    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback updating modification timestamp on entity updates.
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getReportedTax() {
        return reportedTax;
    }

    public void setReportedTax(BigDecimal reportedTax) {
        this.reportedTax = reportedTax;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getFailureReasons() {
        return failureReasons;
    }

    public void setFailureReasons(String failureReasons) {
        this.failureReasons = failureReasons;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public BigDecimal getExpectedTax() {
        return expectedTax;
    }

    public void setExpectedTax(BigDecimal expectedTax) {
        this.expectedTax = expectedTax;
    }

    public BigDecimal getTaxGap() {
        return taxGap;
    }

    public void setTaxGap(BigDecimal taxGap) {
        this.taxGap = taxGap;
    }

    public ComplianceStatus getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(ComplianceStatus complianceStatus) {
        this.complianceStatus = complianceStatus;
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
