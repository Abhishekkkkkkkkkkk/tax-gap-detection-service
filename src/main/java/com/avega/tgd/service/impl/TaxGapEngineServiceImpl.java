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
 * Core implementation of the Tax Gap Calculation Engine.
 *
 * <p>Financial Computing Principles:
 * <ul>
 *   <li>All arithmetic operations utilize {@link BigDecimal} with explicit scales and {@link RoundingMode#HALF_UP}
 *       to completely avoid IEEE-754 binary floating-point rounding inaccuracies common with {@code float} or {@code double}.</li>
 *   <li>Tax rate inputs are normalized so both fractional formats (e.g., {@code 0.18}) and percentage formats
 *       (e.g., {@code 18.0}) are handled consistently without runtime parsing exceptions.</li>
 *   <li>A legal rounding tolerance of &plusmn;1.00 is permitted to accommodate itemized invoice rounding differences
 *       versus aggregated calculation discrepancies before flagging transactions as underpaid or overpaid.</li>
 * </ul>
 */
@Service
public class TaxGapEngineServiceImpl implements TaxGapEngineService {

    private static final Logger log = LoggerFactory.getLogger(TaxGapEngineServiceImpl.class);

    // Permissible rounding tolerance (+/- 1.00 in currency units)
    private static final BigDecimal TOLERANCE_ONE = new BigDecimal("1.00");
    private static final BigDecimal NEGATIVE_TOLERANCE_ONE = new BigDecimal("-1.00");

    /**
     * Executes the tax gap determination formula:
     * <ol>
     *   <li>Normalizes taxRate to a standard decimal factor (&le; 1.0).</li>
     *   <li>Computes statutory expected tax: {@code expectedTax = amount * normalizedRate} scaled to 2 decimal places.</li>
     *   <li>If {@code reportedTax} is omitted, marks as {@link ComplianceStatus#NON_COMPLIANT}.</li>
     *   <li>Computes gap: {@code taxGap = expectedTax - reportedTax}.</li>
     *   <li>Applies tolerance threshold (&plusmn;1.00) to classify into COMPLIANT, UNDERPAID, or OVERPAID.</li>
     * </ol>
     *
     * @param amount      The taxable transaction gross amount
     * @param taxRate     The applicable statutory tax rate
     * @param reportedTax The self-reported tax figure submitted by the taxpayer
     * @return Calculation result containing amounts, gap, and compliance status
     */
    @Override
    public TaxGapResult computeTaxGap(BigDecimal amount, BigDecimal taxRate, BigDecimal reportedTax) {
        if (amount == null || taxRate == null) {
            throw new IllegalArgumentException("Amount and taxRate cannot be null for tax gap computation");
        }

        // Normalize tax rate: handles cases where upstream callers supply either 0.18 or 18.0 (18%)
        BigDecimal normalizedRate = normalizeTaxRate(taxRate);

        // expectedTax = amount * normalizedRate, rounded standard commercial half-up to cents/paise
        BigDecimal expectedTax = amount.multiply(normalizedRate).setScale(2, RoundingMode.HALF_UP);

        // A transaction with missing reportedTax cannot be verified and is treated as an audit violation
        if (reportedTax == null) {
            log.debug("Reported tax is missing for amount {} and taxRate {}. Status: NON_COMPLIANT", amount, taxRate);
            return new TaxGapResult(expectedTax, expectedTax, ComplianceStatus.NON_COMPLIANT, "Reported tax is missing");
        }

        BigDecimal scaledReportedTax = reportedTax.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxGap = expectedTax.subtract(scaledReportedTax).setScale(2, RoundingMode.HALF_UP);

        ComplianceStatus status;
        String notes;

        // Tolerance evaluation:
        // |taxGap| <= 1.00 -> Acceptable rounding variance (COMPLIANT)
        // taxGap > 1.00   -> Taxpayer underpaid their expected liability (UNDERPAID)
        // taxGap < -1.00  -> Taxpayer overpaid compared to statutory calculation (OVERPAID)
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

    /**
     * Evaluates and updates the state of a {@link TransactionEntity} with computed tax values.
     *
     * @param transaction The target entity to update
     * @return Result of the computation
     */
    @Override
    public TaxGapResult evaluateTransaction(TransactionEntity transaction) {
        TaxGapResult result = computeTaxGap(transaction.getAmount(), transaction.getTaxRate(), transaction.getReportedTax());
        transaction.setExpectedTax(result.getExpectedTax());
        transaction.setTaxGap(result.getTaxGap());
        transaction.setComplianceStatus(result.getComplianceStatus());
        return result;
    }

    /**
     * Normalizes percentage rates into decimal multipliers.
     * For example, a rate passed as 18.0 (for 18%) is converted to 0.180000,
     * while a rate already passed as 0.18 is preserved as-is.
     *
     * @param rate Input rate
     * @return Decimal multiplier between 0 and 1
     */
    private BigDecimal normalizeTaxRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return rate;
    }
}
