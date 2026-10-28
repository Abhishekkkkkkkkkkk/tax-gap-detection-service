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
import java.math.RoundingMode;

/**
 * Compliance rule flagging transactions exceeding a configured high-value threshold.
 */
@Component
public class HighValueTransactionRuleEvaluator implements TaxRuleEvaluator {

    public static final String RULE_NAME = "HIGH_VALUE_TRANSACTION";
    private static final Logger log = LoggerFactory.getLogger(HighValueTransactionRuleEvaluator.class);

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("100000.00");

    private final ObjectMapper objectMapper;

    public HighValueTransactionRuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEntity transaction, String configJson, Severity defaultSeverity) {
        if (transaction == null || transaction.getAmount() == null) {
            return RuleEvaluationResult.pass();
        }

        BigDecimal threshold = DEFAULT_THRESHOLD;

        // Parse custom threshold from rule configuration JSON if present
        if (configJson != null && !configJson.trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(configJson);
                if (root.has("threshold") && !root.get("threshold").isNull()) {
                    threshold = new BigDecimal(root.get("threshold").asText()).setScale(2, RoundingMode.HALF_UP);
                }
            } catch (Exception e) {
                log.warn("Failed to parse configJson for rule {}: {}. Falling back to default threshold {}",
                        RULE_NAME, configJson, DEFAULT_THRESHOLD);
            }
        }

        if (transaction.getAmount().compareTo(threshold) > 0) {
            Severity severity = defaultSeverity != null ? defaultSeverity : Severity.HIGH;
            String message = String.format("Transaction amount %s exceeds high-value threshold of %s",
                    transaction.getAmount(), threshold);
            return RuleEvaluationResult.fail(severity, message);
        }

        return RuleEvaluationResult.pass();
    }
}
