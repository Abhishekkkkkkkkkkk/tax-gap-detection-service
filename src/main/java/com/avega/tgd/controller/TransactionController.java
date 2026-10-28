package com.avega.tgd.controller;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.dto.request.BatchTransactionRequestDto;
import com.avega.tgd.dto.request.TransactionRequestDto;
import com.avega.tgd.dto.response.ApiResponseDto;
import com.avega.tgd.dto.response.BatchUploadResponseDto;
import com.avega.tgd.dto.response.TransactionResponseDto;
import com.avega.tgd.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller managing financial transaction ingestion and retrieval operations.
 *
 * <p>Endpoints provide:
 * <ul>
 *   <li>Batch transaction ingestion via direct JSON array ({@code /batch}) or wrapper object ({@code /upload}).</li>
 *   <li>Single transaction processing ({@code POST /api/v1/transactions}).</li>
 *   <li>Point lookups by unique transaction identifier ({@code GET /{transactionId}}).</li>
 *   <li>Paginated browsing of transaction history ({@code GET /api/v1/transactions}).</li>
 * </ul>
 *
 * <p>Security: Requires authenticated session with either {@code ROLE_AUDITOR} or {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Endpoints for uploading and querying financial transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Uploads and processes a batch of financial transactions formatted as a direct JSON array.
     *
     * <p>Executes validation, expected tax and tax gap computation, and active rule evaluation.
     * Individual record failures are isolated and reported without aborting the remaining batch.
     *
     * @param transactions list of transaction request DTOs
     * @return HTTP 201 CREATED with {@link BatchUploadResponseDto} detailing processed and failed counts
     */
    @PostMapping("/batch")
    @Operation(summary = "Upload a batch of transactions",
               description = "Validates each transaction, calculates tax gap & compliance, and runs active rules")
    public ResponseEntity<ApiResponseDto<BatchUploadResponseDto>> uploadBatch(
            @RequestBody List<TransactionRequestDto> transactions) {
        BatchUploadResponseDto response = transactionService.processBatch(transactions);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Batch transactions processed", response));
    }

    /**
     * Uploads and processes a batch of financial transactions wrapped inside an object container
     * (e.g. {@code { "transactions": [ ... ] }}).
     *
     * @param request wrapper containing transaction request list
     * @return HTTP 201 CREATED with {@link BatchUploadResponseDto}
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload transactions via container object",
               description = "Alias for batch upload accepting { \"transactions\": [...] }")
    public ResponseEntity<ApiResponseDto<BatchUploadResponseDto>> uploadBatchWrapper(
            @RequestBody BatchTransactionRequestDto request) {
        List<TransactionRequestDto> list = request != null ? request.getTransactions() : List.of();
        BatchUploadResponseDto response = transactionService.processBatch(list);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Batch transactions processed", response));
    }

    /**
     * Ingests and evaluates a single financial transaction synchronously.
     *
     * @param transaction the transaction request DTO
     * @return HTTP 201 CREATED with the processed {@link TransactionResponseDto}
     */
    @PostMapping
    @Operation(summary = "Upload a single transaction")
    public ResponseEntity<ApiResponseDto<TransactionResponseDto>> uploadSingle(
            @RequestBody TransactionRequestDto transaction) {
        TransactionResponseDto response = transactionService.processTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Transaction processed", response));
    }

    /**
     * Retrieves an individual transaction by its unique external transaction ID.
     *
     * @param transactionId the unique transaction identifier
     * @return HTTP 200 OK with the transaction details, or HTTP 404 NOT FOUND
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by transactionId")
    public ResponseEntity<ApiResponseDto<TransactionResponseDto>> getByTransactionId(
            @PathVariable String transactionId) {
        TransactionEntity entity = transactionService.getByTransactionId(transactionId);
        return ResponseEntity.ok(ApiResponseDto.ok(TransactionResponseDto.fromEntity(entity)));
    }

    /**
     * Retrieves a paginated list of transactions ordered by default sort criteria.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return HTTP 200 OK with a page of {@link TransactionResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get paged list of transactions")
    public ResponseEntity<ApiResponseDto<Page<TransactionResponseDto>>> getTransactions(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<TransactionResponseDto> page = transactionService.getTransactions(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }
}
