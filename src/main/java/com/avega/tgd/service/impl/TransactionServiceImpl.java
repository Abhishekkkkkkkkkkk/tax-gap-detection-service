package com.avega.tgd.service.impl;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import com.avega.tgd.domain.enums.ComplianceStatus;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.domain.enums.ValidationStatus;
import com.avega.tgd.dto.request.TransactionRequestDto;
import com.avega.tgd.dto.response.BatchUploadResponseDto;
import com.avega.tgd.dto.response.TransactionResponseDto;
import com.avega.tgd.exception.ResourceNotFoundException;
import com.avega.tgd.repository.TransactionRepository;
import com.avega.tgd.service.AuditLogService;
import com.avega.tgd.service.ExceptionService;
import com.avega.tgd.service.RuleEngineService;
import com.avega.tgd.service.TaxGapEngineService;
import com.avega.tgd.service.TransactionService;
import com.avega.tgd.service.dto.TaxGapResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core implementation of {@link TransactionService} managing ingestion, validation,
 * tax calculation, rule evaluation, and audit logging.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final TaxGapEngineService taxGapEngineService;
    private final RuleEngineService ruleEngineService;
    private final ExceptionService exceptionService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  TaxGapEngineService taxGapEngineService,
                                  RuleEngineService ruleEngineService,
                                  ExceptionService exceptionService,
                                  AuditLogService auditLogService,
                                  ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.taxGapEngineService = taxGapEngineService;
        this.ruleEngineService = ruleEngineService;
        this.exceptionService = exceptionService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TransactionResponseDto processTransaction(TransactionRequestDto dto) {
        // Capture verbatim payload for non-repudiation and forensic audit
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            rawJson = String.valueOf(dto);
        }

        List<String> validationErrors = validateTransactionDto(dto);
        boolean isValid = validationErrors.isEmpty();

        // If transactionId is missing, generate synthetic ID to avoid unique constraint violations while storing the failure
        String txId = dto.getTransactionId();
        if (txId == null || txId.trim().isEmpty()) {
            txId = "UNASSIGNED-" + UUID.randomUUID();
        }

        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId(txId);
        entity.setDate(dto.getDate());
        entity.setCustomerId(dto.getCustomerId());
        entity.setAmount(dto.getAmount());
        entity.setTaxRate(dto.getTaxRate());
        entity.setReportedTax(dto.getReportedTax());
        entity.setTransactionType(dto.getTransactionType());
        entity.setOriginalTransactionId(dto.getOriginalTransactionId());
        entity.setRawPayload(rawJson);

        if (!isValid) {
            String reasons = String.join("; ", validationErrors);
            entity.setValidationStatus(ValidationStatus.FAILURE);
            entity.setFailureReasons(reasons);
            entity.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);

            TransactionEntity savedFailed = transactionRepository.save(entity);

            Map<String, Object> ingestionAudit = new HashMap<>();
            ingestionAudit.put("status", "VALIDATION_FAILURE");
            ingestionAudit.put("errors", validationErrors);
            ingestionAudit.put("raw", rawJson);
            auditLogService.logEvent(AuditEventType.INGESTION, txId, ingestionAudit);

            exceptionService.createException(
                    txId,
                    dto.getCustomerId() != null ? dto.getCustomerId() : "UNKNOWN",
                    "SCHEMA_VALIDATION_FAILURE",
                    Severity.HIGH,
                    "Transaction validation failed: " + reasons
            );

            return TransactionResponseDto.fromEntity(savedFailed);
        }

        entity.setValidationStatus(ValidationStatus.SUCCESS);
        entity.setFailureReasons(null);

        Map<String, Object> ingestionAudit = new HashMap<>();
        ingestionAudit.put("status", "SUCCESS");
        ingestionAudit.put("amount", dto.getAmount());
        ingestionAudit.put("taxRate", dto.getTaxRate());
        ingestionAudit.put("reportedTax", dto.getReportedTax());
        auditLogService.logEvent(AuditEventType.INGESTION, txId, ingestionAudit);

        TaxGapResult taxGapResult = taxGapEngineService.evaluateTransaction(entity);

        Map<String, Object> taxAudit = new HashMap<>();
        taxAudit.put("expectedTax", taxGapResult.getExpectedTax());
        taxAudit.put("reportedTax", dto.getReportedTax());
        taxAudit.put("taxGap", taxGapResult.getTaxGap());
        taxAudit.put("complianceStatus", taxGapResult.getComplianceStatus());
        taxAudit.put("notes", taxGapResult.getNotes());
        auditLogService.logEvent(AuditEventType.TAX_COMPUTATION, txId, taxAudit);

        if (taxGapResult.getComplianceStatus() == ComplianceStatus.UNDERPAID) {
            exceptionService.createException(
                    txId,
                    dto.getCustomerId(),
                    "TAX_GAP_UNDERPAID",
                    Severity.HIGH,
                    String.format("Tax underpayment detected: Expected %s, Reported %s (Gap: %s)",
                            taxGapResult.getExpectedTax(), dto.getReportedTax(), taxGapResult.getTaxGap())
            );
        } else if (taxGapResult.getComplianceStatus() == ComplianceStatus.NON_COMPLIANT) {
            exceptionService.createException(
                    txId,
                    dto.getCustomerId(),
                    "TAX_REPORTED_MISSING",
                    Severity.HIGH,
                    "Reported tax is missing for transaction: expected " + taxGapResult.getExpectedTax()
            );
        }

        // Persist entity before running rules so rules (e.g. refund lookups) can query it
        TransactionEntity savedTx = transactionRepository.save(entity);

        ruleEngineService.executeRules(savedTx);

        return TransactionResponseDto.fromEntity(savedTx);
    }

    @Override
    @Transactional
    public BatchUploadResponseDto processBatch(List<TransactionRequestDto> transactionRequests) {
        if (transactionRequests == null) {
            return new BatchUploadResponseDto(0, 0, 0, new ArrayList<>());
        }

        int totalReceived = transactionRequests.size();
        int totalSuccess = 0;
        int totalFailed = 0;
        List<TransactionResponseDto> results = new ArrayList<>();

        for (TransactionRequestDto request : transactionRequests) {
            try {
                TransactionResponseDto response = processTransaction(request);
                results.add(response);
                if (response.getValidationStatus() == ValidationStatus.SUCCESS) {
                    totalSuccess++;
                } else {
                    totalFailed++;
                }
            } catch (Exception e) {
                // Prevent single transaction errors from crashing batch processing
                log.error("Unexpected error processing transaction {}: ", request.getTransactionId(), e);
                totalFailed++;
                TransactionResponseDto errorDto = new TransactionResponseDto();
                errorDto.setTransactionId(request.getTransactionId());
                errorDto.setValidationStatus(ValidationStatus.FAILURE);
                errorDto.setFailureReasons("Processing exception: " + e.getMessage());
                results.add(errorDto);
            }
        }

        return new BatchUploadResponseDto(totalReceived, totalSuccess, totalFailed, results);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionEntity getByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(TransactionResponseDto::fromEntity);
    }

    private List<String> validateTransactionDto(TransactionRequestDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto.getTransactionId() == null || dto.getTransactionId().trim().isEmpty()) {
            errors.add("transactionId is required");
        }
        if (dto.getDate() == null) {
            errors.add("date is required and must follow valid format (YYYY-MM-DD)");
        }
        if (dto.getCustomerId() == null || dto.getCustomerId().trim().isEmpty()) {
            errors.add("customerId is required");
        }
        if (dto.getAmount() == null) {
            errors.add("amount is required");
        } else if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("amount must be > 0");
        }
        if (dto.getTaxRate() == null) {
            errors.add("taxRate is required");
        } else if (dto.getTaxRate().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("taxRate cannot be negative");
        }
        if (dto.getTransactionType() == null) {
            errors.add("transactionType is required (SALE, REFUND, or EXPENSE)");
        }

        return errors;
    }
}
