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
 * REST controller for transaction ingestion and retrieval operations.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Endpoints for uploading and querying financial transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/batch")
    @Operation(summary = "Upload a batch of transactions",
               description = "Validates each transaction, calculates tax gap & compliance, and runs active rules")
    public ResponseEntity<ApiResponseDto<BatchUploadResponseDto>> uploadBatch(
            @RequestBody List<TransactionRequestDto> transactions) {
        BatchUploadResponseDto response = transactionService.processBatch(transactions);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Batch transactions processed", response));
    }

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

    @PostMapping
    @Operation(summary = "Upload a single transaction")
    public ResponseEntity<ApiResponseDto<TransactionResponseDto>> uploadSingle(
            @RequestBody TransactionRequestDto transaction) {
        TransactionResponseDto response = transactionService.processTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Transaction processed", response));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by external transaction ID")
    public ResponseEntity<ApiResponseDto<TransactionResponseDto>> getByTransactionId(
            @PathVariable String transactionId) {
        TransactionEntity entity = transactionService.getByTransactionId(transactionId);
        return ResponseEntity.ok(ApiResponseDto.ok(TransactionResponseDto.fromEntity(entity)));
    }

    @GetMapping
    @Operation(summary = "List all transactions with pagination")
    public ResponseEntity<ApiResponseDto<Page<TransactionResponseDto>>> getTransactions(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<TransactionResponseDto> page = transactionService.getTransactions(pageable);
        return ResponseEntity.ok(ApiResponseDto.ok(page));
    }
}
