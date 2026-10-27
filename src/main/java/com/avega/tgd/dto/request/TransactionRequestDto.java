package com.avega.tgd.dto.request;

import com.avega.tgd.domain.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for incoming financial transaction records.
 */
public class TransactionRequestDto {

    private String transactionId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String customerId;

    private BigDecimal amount;

    private BigDecimal taxRate;

    private BigDecimal reportedTax;

    private TransactionType transactionType;

    private String originalTransactionId;

    public TransactionRequestDto() {
    }

    public TransactionRequestDto(String transactionId, LocalDate date, String customerId,
                                 BigDecimal amount, BigDecimal taxRate, BigDecimal reportedTax,
                                 TransactionType transactionType, String originalTransactionId) {
        this.transactionId = transactionId;
        this.date = date;
        this.customerId = customerId;
        this.amount = amount;
        this.taxRate = taxRate;
        this.reportedTax = reportedTax;
        this.transactionType = transactionType;
        this.originalTransactionId = originalTransactionId;
    }

    // Getters and Setters

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
}
