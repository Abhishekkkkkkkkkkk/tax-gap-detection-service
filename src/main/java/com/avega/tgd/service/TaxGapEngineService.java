package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.service.dto.TaxGapResult;

import java.math.BigDecimal;

/**
 * Service for computing expected tax, tax gap, and compliance classification.
 */
public interface TaxGapEngineService {

    TaxGapResult computeTaxGap(BigDecimal amount, BigDecimal taxRate, BigDecimal reportedTax);

    TaxGapResult evaluateTransaction(TransactionEntity transaction);
}
