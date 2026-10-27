package com.avega.tgd.service.dto;

import com.avega.tgd.domain.enums.ComplianceStatus;

import java.math.BigDecimal;

/**
 * Result object produced by the Tax Gap Calculation Engine.
 */
public class TaxGapResult {

    private final BigDecimal expectedTax;
    private final BigDecimal taxGap;
    private final ComplianceStatus complianceStatus;
    private final String notes;

    public TaxGapResult(BigDecimal expectedTax, BigDecimal taxGap, ComplianceStatus complianceStatus, String notes) {
        this.expectedTax = expectedTax;
        this.taxGap = taxGap;
        this.complianceStatus = complianceStatus;
        this.notes = notes;
    }

    public BigDecimal getExpectedTax() {
        return expectedTax;
    }

    public BigDecimal getTaxGap() {
        return taxGap;
    }

    public ComplianceStatus getComplianceStatus() {
        return complianceStatus;
    }

    public String getNotes() {
        return notes;
    }
}
