package com.avega.tgd.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Data Transfer Object delivering the aggregated tax compliance summary for a customer.
 *
 * <p>Directly satisfies Requirement 6.A ("Customer Tax Summary Report"):
 * <ul>
 *   <li>{@code totalAmount}: Cumulative monetary volume of all successful transactions.</li>
 *   <li>{@code totalReportedTax}: Cumulative tax reported by the customer.</li>
 *   <li>{@code totalExpectedTax}: Cumulative tax calculated by the system engine.</li>
 *   <li>{@code totalTaxGap}: Cumulative variance ({@code totalExpectedTax - totalReportedTax}).</li>
 *   <li>{@code complianceScore}: Percentage metric computed as:
 *       <pre>complianceScore = 100 - (nonCompliantTransactions / totalTransactions * 100)</pre></li>
 * </ul>
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
     * Computes the customer compliance score according to the business formula:
     * <pre>
     * complianceScore = 100 - (nonCompliantTransactions / totalTransactions * 100)
     * </pre>
     *
     * <p>Edge Cases Handled:
     * <ul>
     *   <li>If total transactions is 0 (or negative), score is 100.00.</li>
     *   <li>If non-compliant count equals or exceeds total count, score is clamped to 0.00 (no negative scores).</li>
     *   <li>Maintains scale of 4 during division before rounding to 2 decimal places with {@link RoundingMode#HALF_UP}.</li>
     * </ul>
     *
     * @param total        total count of transactions
     * @param nonCompliant count of non-compliant transactions
     * @return compliance score as a {@link BigDecimal} rounded to 2 decimal places
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
