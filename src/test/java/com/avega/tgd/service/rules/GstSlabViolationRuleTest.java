package com.avega.tgd.service.rules;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.service.rules.impl.GstSlabViolationRuleEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GST Slab Violation Rule Unit Tests")
class GstSlabViolationRuleTest {

    private GstSlabViolationRuleEvaluator ruleEvaluator;

    @BeforeEach
    void setUp() {
        ruleEvaluator = new GstSlabViolationRuleEvaluator(new ObjectMapper());
    }

    @Test
    @DisplayName("Amount below slab threshold (50,000) should pass even with low tax rate")
    void testAmountBelowSlabThresholdPasses() {
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("30000.00"));
        tx.setTaxRate(new BigDecimal("0.05")); // 5% tax

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, null, Severity.MEDIUM);

        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("Amount above slab threshold with compliant tax rate (>= 18%) should pass")
    void testAmountAboveSlabWithCompliantRatePasses() {
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("75000.00"));
        tx.setTaxRate(new BigDecimal("0.18"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, null, Severity.MEDIUM);

        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("Amount above slab threshold with lower tax rate (< 18%) should fail")
    void testAmountAboveSlabWithLowerRateFails() {
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("75000.00"));
        tx.setTaxRate(new BigDecimal("0.12")); // 12% is lower than required 18%

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, null, Severity.MEDIUM);

        assertFalse(result.isPassed());
        assertEquals(Severity.MEDIUM, result.getSeverity());
        assertTrue(result.getMessage().contains("exceeds slab threshold"));
    }

    @Test
    @DisplayName("Tax rate provided as percentage 12.0 should be detected as violation (< 18%)")
    void testPercentageInputSlabViolation() {
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("60000.00"));
        tx.setTaxRate(new BigDecimal("12.0"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, null, Severity.MEDIUM);

        assertFalse(result.isPassed());
    }

    @Test
    @DisplayName("Custom slab config via JSON should be evaluated correctly")
    void testCustomSlabConfiguration() {
        String customConfig = "{\"slabThreshold\": 25000.00, \"requiredTaxRate\": 0.28}";
        TransactionEntity tx = new TransactionEntity();
        tx.setAmount(new BigDecimal("30000.00"));
        tx.setTaxRate(new BigDecimal("0.18")); // Below required 28%

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, customConfig, Severity.HIGH);

        assertFalse(result.isPassed());
        assertEquals(Severity.HIGH, result.getSeverity());
    }
}
