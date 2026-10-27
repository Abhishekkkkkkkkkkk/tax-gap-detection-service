package com.avega.tgd.service.rules;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.service.rules.impl.HighValueTransactionRuleEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("High-Value Transaction Rule Unit Tests")
class HighValueTransactionRuleTest {

    private HighValueTransactionRuleEvaluator ruleEvaluator;

    @BeforeEach
    void setUp() {
        ruleEvaluator = new HighValueTransactionRuleEvaluator(new ObjectMapper());
    }

    @Test
    @DisplayName("Transaction below default threshold (100,000) should pass")
    void testAmountBelowThresholdPasses() {
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("99999.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, null, Severity.HIGH);

        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("Transaction exactly at threshold should pass")
    void testAmountAtThresholdPasses() {
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("100000.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, null, Severity.HIGH);

        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("Transaction above threshold should fail with configured severity")
    void testAmountAboveThresholdFails() {
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("100001.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, null, Severity.HIGH);

        assertFalse(result.isPassed());
        assertEquals(Severity.HIGH, result.getSeverity());
        assertTrue(result.getMessage().contains("exceeds high-value threshold"));
    }

    @Test
    @DisplayName("Custom threshold from config JSON should be respected")
    void testCustomThresholdFromConfig() {
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("50001.00"));

        String customConfig = "{\"threshold\": 50000.00}";
        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, customConfig, Severity.HIGH);

        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("50000.00"));
    }

    @Test
    @DisplayName("Malformed config JSON should fallback gracefully to default threshold")
    void testMalformedConfigFallback() {
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("150000.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, "{invalid json}", Severity.HIGH);

        assertFalse(result.isPassed());
    }
}
