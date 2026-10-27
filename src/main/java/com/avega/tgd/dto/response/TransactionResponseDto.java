package com.avega.tgd.dto.response;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.ComplianceStatus;
import com.avega.tgd.domain.enums.TransactionType;
import com.avega.tgd.domain.enums.ValidationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO representing the outcome of transaction ingestion, calculation, and compliance validation.
 */
public class TransactionResponseDto {

    private Long id;
    private String transactionId;
    private LocalDate date;
    private String customerId;
    private BigDecimal amount;
    private BigDecimal taxRate;
    private BigDecimal reportedTax;
    private TransactionType transactionType;
    private String originalTransactionId;
    private ValidationStatus validationStatus;
    private String failureReasons;
    private BigDecimal expectedTax;
    private BigDecimal taxGap;
    private ComplianceStatus complianceStatus;
    private LocalDateTime createdAt;

    public TransactionResponseDto() {
    }

    public static TransactionResponseDto fromEntity(TransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        TransactionResponseDto dto = new TransactionResponseDto();
        dto.setId(entity.getId());
        dto.setTransactionId(entity.getTransactionId());
        dto.setDate(entity.getDate());
        dto.setCustomerId(entity.getCustomerId());
        dto.setAmount(entity.getAmount());
        dto.setTaxRate(entity.getTaxRate());
        dto.setReportedTax(entity.getReportedTax());
        dto.setTransactionType(entity.getTransactionType());
        dto.setOriginalTransactionId(entity.getOriginalTransactionId());
        dto.setValidationStatus(entity.getValidationStatus());
        dto.setFailureReasons(entity.getFailureReasons());
        dto.setExpectedTax(entity.getExpectedTax());
        dto.setTaxGap(entity.getTaxGap());
        dto.setComplianceStatus(entity.getComplianceStatus());
        dto.setCreatedAt(entity.getCreatedAt());
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
}
