package com.avega.tgd.service.rules.impl;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.domain.enums.TransactionType;
import com.avega.tgd.repository.TransactionRepository;
import com.avega.tgd.service.rules.RuleEvaluationResult;
import com.avega.tgd.service.rules.TaxRuleEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mandatory Rule (B): Refund Validation Rule
 * Refund amount must not exceed original sale amount.
 */
@Component
public class RefundValidationRuleEvaluator implements TaxRuleEvaluator {

    public static final String RULE_NAME = "REFUND_VALIDATION";
    private static final Logger log = LoggerFactory.getLogger(RefundValidationRuleEvaluator.class);

    private final TransactionRepository transactionRepository;

    public RefundValidationRuleEvaluator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEntity transaction, String configJson, Severity defaultSeverity) {
        if (transaction == null || transaction.getTransactionType() != TransactionType.REFUND) {
            return RuleEvaluationResult.pass();
        }

        Severity severity = defaultSeverity != null ? defaultSeverity : Severity.HIGH;

        // 1. Check if original transaction ID is explicitly linked
        if (transaction.getOriginalTransactionId() != null && !transaction.getOriginalTransactionId().trim().isEmpty()) {
            String origTxId = transaction.getOriginalTransactionId().trim();
            Optional<TransactionEntity> originalSaleOpt = transactionRepository.findFirstByTransactionIdAndTransactionType(
                    origTxId, TransactionType.SALE);

            if (originalSaleOpt.isEmpty()) {
                // If originalTransactionId cannot be found as a SALE, check general transaction id
                Optional<TransactionEntity> anyTxOpt = transactionRepository.findByTransactionId(origTxId);
                if (anyTxOpt.isEmpty()) {
                    return RuleEvaluationResult.fail(severity,
                            String.format("Original sale transaction '%s' was not found for refund", origTxId));
                } else if (anyTxOpt.get().getTransactionType() != TransactionType.SALE) {
                    return RuleEvaluationResult.fail(severity,
                            String.format("Linked original transaction '%s' is not a SALE (Type: %s)",
                                    origTxId, anyTxOpt.get().getTransactionType()));
                }
                originalSaleOpt = anyTxOpt;
            }

            TransactionEntity originalSale = originalSaleOpt.get();
            if (transaction.getAmount().compareTo(originalSale.getAmount()) > 0) {
                return RuleEvaluationResult.fail(severity,
                        String.format("Refund amount %s exceeds original sale amount %s (Original Sale ID: %s)",
                                transaction.getAmount(), originalSale.getAmount(), origTxId));
            }

            return RuleEvaluationResult.pass();
        }

        // 2. Fallback: if no originalTransactionId is specified, check against customer's existing sales
        if (transaction.getCustomerId() != null) {
            Optional<TransactionEntity> highestSaleOpt = transactionRepository
                    .findTopByCustomerIdAndTransactionTypeOrderByAmountDesc(
                            transaction.getCustomerId(), TransactionType.SALE);

            if (highestSaleOpt.isEmpty()) {
                return RuleEvaluationResult.fail(severity,
                        String.format("No previous SALE found for customer '%s' to justify refund amount %s",
                                transaction.getCustomerId(), transaction.getAmount()));
            }

            TransactionEntity highestSale = highestSaleOpt.get();
            if (transaction.getAmount().compareTo(highestSale.getAmount()) > 0) {
                return RuleEvaluationResult.fail(severity,
                        String.format("Refund amount %s exceeds highest recorded sale %s for customer '%s'",
                                transaction.getAmount(), highestSale.getAmount(), transaction.getCustomerId()));
            }
        }

        return RuleEvaluationResult.pass();
    }
}
