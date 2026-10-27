package com.avega.tgd.service.impl;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.ComplianceStatus;
import com.avega.tgd.service.TaxGapEngineService;
import com.avega.tgd.service.dto.TaxGapResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementation of the Tax Gap Calculation Engine.
 *
 * Rules:
 * - expectedTax = amount * taxRate (rounded to 2 decimal places with HALF_UP)
 * - taxGap = expectedTax - reportedTax
 * - Compliance:
 *     reportedTax missing -> NON_COMPLIANT
 *     |taxGap| <= 1        -> COMPLIANT
 *     taxGap > 1           -> UNDERPAID
 *     taxGap < -1          -> OVERPAID
 */
@Service
public class TaxGapEngineServiceImpl implements TaxGapEngineService {

    private static final Logger log = LoggerFactory.getLogger(TaxGapEngineServiceImpl.class);

    private static final BigDecimal TOLERANCE_ONE = new BigDecimal("1.00");
    private static final BigDecimal NEGATIVE_TOLERANCE_ONE = new BigDecimal("-1.00");

    @Override
    public TaxGapResult computeTaxGap(BigDecimal amount, BigDecimal taxRate, BigDecimal reportedTax) {
        if (amount == null || taxRate == null) {
            throw new IllegalArgumentException("Amount and taxRate cannot be null for tax gap computation");
        }

        // Normalize tax rate: if rate was passed as whole percentage (e.g. 18.00 instead of 0.18)
        BigDecimal normalizedRate = normalizeTaxRate(taxRate);

        // expectedTax = amount * normalizedRate
        BigDecimal expectedTax = amount.multiply(normalizedRate).setScale(2, RoundingMode.HALF_UP);

        // If reportedTax is missing (null) -> NON_COMPLIANT
        if (reportedTax == null) {
            log.debug("Reported tax is missing for amount {} and taxRate {}. Status: NON_COMPLIANT", amount, taxRate);
            return new TaxGapResult(expectedTax, expectedTax, ComplianceStatus.NON_COMPLIANT, "Reported tax is missing");
        }

        BigDecimal scaledReportedTax = reportedTax.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxGap = expectedTax.subtract(scaledReportedTax).setScale(2, RoundingMode.HALF_UP);

        ComplianceStatus status;
        String notes;

        // Determine Compliance:
        // |taxGap| <= 1 -> COMPLIANT
        // taxGap > 1   -> UNDERPAID
        // taxGap < -1  -> OVERPAID
        BigDecimal absGap = taxGap.abs();
        if (absGap.compareTo(TOLERANCE_ONE) <= 0) {
            status = ComplianceStatus.COMPLIANT;
            notes = "Tax gap within acceptable threshold (+/- 1.00)";
        } else if (taxGap.compareTo(TOLERANCE_ONE) > 0) {
            status = ComplianceStatus.UNDERPAID;
            notes = "Underpaid tax: expected " + expectedTax + ", reported " + scaledReportedTax + " (gap: " + taxGap + ")";
        } else {
            status = ComplianceStatus.OVERPAID;
            notes = "Overpaid tax: expected " + expectedTax + ", reported " + scaledReportedTax + " (gap: " + taxGap + ")";
        }

        return new TaxGapResult(expectedTax, taxGap, status, notes);
    }

    @Override
    public TaxGapResult evaluateTransaction(TransactionEntity transaction) {
        TaxGapResult result = computeTaxGap(transaction.getAmount(), transaction.getTaxRate(), transaction.getReportedTax());
        transaction.setExpectedTax(result.getExpectedTax());
        transaction.setTaxGap(result.getTaxGap());
        transaction.setComplianceStatus(result.getComplianceStatus());
        return result;
    }

    /**
     * Normalizes percentage rates. If a user supplies 18.0 instead of 0.18, divides by 100.
     */
    private BigDecimal normalizeTaxRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return rate;
    }
}
