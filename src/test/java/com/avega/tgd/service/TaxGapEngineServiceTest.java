package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.ComplianceStatus;
import com.avega.tgd.service.dto.TaxGapResult;
import com.avega.tgd.service.impl.TaxGapEngineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tax Gap Calculation Engine Unit Tests")
class TaxGapEngineServiceTest {

    private TaxGapEngineService taxGapEngineService;

    @BeforeEach
    void setUp() {
        taxGapEngineService = new TaxGapEngineServiceImpl();
    }

    @Test
    @DisplayName("Exact tax match should yield taxGap = 0 and status COMPLIANT")
    void testExactTaxMatch() {
        // amount: 1000.00, taxRate: 0.18 -> expectedTax = 180.00, reportedTax = 180.00
        TaxGapResult result = taxGapEngineService.computeTaxGap(
                new BigDecimal("1000.00"),
                new BigDecimal("0.18"),
                new BigDecimal("180.00")
        );

        assertEquals(new BigDecimal("180.00"), result.getExpectedTax());
        assertEquals(new BigDecimal("0.00"), result.getTaxGap());
        assertEquals(ComplianceStatus.COMPLIANT, result.getComplianceStatus());
    }

    @Test
    @DisplayName("Tax gap within +/- 1.00 tolerance should be COMPLIANT")
    void testTaxGapWithinTolerance() {
        // expected: 180.00, reported: 179.50 -> gap = 0.50 (<= 1.00)
        TaxGapResult result1 = taxGapEngineService.computeTaxGap(
                new BigDecimal("1000.00"),
                new BigDecimal("0.18"),
                new BigDecimal("179.50")
        );
        assertEquals(ComplianceStatus.COMPLIANT, result1.getComplianceStatus());
        assertEquals(new BigDecimal("0.50"), result1.getTaxGap());

        // expected: 180.00, reported: 180.80 -> gap = -0.80 (>= -1.00)
        TaxGapResult result2 = taxGapEngineService.computeTaxGap(
                new BigDecimal("1000.00"),
                new BigDecimal("0.18"),
                new BigDecimal("180.80")
        );
        assertEquals(ComplianceStatus.COMPLIANT, result2.getComplianceStatus());
        assertEquals(new BigDecimal("-0.80"), result2.getTaxGap());
    }

    @Test
    @DisplayName("Tax gap > 1.00 should be marked as UNDERPAID")
    void testUnderpaidTax() {
        // expected: 180.00, reported: 100.00 -> gap = 80.00 (> 1)
        TaxGapResult result = taxGapEngineService.computeTaxGap(
                new BigDecimal("1000.00"),
                new BigDecimal("0.18"),
                new BigDecimal("100.00")
        );

        assertEquals(new BigDecimal("180.00"), result.getExpectedTax());
        assertEquals(new BigDecimal("80.00"), result.getTaxGap());
        assertEquals(ComplianceStatus.UNDERPAID, result.getComplianceStatus());
    }

    @Test
    @DisplayName("Tax gap < -1.00 should be marked as OVERPAID")
    void testOverpaidTax() {
        // expected: 180.00, reported: 250.00 -> gap = -70.00 (< -1)
        TaxGapResult result = taxGapEngineService.computeTaxGap(
                new BigDecimal("1000.00"),
                new BigDecimal("0.18"),
                new BigDecimal("250.00")
        );

        assertEquals(new BigDecimal("180.00"), result.getExpectedTax());
        assertEquals(new BigDecimal("-70.00"), result.getTaxGap());
        assertEquals(ComplianceStatus.OVERPAID, result.getComplianceStatus());
    }

    @Test
    @DisplayName("Missing reported tax should result in NON_COMPLIANT status")
    void testMissingReportedTax() {
        TaxGapResult result = taxGapEngineService.computeTaxGap(
                new BigDecimal("500.00"),
                new BigDecimal("0.18"),
                null
        );

        assertEquals(new BigDecimal("90.00"), result.getExpectedTax());
        assertEquals(ComplianceStatus.NON_COMPLIANT, result.getComplianceStatus());
    }

    @Test
    @DisplayName("Percentage rates supplied as whole numbers (e.g. 18.0) should be normalized")
    void testNormalizedPercentageTaxRate() {
        TaxGapResult result = taxGapEngineService.computeTaxGap(
                new BigDecimal("1000.00"),
                new BigDecimal("18.0"),
                new BigDecimal("180.00")
        );

        assertEquals(new BigDecimal("180.00"), result.getExpectedTax());
        assertEquals(ComplianceStatus.COMPLIANT, result.getComplianceStatus());
    }

    @Test
    @DisplayName("evaluateTransaction should update transaction entity fields")
    void testEvaluateTransactionEntity() {
        TransactionEntity entity = new TransactionEntity();
        entity.setAmount(new BigDecimal("2000.00"));
        entity.setTaxRate(new BigDecimal("0.18"));
        entity.setReportedTax(new BigDecimal("300.00"));

        TaxGapResult result = taxGapEngineService.evaluateTransaction(entity);

        assertEquals(new BigDecimal("360.00"), entity.getExpectedTax());
        assertEquals(new BigDecimal("60.00"), entity.getTaxGap());
        assertEquals(ComplianceStatus.UNDERPAID, entity.getComplianceStatus());
        assertEquals(ComplianceStatus.UNDERPAID, result.getComplianceStatus());
    }

    @Test
    @DisplayName("Null amount or tax rate should throw IllegalArgumentException")
    void testNullInputsThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                taxGapEngineService.computeTaxGap(null, new BigDecimal("0.18"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                taxGapEngineService.computeTaxGap(new BigDecimal("100"), null, BigDecimal.ZERO));
    }
}
