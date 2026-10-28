package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.dto.request.TransactionRequestDto;
import com.avega.tgd.dto.response.BatchUploadResponseDto;
import com.avega.tgd.dto.response.TransactionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface governing the financial transaction ingestion pipeline.
 *
 * <p>Pipeline Sequence:
 * <ol>
 *   <li><b>Syntax &amp; Structural Validation:</b> Validates required fields, date presence, positive amount, and valid transaction type.</li>
 *   <li><b>Raw Payload Archival:</b> Retains verbatim JSON request body in the database regardless of validation status.</li>
 *   <li><b>Failure Handling:</b> Malformed records are persisted with {@code FAILURE} status, audited, and logged as exceptions without aborting valid sibling transactions in the batch.</li>
 *   <li><b>Tax Gap Evaluation:</b> Valid transactions are processed via {@link TaxGapEngineService} for gap computation and compliance assignment.</li>
 *   <li><b>Rule Engine Invocation:</b> Active database-stored compliance rules are evaluated via {@link RuleEngineService}.</li>
 *   <li><b>Audit Trail:</b> Ingestion, tax calculations, and rule executions are logged through {@link AuditLogService}.</li>
 * </ol>
 */
public interface TransactionService {

    /**
     * Ingests, validates, and evaluates a single financial transaction.
     *
     * @param requestDto The incoming transaction payload
     * @return {@link TransactionResponseDto} containing processed status, tax calculations, and compliance result
     */
    TransactionResponseDto processTransaction(TransactionRequestDto requestDto);

    /**
     * Processes a batch of financial transactions with per-record fault isolation.
     * Individual validation or calculation failures do not abort processing of remaining batch records.
     *
     * @param transactionRequests List of transaction request payloads
     * @return Summary response detailing total received, successful, failed counts, and individual outcomes
     */
    BatchUploadResponseDto processBatch(List<TransactionRequestDto> transactionRequests);

    /**
     * Looks up an ingested transaction entity by its unique business transaction identifier.
     *
     * @param transactionId The business transaction identifier
     * @return The matching {@link TransactionEntity}
     * @throws com.avega.tgd.exception.ResourceNotFoundException if no transaction matches the given ID
     */
    TransactionEntity getByTransactionId(String transactionId);

    /**
     * Retrieves a paginated view of all processed transactions.
     *
     * @param pageable Pagination and sorting parameters
     * @return Page of transaction response DTOs
     */
    Page<TransactionResponseDto> getTransactions(Pageable pageable);
}
