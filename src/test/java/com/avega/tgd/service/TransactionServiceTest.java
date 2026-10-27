package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import com.avega.tgd.domain.enums.ComplianceStatus;
import com.avega.tgd.domain.enums.TransactionType;
import com.avega.tgd.domain.enums.ValidationStatus;
import com.avega.tgd.dto.request.TransactionRequestDto;
import com.avega.tgd.dto.response.BatchUploadResponseDto;
import com.avega.tgd.dto.response.TransactionResponseDto;
import com.avega.tgd.repository.TransactionRepository;
import com.avega.tgd.service.dto.TaxGapResult;
import com.avega.tgd.service.impl.TransactionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TaxGapEngineService taxGapEngineService;

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private ExceptionService exceptionService;

    @Mock
    private AuditLogService auditLogService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(
                transactionRepository,
                taxGapEngineService,
                ruleEngineService,
                exceptionService,
                auditLogService,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("Valid transaction should be processed, evaluated for tax gap and rules, and saved as SUCCESS")
    void testProcessValidTransaction() {
        TransactionRequestDto dto = new TransactionRequestDto(
                "TX-100",
                LocalDate.of(2026, 10, 1),
                "CUST-001",
                new BigDecimal("1000.00"),
                new BigDecimal("0.18"),
                new BigDecimal("180.00"),
                TransactionType.SALE,
                null
        );

        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(taxGapEngineService.evaluateTransaction(any(TransactionEntity.class)))
                .thenReturn(new TaxGapResult(new BigDecimal("180.00"), BigDecimal.ZERO, ComplianceStatus.COMPLIANT, "OK"));

        TransactionResponseDto response = transactionService.processTransaction(dto);

        assertNotNull(response);
        assertEquals(ValidationStatus.SUCCESS, response.getValidationStatus());
        assertEquals("TX-100", response.getTransactionId());

        verify(auditLogService).logEvent(eq(AuditEventType.INGESTION), eq("TX-100"), any());
        verify(taxGapEngineService).evaluateTransaction(any(TransactionEntity.class));
        verify(auditLogService).logEvent(eq(AuditEventType.TAX_COMPUTATION), eq("TX-100"), any());
        verify(ruleEngineService).executeRules(any(TransactionEntity.class));
        verify(transactionRepository, atLeastOnce()).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Invalid transaction (amount <= 0, missing date) should fail validation and log failure")
    void testProcessInvalidTransactionFailsValidation() {
        TransactionRequestDto dto = new TransactionRequestDto();
        dto.setTransactionId("TX-INVALID");
        dto.setCustomerId("CUST-001");
        dto.setAmount(new BigDecimal("-50.00")); // Invalid: <= 0
        dto.setDate(null); // Invalid: missing date
        dto.setTaxRate(new BigDecimal("0.18"));
        dto.setTransactionType(TransactionType.SALE);

        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponseDto response = transactionService.processTransaction(dto);

        assertNotNull(response);
        assertEquals(ValidationStatus.FAILURE, response.getValidationStatus());
        assertTrue(response.getFailureReasons().contains("amount must be > 0"));
        assertTrue(response.getFailureReasons().contains("date is required"));

        verify(auditLogService).logEvent(eq(AuditEventType.INGESTION), eq("TX-INVALID"), any());
        verify(exceptionService).createException(eq("TX-INVALID"), eq("CUST-001"), eq("SCHEMA_VALIDATION_FAILURE"), any(), any());
        verifyNoInteractions(taxGapEngineService);
        verifyNoInteractions(ruleEngineService);
    }

    @Test
    @DisplayName("Batch upload should process list and report correct success/failed counts")
    void testProcessBatch() {
        TransactionRequestDto valid = new TransactionRequestDto(
                "TX-V1", LocalDate.now(), "CUST-1", new BigDecimal("100"), new BigDecimal("0.18"),
                new BigDecimal("18"), TransactionType.SALE, null);

        TransactionRequestDto invalid = new TransactionRequestDto(
                "TX-INV", null, null, BigDecimal.ZERO, null, null, null, null);

        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(taxGapEngineService.evaluateTransaction(any(TransactionEntity.class)))
                .thenReturn(new TaxGapResult(new BigDecimal("18.00"), BigDecimal.ZERO, ComplianceStatus.COMPLIANT, "OK"));

        BatchUploadResponseDto batchResponse = transactionService.processBatch(List.of(valid, invalid));

        assertEquals(2, batchResponse.getTotalReceived());
        assertEquals(1, batchResponse.getTotalSuccess());
        assertEquals(1, batchResponse.getTotalFailed());
    }
}
