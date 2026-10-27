package com.avega.tgd.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Summary report response for a customer's cumulative tax compliance.
 * Computes: complianceScore = 100 - (nonCompliantTransactions / totalTransactions * 100)
 */
public class CustomerTaxSummaryDto {

    private String customerId;
    private long totalTransactions;
    private BigDecimal totalAmount;
    private BigDecimal totalReportedTax;
    private BigDecimal totalExpectedTax;
    private BigDecimal totalTaxGap;
    private long compliantTransactions;
    private long nonCompliantTransactions;
    private BigDecimal complianceScore;

    public CustomerTaxSummaryDto() {
    }

    public CustomerTaxSummaryDto(String customerId, long totalTransactions, BigDecimal totalAmount,
                                 BigDecimal totalReportedTax, BigDecimal totalExpectedTax,
                                 BigDecimal totalTaxGap, long compliantTransactions, long nonCompliantTransactions) {
        this.customerId = customerId;
        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.totalReportedTax = totalReportedTax != null ? totalReportedTax : BigDecimal.ZERO;
        this.totalExpectedTax = totalExpectedTax != null ? totalExpectedTax : BigDecimal.ZERO;
        this.totalTaxGap = totalTaxGap != null ? totalTaxGap : BigDecimal.ZERO;
        this.compliantTransactions = compliantTransactions;
        this.nonCompliantTransactions = nonCompliantTransactions;
        this.complianceScore = calculateComplianceScore(totalTransactions, nonCompliantTransactions);
    }

    /**
     * Calculates compliance score:
     * complianceScore = 100 - (nonCompliantTransactions / totalTransactions * 100)
     */
    public static BigDecimal calculateComplianceScore(long total, long nonCompliant) {
        if (total <= 0) {
            return BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalBd = BigDecimal.valueOf(total);
        BigDecimal nonCompliantBd = BigDecimal.valueOf(nonCompliant);
        BigDecimal percentageNonCompliant = nonCompliantBd
                .multiply(BigDecimal.valueOf(100))
                .divide(totalBd, 4, RoundingMode.HALF_UP);
        BigDecimal score = BigDecimal.valueOf(100).subtract(percentageNonCompliant);
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    // Getters and Setters

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalReportedTax() {
        return totalReportedTax;
    }

    public void setTotalReportedTax(BigDecimal totalReportedTax) {
        this.totalReportedTax = totalReportedTax;
    }

    public BigDecimal getTotalExpectedTax() {
        return totalExpectedTax;
    }

    public void setTotalExpectedTax(BigDecimal totalExpectedTax) {
        this.totalExpectedTax = totalExpectedTax;
    }

    public BigDecimal getTotalTaxGap() {
        return totalTaxGap;
    }

    public void setTotalTaxGap(BigDecimal totalTaxGap) {
        this.totalTaxGap = totalTaxGap;
    }

    public long getCompliantTransactions() {
        return compliantTransactions;
    }

    public void setCompliantTransactions(long compliantTransactions) {
        this.compliantTransactions = compliantTransactions;
    }

    public long getNonCompliantTransactions() {
        return nonCompliantTransactions;
    }

    public void setNonCompliantTransactions(long nonCompliantTransactions) {
        this.nonCompliantTransactions = nonCompliantTransactions;
    }

    public BigDecimal getComplianceScore() {
        return complianceScore;
    }

    public void setComplianceScore(BigDecimal complianceScore) {
        this.complianceScore = complianceScore;
    }
}
