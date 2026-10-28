package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.dto.request.TransactionRequestDto;
import com.avega.tgd.dto.response.BatchUploadResponseDto;
import com.avega.tgd.dto.response.TransactionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service governing transaction ingestion, validation, and processing.
 */
public interface TransactionService {

    TransactionResponseDto processTransaction(TransactionRequestDto requestDto);

    BatchUploadResponseDto processBatch(List<TransactionRequestDto> transactionRequests);

    TransactionEntity getByTransactionId(String transactionId);

    Page<TransactionResponseDto> getTransactions(Pageable pageable);
}
