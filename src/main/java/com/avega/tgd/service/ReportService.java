package com.avega.tgd.service;

import com.avega.tgd.dto.response.CustomerTaxSummaryDto;
import com.avega.tgd.dto.response.ExceptionSummaryDto;

import java.util.List;

/**
 * Service interface for generating aggregated tax summary and exception reports.
 * Queries are executed directly against the database engine via SQL aggregation.
 */
public interface ReportService {

    /**
     * Aggregates tax summaries for all customers with compliance scores.
     */
    List<CustomerTaxSummaryDto> getAllCustomerTaxSummaries();

    /**
     * Aggregates tax summary for a specific customer with compliance score.
     */
    CustomerTaxSummaryDto getCustomerTaxSummary(String customerId);

    /**
     * Aggregates overall exception count, count by severity, and customer distribution.
     */
    ExceptionSummaryDto getExceptionSummary();
}
