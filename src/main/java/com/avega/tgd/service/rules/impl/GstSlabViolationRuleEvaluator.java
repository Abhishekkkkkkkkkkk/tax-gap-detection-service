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
 * Compliance rule enforcing statutory minimum tax rates for transactions exceeding slab thresholds.
 */
@Component
public class GstSlabViolationRuleEvaluator implements TaxRuleEvaluator {

    public static final String RULE_NAME = "GST_SLAB_VIOLATION";
    private static final Logger log = LoggerFactory.getLogger(GstSlabViolationRuleEvaluator.class);

    private static final BigDecimal DEFAULT_SLAB_THRESHOLD = new BigDecimal("50000.00");
    private static final BigDecimal DEFAULT_REQUIRED_TAX_RATE = new BigDecimal("0.18");

    private final ObjectMapper objectMapper;

    public GstSlabViolationRuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEntity transaction, String configJson, Severity defaultSeverity) {
        if (transaction == null || transaction.getAmount() == null || transaction.getTaxRate() == null) {
            return RuleEvaluationResult.pass();
        }

        BigDecimal slabThreshold = DEFAULT_SLAB_THRESHOLD;
        BigDecimal requiredTaxRate = DEFAULT_REQUIRED_TAX_RATE;

        // Parse custom slab thresholds and rates from JSON configuration
        if (configJson != null && !configJson.trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(configJson);

                if (root.has("slabThreshold") && !root.get("slabThreshold").isNull()) {
                    slabThreshold = new BigDecimal(root.get("slabThreshold").asText());
                }
                if (root.has("requiredTaxRate") && !root.get("requiredTaxRate").isNull()) {
                    requiredTaxRate = normalizeTaxRate(new BigDecimal(root.get("requiredTaxRate").asText()));
                }

                if (root.has("slabs") && root.get("slabs").isArray()) {
                    for (JsonNode slabNode : root.get("slabs")) {
                        BigDecimal minAmount = new BigDecimal(slabNode.get("minAmount").asText());
                        BigDecimal slabMinRate = normalizeTaxRate(new BigDecimal(slabNode.get("requiredTaxRate").asText()));
                        if (transaction.getAmount().compareTo(minAmount) >= 0) {
                            slabThreshold = minAmount;
                            requiredTaxRate = slabMinRate;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse configJson for rule {}: {}. Using default values", RULE_NAME, configJson);
            }
        }

        BigDecimal normalizedTxTaxRate = normalizeTaxRate(transaction.getTaxRate());

        if (transaction.getAmount().compareTo(slabThreshold) > 0 && normalizedTxTaxRate.compareTo(requiredTaxRate) < 0) {
            Severity severity = defaultSeverity != null ? defaultSeverity : Severity.MEDIUM;
            String message = String.format(
                    "Transaction amount %s exceeds slab threshold %s, requiring tax rate of at least %s, but provided rate is %s",
                    transaction.getAmount(), slabThreshold, requiredTaxRate, normalizedTxTaxRate);
            return RuleEvaluationResult.fail(severity, message);
        }

        return RuleEvaluationResult.pass();
    }

    private BigDecimal normalizeTaxRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        }
        return rate;
    }
}
