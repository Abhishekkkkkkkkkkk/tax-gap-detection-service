package com.avega.tgd.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary response for a batch upload operation detailing processed, successful, and failed counts.
 */
public class BatchUploadResponseDto {

    private int totalReceived;
    private int totalSuccess;
    private int totalFailed;
    private List<TransactionResponseDto> results = new ArrayList<>();

    public BatchUploadResponseDto() {
    }

    public BatchUploadResponseDto(int totalReceived, int totalSuccess, int totalFailed, List<TransactionResponseDto> results) {
        this.totalReceived = totalReceived;
        this.totalSuccess = totalSuccess;
        this.totalFailed = totalFailed;
        this.results = results != null ? results : new ArrayList<>();
    }

    // Getters and Setters

    public int getTotalReceived() {
        return totalReceived;
    }

    public void setTotalReceived(int totalReceived) {
        this.totalReceived = totalReceived;
    }

    public int getTotalSuccess() {
        return totalSuccess;
    }

    public void setTotalSuccess(int totalSuccess) {
        this.totalSuccess = totalSuccess;
    }

    public int getTotalFailed() {
        return totalFailed;
    }

    public void setTotalFailed(int totalFailed) {
        this.totalFailed = totalFailed;
    }

    public List<TransactionResponseDto> getResults() {
        return results;
    }

    public void setResults(List<TransactionResponseDto> results) {
        this.results = results;
    }
}
