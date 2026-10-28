package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.service.dto.TaxGapResult;

import java.math.BigDecimal;

/**
 * Service interface for calculating expected statutory tax, computing tax gaps,
 * and classifying transactions into audit compliance categories.
 *
 * <p>Core Formulas &amp; Rules:
 * <ul>
 *   <li><b>Expected Tax:</b> {@code expectedTax = amount * taxRate} (rounded Half-Up to 2 decimal places).</li>
 *   <li><b>Tax Gap:</b> {@code taxGap = expectedTax - reportedTax}.</li>
 *   <li><b>Compliance Status:</b>
 *     <ul>
 *       <li>{@code reportedTax == null} &rarr; {@code NON_COMPLIANT} (tax evasion or missing mandatory audit data).</li>
 *       <li>{@code |taxGap| <= 1.00} &rarr; {@code COMPLIANT} (within allowable rounding tolerance).</li>
 *       <li>{@code taxGap > 1.00} &rarr; {@code UNDERPAID} (under-reported tax liability).</li>
 *       <li>{@code taxGap < -1.00} &rarr; {@code OVERPAID} (excess reported tax).</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public interface TaxGapEngineService {

    /**
     * Computes the expected tax, mathematical tax gap, and compliance classification
     * for a given financial transaction.
     *
     * @param amount      The gross financial amount (must be positive, non-null)
     * @param taxRate     The statutory tax rate (e.g. 0.18 or 18% as decimal/whole percentage)
     * @param reportedTax The tax amount reported on the transaction invoice (nullable)
     * @return A {@link TaxGapResult} holding expectedTax, taxGap, complianceStatus, and audit notes
     * @throws IllegalArgumentException if amount or taxRate is null
     */
    TaxGapResult computeTaxGap(BigDecimal amount, BigDecimal taxRate, BigDecimal reportedTax);

    /**
     * Evaluates a persisted or in-flight {@link TransactionEntity}, computing tax gap metrics
     * and directly mutating the entity's {@code expectedTax}, {@code taxGap}, and {@code complianceStatus}.
     *
     * @param transaction The target transaction entity to evaluate and mutate
     * @return The calculated {@link TaxGapResult} containing detailed computation outcomes
     */
    TaxGapResult evaluateTransaction(TransactionEntity transaction);
}
