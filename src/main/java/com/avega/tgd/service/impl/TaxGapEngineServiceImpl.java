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
 * Core implementation of the Tax Gap Calculation Engine using BigDecimal arithmetic and rounding tolerance.
 */
@Service
public class TaxGapEngineServiceImpl implements TaxGapEngineService {

    private static final Logger log = LoggerFactory.getLogger(TaxGapEngineServiceImpl.class);

    // Permissible rounding tolerance (+/- 1.00)
    private static final BigDecimal TOLERANCE_ONE = new BigDecimal("1.00");

    @Override
    public TaxGapResult computeTaxGap(BigDecimal amount, BigDecimal taxRate, BigDecimal reportedTax) {
        if (amount == null || taxRate == null) {
            throw new IllegalArgumentException("Amount and taxRate cannot be null for tax gap computation");
        }

        BigDecimal normalizedRate = normalizeTaxRate(taxRate);
        BigDecimal expectedTax = amount.multiply(normalizedRate).setScale(2, RoundingMode.HALF_UP);

        // Missing reported tax cannot be verified and is treated as non-compliant
        if (reportedTax == null) {
            log.debug("Reported tax is missing for amount {} and taxRate {}. Status: NON_COMPLIANT", amount, taxRate);
            return new TaxGapResult(expectedTax, expectedTax, ComplianceStatus.NON_COMPLIANT, "Reported tax is missing");
        }

        BigDecimal scaledReportedTax = reportedTax.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxGap = expectedTax.subtract(scaledReportedTax).setScale(2, RoundingMode.HALF_UP);

        ComplianceStatus status;
        String notes;

        // Apply +/- 1.00 tolerance boundary
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
     * Normalizes percentage rates (e.g. 18.0) into decimal multipliers (0.18).
     */
    private BigDecimal normalizeTaxRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return rate;
    }
}
