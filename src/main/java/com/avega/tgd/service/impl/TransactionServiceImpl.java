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
 * Core implementation of {@link TransactionService} coordinating the transaction lifecycle:
 *
 * <p>Architecture &amp; Design Decisions:
 * <ul>
 *   <li><b>Fault Isolation:</b> Batches are processed on a per-transaction basis. If record #3 fails syntax
 *       validation, it is recorded as a FAILURE with diagnostics, while records #1, #2, and #4 complete normally.</li>
 *   <li><b>Audit Compliance:</b> The raw request body is retained in {@code raw_payload} to provide an immutable
 *       forensic audit trail should an auditor need to inspect what the customer originally sent.</li>
 *   <li><b>Sequential Rule Execution:</b> The transaction and its tax gap calculations are flushed to the
 *       database before running compliance rules, ensuring dependent rules (such as refund validation) can query
 *       and inspect existing transaction records.</li>
 * </ul>
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

    /**
     * Ingests, validates, computes tax gap, evaluates active rules, logs audit events,
     * and persists the transaction entity.
     *
     * @param dto Incoming transaction request payload
     * @return Response DTO with full validation and calculation details
     */
    @Override
    @Transactional
    public TransactionResponseDto processTransaction(TransactionRequestDto dto) {
        // Step 1: Capture the exact verbatim raw payload for non-repudiation and forensic audit
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            rawJson = String.valueOf(dto);
        }

        // Step 2: Validate structural syntax and required business fields
        List<String> validationErrors = validateTransactionDto(dto);
        boolean isValid = validationErrors.isEmpty();

        // Fallback: If transactionId was omitted in an invalid request, generate a synthetic ID
        // to prevent database UNIQUE constraint violations while still persisting the failed attempt.
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
            // Validation failed: persist failed transaction record along with specific failure reasons
            String reasons = String.join("; ", validationErrors);
            entity.setValidationStatus(ValidationStatus.FAILURE);
            entity.setFailureReasons(reasons);
            entity.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);

            TransactionEntity savedFailed = transactionRepository.save(entity);

            // Audit log: record failed ingestion milestone with full diagnostic errors
            Map<String, Object> ingestionAudit = new HashMap<>();
            ingestionAudit.put("status", "VALIDATION_FAILURE");
            ingestionAudit.put("errors", validationErrors);
            ingestionAudit.put("raw", rawJson);
            auditLogService.logEvent(AuditEventType.INGESTION, txId, ingestionAudit);

            // Exception record: create an auditor-visible exception record for the syntax failure
            exceptionService.createException(
                    txId,
                    dto.getCustomerId() != null ? dto.getCustomerId() : "UNKNOWN",
                    "SCHEMA_VALIDATION_FAILURE",
                    Severity.HIGH,
                    "Transaction validation failed: " + reasons
            );

            return TransactionResponseDto.fromEntity(savedFailed);
        }

        // Validation passed: mark transaction as successful
        entity.setValidationStatus(ValidationStatus.SUCCESS);
        entity.setFailureReasons(null);

        // Audit log: record successful ingestion milestone
        Map<String, Object> ingestionAudit = new HashMap<>();
        ingestionAudit.put("status", "SUCCESS");
        ingestionAudit.put("amount", dto.getAmount());
        ingestionAudit.put("taxRate", dto.getTaxRate());
        ingestionAudit.put("reportedTax", dto.getReportedTax());
        auditLogService.logEvent(AuditEventType.INGESTION, txId, ingestionAudit);

        // Step 3: Compute expected tax, tax gap, and compliance classification
        TaxGapResult taxGapResult = taxGapEngineService.evaluateTransaction(entity);

        Map<String, Object> taxAudit = new HashMap<>();
        taxAudit.put("expectedTax", taxGapResult.getExpectedTax());
        taxAudit.put("reportedTax", dto.getReportedTax());
        taxAudit.put("taxGap", taxGapResult.getTaxGap());
        taxAudit.put("complianceStatus", taxGapResult.getComplianceStatus());
        taxAudit.put("notes", taxGapResult.getNotes());
        auditLogService.logEvent(AuditEventType.TAX_COMPUTATION, txId, taxAudit);

        // If compliance indicates underpayment or missing tax, generate an audit exception
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

        // Persist transaction entity before executing rules so that subsequent rules
        // (such as RefundValidationRule looking up sales history) can discover this or related records.
        TransactionEntity savedTx = transactionRepository.save(entity);

        // Step 4: Execute all enabled compliance rules from the database
        ruleEngineService.executeRules(savedTx);

        return TransactionResponseDto.fromEntity(savedTx);
    }

    /**
     * Iterates over batch transactions, isolating errors so invalid records do not fail the batch.
     *
     * @param transactionRequests List of transaction request objects
     * @return Aggregated response summarizing total, successful, and failed counts
     */
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
                // Defensive catch-all to prevent unhandled runtime exceptions from crashing the batch
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

    /**
     * Fetches a transaction entity by its unique business transaction ID.
     *
     * @param transactionId Unique business transaction ID
     * @return The transaction entity
     * @throws ResourceNotFoundException if no transaction exists with the given ID
     */
    @Override
    @Transactional(readOnly = true)
    public TransactionEntity getByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
    }

    /**
     * Retrieves transactions with pagination and sorting.
     *
     * @param pageable Pagination and sorting criteria
     * @return Paged DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(TransactionResponseDto::fromEntity);
    }

    /**
     * Enforces domain validation rules:
     * <ul>
     *   <li>{@code transactionId} must not be blank.</li>
     *   <li>{@code date} must be present and parseable.</li>
     *   <li>{@code customerId} must not be blank.</li>
     *   <li>{@code amount} must be strictly greater than zero.</li>
     *   <li>{@code taxRate} must be non-negative.</li>
     *   <li>{@code transactionType} must be one of SALE, REFUND, or EXPENSE.</li>
     * </ul>
     *
     * @param dto Transaction DTO to validate
     * @return List of error message strings (empty if valid)
     */
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
