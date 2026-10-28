package com.avega.tgd.service.rules.impl;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.service.rules.RuleEvaluationResult;
import com.avega.tgd.service.rules.TaxRuleEvaluator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mandatory Rule (A): High-Value Transaction Rule.
 *
 * <p>Business Rationale:
 * High-value financial transactions represent significant audit risk for tax leakage and require
 * enhanced auditor scrutiny. When a transaction's gross amount exceeds the configured threshold,
 * this evaluator flags an audit exception with {@link Severity#HIGH}.
 *
 * <p>Configuration Format (JSON):
 * <pre>{@code
 *   { "threshold": 100000.00 }
 * }</pre>
 * If configuration is missing or unparseable, falls back to the default threshold of 100,000.00.
 */
@Component
public class HighValueTransactionRuleEvaluator implements TaxRuleEvaluator {

    public static final String RULE_NAME = "HIGH_VALUE_TRANSACTION";
    private static final Logger log = LoggerFactory.getLogger(HighValueTransactionRuleEvaluator.class);

    // Default threshold if not overridden by the database JSON configuration
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("100000.00");

    private final ObjectMapper objectMapper;

    public HighValueTransactionRuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    /**
     * Evaluates whether the transaction amount exceeds the threshold.
     *
     * @param transaction     Transaction entity to evaluate
     * @param configJson      JSON configuration string containing threshold
     * @param defaultSeverity Severity level defined on the database rule record
     * @return {@link RuleEvaluationResult#pass()} or {@link RuleEvaluationResult#fail(Severity, String)}
     */
    @Override
    public RuleEvaluationResult evaluate(TransactionEntity transaction, String configJson, Severity defaultSeverity) {
        if (transaction == null || transaction.getAmount() == null) {
            return RuleEvaluationResult.pass();
        }

        BigDecimal threshold = DEFAULT_THRESHOLD;

        // Parse custom threshold from JSON configuration if present in database
        if (configJson != null && !configJson.trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(configJson);
                if (root.has("threshold") && !root.get("threshold").isNull()) {
                    threshold = new BigDecimal(root.get("threshold").asText()).setScale(2, java.math.RoundingMode.HALF_UP);
                }
            } catch (Exception e) {
                log.warn("Failed to parse configJson for rule {}: {}. Falling back to default threshold {}",
                        RULE_NAME, configJson, DEFAULT_THRESHOLD);
            }
        }

        // Trigger an exception if gross amount exceeds the threshold
        if (transaction.getAmount().compareTo(threshold) > 0) {
            Severity severity = defaultSeverity != null ? defaultSeverity : Severity.HIGH;
            String message = String.format("Transaction amount %s exceeds high-value threshold of %s",
                    transaction.getAmount(), threshold);
            return RuleEvaluationResult.fail(severity, message);
        }

        return RuleEvaluationResult.pass();
    }
}
