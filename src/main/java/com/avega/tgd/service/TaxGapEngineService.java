package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.service.dto.TaxGapResult;

import java.math.BigDecimal;

/**
 * Service interface for calculating expected tax, tax gaps, and determining compliance status.
 */
public interface TaxGapEngineService {

    /**
     * Computes expected tax, tax gap, and compliance status for a transaction.
     *
     * @param amount      Transaction financial amount
     * @param taxRate     Applicable tax rate (e.g. 0.18 or 18)
     * @param reportedTax Reported tax (can be null)
     * @return Calculated TaxGapResult
     */
    TaxGapResult computeTaxGap(BigDecimal amount, BigDecimal taxRate, BigDecimal reportedTax);

    /**
     * Evaluates and updates the transaction entity with tax gap calculations.
     *
     * @param transaction Transaction entity to evaluate
     * @return Updated TaxGapResult
     */
    TaxGapResult evaluateTransaction(TransactionEntity transaction);
}
