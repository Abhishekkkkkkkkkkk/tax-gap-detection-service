package com.avega.tgd.service;

import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.dto.response.CustomerTaxSummaryDto;
import com.avega.tgd.dto.response.ExceptionSummaryDto;
import com.avega.tgd.repository.ExceptionRepository;
import com.avega.tgd.repository.TransactionRepository;
import com.avega.tgd.repository.projection.CustomerExceptionCountProjection;
import com.avega.tgd.repository.projection.CustomerTaxSummaryProjection;
import com.avega.tgd.repository.projection.SeverityCountProjection;
import com.avega.tgd.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Report Service Unit Tests")
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExceptionRepository exceptionRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(transactionRepository, exceptionRepository);
    }

    @Test
    @DisplayName("Compliance score calculation formula: 100 - (nonCompliant / total * 100)")
    void testComplianceScoreFormula() {
        // 10 total, 2 non-compliant -> score = 100 - (2/10 * 100) = 80.00
        BigDecimal score = CustomerTaxSummaryDto.calculateComplianceScore(10, 2);
        assertEquals(new BigDecimal("80.00"), score);

        // 10 total, 0 non-compliant -> score = 100.00
        BigDecimal perfectScore = CustomerTaxSummaryDto.calculateComplianceScore(10, 0);
        assertEquals(new BigDecimal("100.00"), perfectScore);

        // 10 total, 10 non-compliant -> score = 0.00
        BigDecimal zeroScore = CustomerTaxSummaryDto.calculateComplianceScore(10, 10);
        assertEquals(new BigDecimal("0.00"), zeroScore);

        // 0 total -> score = 100.00
        BigDecimal emptyScore = CustomerTaxSummaryDto.calculateComplianceScore(0, 0);
        assertEquals(new BigDecimal("100.00"), emptyScore);
    }

    @Test
    @DisplayName("getCustomerTaxSummary should map projection into DTO correctly")
    void testGetCustomerTaxSummary() {
        CustomerTaxSummaryProjection mockProjection = mock(CustomerTaxSummaryProjection.class);
        when(mockProjection.getCustomerId()).thenReturn("CUST-001");
        when(mockProjection.getTotalTransactions()).thenReturn(5L);
        when(mockProjection.getTotalAmount()).thenReturn(new BigDecimal("50000.00"));
        when(mockProjection.getTotalReportedTax()).thenReturn(new BigDecimal("9000.00"));
        when(mockProjection.getTotalExpectedTax()).thenReturn(new BigDecimal("9000.00"));
        when(mockProjection.getTotalTaxGap()).thenReturn(BigDecimal.ZERO);
        when(mockProjection.getCompliantCount()).thenReturn(4L);
        when(mockProjection.getNonCompliantCount()).thenReturn(1L);

        when(transactionRepository.getCustomerTaxSummaryByCustomerId("CUST-001"))
                .thenReturn(Optional.of(mockProjection));

        CustomerTaxSummaryDto summary = reportService.getCustomerTaxSummary("CUST-001");

        assertNotNull(summary);
        assertEquals("CUST-001", summary.getCustomerId());
        assertEquals(5L, summary.getTotalTransactions());
        assertEquals(new BigDecimal("50000.00"), summary.getTotalAmount());
        // 5 total, 1 non-compliant -> 100 - (1/5 * 100) = 80.00
        assertEquals(new BigDecimal("80.00"), summary.getComplianceScore());
    }

    @Test
    @DisplayName("getExceptionSummary should aggregate total count, severity breakdown, and customer breakdown")
    void testGetExceptionSummary() {
        when(exceptionRepository.count()).thenReturn(15L);

        SeverityCountProjection highSev = mock(SeverityCountProjection.class);
        when(highSev.getSeverity()).thenReturn(Severity.HIGH);
        when(highSev.getCount()).thenReturn(10L);

        SeverityCountProjection medSev = mock(SeverityCountProjection.class);
        when(medSev.getSeverity()).thenReturn(Severity.MEDIUM);
        when(medSev.getCount()).thenReturn(5L);

        when(exceptionRepository.countGroupedBySeverity()).thenReturn(List.of(highSev, medSev));

        CustomerExceptionCountProjection cust1 = mock(CustomerExceptionCountProjection.class);
        when(cust1.getCustomerId()).thenReturn("CUST-001");
        when(cust1.getCount()).thenReturn(8L);

        when(exceptionRepository.countGroupedByCustomer()).thenReturn(List.of(cust1));

        ExceptionSummaryDto summary = reportService.getExceptionSummary();

        assertNotNull(summary);
        assertEquals(15L, summary.getTotalExceptions());
        assertEquals(10L, summary.getCountBySeverity().get(Severity.HIGH));
        assertEquals(5L, summary.getCountBySeverity().get(Severity.MEDIUM));
        assertEquals(0L, summary.getCountBySeverity().get(Severity.LOW));
        assertEquals(8L, summary.getCustomerWiseExceptionCount().get("CUST-001"));
    }
}
