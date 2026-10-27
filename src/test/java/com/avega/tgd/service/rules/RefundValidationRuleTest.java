package com.avega.tgd.service.rules;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.domain.enums.TransactionType;
import com.avega.tgd.repository.TransactionRepository;
import com.avega.tgd.service.rules.impl.RefundValidationRuleEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Refund Validation Rule Unit Tests")
class RefundValidationRuleTest {

    @Mock
    private TransactionRepository transactionRepository;

    private RefundValidationRuleEvaluator ruleEvaluator;

    @BeforeEach
    void setUp() {
        ruleEvaluator = new RefundValidationRuleEvaluator(transactionRepository);
    }

    @Test
    @DisplayName("Non-refund transaction should automatically pass")
    void testNonRefundPasses() {
        TransactionEntity tx = new TransactionEntity();
        tx.setTransactionType(TransactionType.SALE);
        tx.setAmount(new BigDecimal("1000.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(tx, null, Severity.HIGH);

        assertTrue(result.isPassed());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("Refund with linked sale where refund <= sale amount should pass")
    void testRefundLessThanSaleAmountPasses() {
        TransactionEntity originalSale = new TransactionEntity();
        originalSale.setTransactionId("TX-SALE-001");
        originalSale.setTransactionType(TransactionType.SALE);
        originalSale.setAmount(new BigDecimal("5000.00"));

        when(transactionRepository.findFirstByTransactionIdAndTransactionType("TX-SALE-001", TransactionType.SALE))
                .thenReturn(Optional.of(originalSale));

        TransactionEntity refundTx = new TransactionEntity();
        refundTx.setTransactionType(TransactionType.REFUND);
        refundTx.setOriginalTransactionId("TX-SALE-001");
        refundTx.setAmount(new BigDecimal("2000.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(refundTx, null, Severity.HIGH);

        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("Refund exceeding linked sale amount should fail with HIGH severity")
    void testRefundExceedingSaleAmountFails() {
        TransactionEntity originalSale = new TransactionEntity();
        originalSale.setTransactionId("TX-SALE-001");
        originalSale.setTransactionType(TransactionType.SALE);
        originalSale.setAmount(new BigDecimal("3000.00"));

        when(transactionRepository.findFirstByTransactionIdAndTransactionType("TX-SALE-001", TransactionType.SALE))
                .thenReturn(Optional.of(originalSale));

        TransactionEntity refundTx = new TransactionEntity();
        refundTx.setTransactionType(TransactionType.REFUND);
        refundTx.setOriginalTransactionId("TX-SALE-001");
        refundTx.setAmount(new BigDecimal("3500.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(refundTx, null, Severity.HIGH);

        assertFalse(result.isPassed());
        assertEquals(Severity.HIGH, result.getSeverity());
        assertTrue(result.getMessage().contains("exceeds original sale amount"));
    }

    @Test
    @DisplayName("Refund with unknown linked sale transaction should fail")
    void testRefundLinkedSaleNotFoundFails() {
        when(transactionRepository.findFirstByTransactionIdAndTransactionType("NON-EXISTENT", TransactionType.SALE))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByTransactionId("NON-EXISTENT"))
                .thenReturn(Optional.empty());

        TransactionEntity refundTx = new TransactionEntity();
        refundTx.setTransactionType(TransactionType.REFUND);
        refundTx.setOriginalTransactionId("NON-EXISTENT");
        refundTx.setAmount(new BigDecimal("1000.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(refundTx, null, Severity.HIGH);

        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("was not found"));
    }

    @Test
    @DisplayName("Unlinked refund with customer having sufficient sale should pass")
    void testUnlinkedRefundWithSufficientCustomerSalePasses() {
        TransactionEntity highestSale = new TransactionEntity();
        highestSale.setTransactionId("TX-SALE-002");
        highestSale.setAmount(new BigDecimal("10000.00"));

        when(transactionRepository.findTopByCustomerIdAndTransactionTypeOrderByAmountDesc("CUST-100", TransactionType.SALE))
                .thenReturn(Optional.of(highestSale));

        TransactionEntity refundTx = new TransactionEntity();
        refundTx.setCustomerId("CUST-100");
        refundTx.setTransactionType(TransactionType.REFUND);
        refundTx.setAmount(new BigDecimal("4000.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(refundTx, null, Severity.HIGH);

        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("Unlinked refund with no previous customer sale should fail")
    void testUnlinkedRefundWithNoSaleFails() {
        when(transactionRepository.findTopByCustomerIdAndTransactionTypeOrderByAmountDesc("CUST-NEW", TransactionType.SALE))
                .thenReturn(Optional.empty());

        TransactionEntity refundTx = new TransactionEntity();
        refundTx.setCustomerId("CUST-NEW");
        refundTx.setTransactionType(TransactionType.REFUND);
        refundTx.setAmount(new BigDecimal("500.00"));

        RuleEvaluationResult result = ruleEvaluator.evaluate(refundTx, null, Severity.HIGH);

        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("No previous SALE found for customer"));
    }
}
