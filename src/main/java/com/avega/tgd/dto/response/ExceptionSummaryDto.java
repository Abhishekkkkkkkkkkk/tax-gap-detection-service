package com.avega.tgd.dto.response;

import com.avega.tgd.domain.enums.Severity;

import java.util.HashMap;
import java.util.Map;

/**
 * Summary report response detailing exceptions count, count by severity,
 * and customer-wise exception distribution.
 */
public class ExceptionSummaryDto {

    private long totalExceptions;
    private Map<Severity, Long> countBySeverity = new HashMap<>();
    private Map<String, Long> customerWiseExceptionCount = new HashMap<>();

    public ExceptionSummaryDto() {
    }

    public ExceptionSummaryDto(long totalExceptions, Map<Severity, Long> countBySeverity, Map<String, Long> customerWiseExceptionCount) {
        this.totalExceptions = totalExceptions;
        this.countBySeverity = countBySeverity != null ? countBySeverity : new HashMap<>();
        this.customerWiseExceptionCount = customerWiseExceptionCount != null ? customerWiseExceptionCount : new HashMap<>();
    }

    // Getters and Setters

    public long getTotalExceptions() {
        return totalExceptions;
    }

    public void setTotalExceptions(long totalExceptions) {
        this.totalExceptions = totalExceptions;
    }

    public Map<Severity, Long> getCountBySeverity() {
        return countBySeverity;
    }

    public void setCountBySeverity(Map<Severity, Long> countBySeverity) {
        this.countBySeverity = countBySeverity;
    }

    public Map<String, Long> getCustomerWiseExceptionCount() {
        return customerWiseExceptionCount;
    }

    public void setCustomerWiseExceptionCount(Map<String, Long> customerWiseExceptionCount) {
        this.customerWiseExceptionCount = customerWiseExceptionCount;
    }
}
