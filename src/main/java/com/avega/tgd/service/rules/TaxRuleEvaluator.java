package com.avega.tgd.service.rules;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.Severity;

/**
 * Strategy interface for evaluating specific business tax rules against a transaction.
 */
public interface TaxRuleEvaluator {

    /**
     * Unique identifier matching ruleName in the database.
     */
    String getRuleName();

    /**
     * Evaluates the rule for the given transaction and rule-specific JSON configuration.
     *
     * @param transaction The transaction entity being evaluated
     * @param configJson  Rule configuration stored in the database
     * @param defaultSeverity Fallback severity configured for the rule
     * @return Result of the rule evaluation
     */
    RuleEvaluationResult evaluate(TransactionEntity transaction, String configJson, Severity defaultSeverity);
}
