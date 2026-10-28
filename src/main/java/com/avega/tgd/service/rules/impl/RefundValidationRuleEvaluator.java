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
 * Mandatory Rule (B): Refund Validation Rule.
 *
 * <p>Business Rationale:
 * A customer should never be credited a refund greater than their original purchase.
 * Fraudulent or erroneous refunds erode tax bases through illegitimate input tax credits (ITC).
 *
 * <p>Evaluation Strategy:
 * <ol>
 *   <li>Non-refund transactions (SALE, EXPENSE) bypass this rule immediately.</li>
 *   <li><b>Explicit Link:</b> If {@code originalTransactionId} is supplied, the system queries the database
 *       to verify that the referenced transaction exists, was of type {@code SALE}, and that {@code refundAmount <= saleAmount}.</li>
 *   <li><b>Historical Fallback:</b> If {@code originalTransactionId} is omitted, the evaluator queries the customer's
 *       highest recorded historical {@code SALE}. If no sale exists or the refund exceeds that highest sale, an exception is flagged.</li>
 * </ol>
 *
 * <p>Implementation Note / Future Scope:
 * The current implementation evaluates the refund against the referenced single sale's amount. Tracking
 * cumulative partial refunds against a single sale across multiple separate refund transactions can be
 * enabled in future iterations by summing previous refunds matching {@code originalTransactionId}.
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

    /**
     * Evaluates refund amounts against original sales records.
     *
     * @param transaction     Transaction to evaluate
     * @param configJson      Rule configuration JSON
     * @param defaultSeverity Fallback severity configured for the rule
     * @return Evaluation pass/fail result
     */
    @Override
    public RuleEvaluationResult evaluate(TransactionEntity transaction, String configJson, Severity defaultSeverity) {
        // Non-refund transactions are exempt from refund validation
        if (transaction == null || transaction.getTransactionType() != TransactionType.REFUND) {
            return RuleEvaluationResult.pass();
        }

        Severity severity = defaultSeverity != null ? defaultSeverity : Severity.HIGH;

        // Strategy 1: Check against explicitly linked original transaction ID
        if (transaction.getOriginalTransactionId() != null && !transaction.getOriginalTransactionId().trim().isEmpty()) {
            String origTxId = transaction.getOriginalTransactionId().trim();
            Optional<TransactionEntity> originalSaleOpt = transactionRepository.findFirstByTransactionIdAndTransactionType(
                    origTxId, TransactionType.SALE);

            if (originalSaleOpt.isEmpty()) {
                // Determine whether the transaction ID was completely missing or was not a SALE
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
            // Flag exception if refund amount exceeds original sale amount
            if (transaction.getAmount().compareTo(originalSale.getAmount()) > 0) {
                return RuleEvaluationResult.fail(severity,
                        String.format("Refund amount %s exceeds original sale amount %s (Original Sale ID: %s)",
                                transaction.getAmount(), originalSale.getAmount(), origTxId));
            }

            return RuleEvaluationResult.pass();
        }

        // Strategy 2: Fallback when unlinked - verify customer has at least one historical sale >= refund amount
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
