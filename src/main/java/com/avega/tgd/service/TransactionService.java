package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.dto.request.TransactionRequestDto;
import com.avega.tgd.dto.response.BatchUploadResponseDto;
import com.avega.tgd.dto.response.TransactionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for transaction ingestion, validation, processing, and retrieval.
 */
public interface TransactionService {

    /**
     * Processes a single transaction request: validates, computes tax gap, evaluates rules, logs audit.
     */
    TransactionResponseDto processTransaction(TransactionRequestDto requestDto);

    /**
     * Processes a batch of financial transactions.
     */
    BatchUploadResponseDto processBatch(List<TransactionRequestDto> transactionRequests);

    /**
     * Retrieves a transaction entity by its business transactionId.
     */
    TransactionEntity getByTransactionId(String transactionId);

    /**
     * Retrieves paged list of transactions.
     */
    Page<TransactionResponseDto> getTransactions(Pageable pageable);
}
