package com.avega.tgd.dto.request;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper DTO for batch transaction upload requests.
 */
public class BatchTransactionRequestDto {

    private List<TransactionRequestDto> transactions = new ArrayList<>();

    public BatchTransactionRequestDto() {
    }

    public BatchTransactionRequestDto(List<TransactionRequestDto> transactions) {
        this.transactions = transactions;
    }

    public List<TransactionRequestDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionRequestDto> transactions) {
        this.transactions = transactions;
    }
}
