package com.avega.tgd.service;

import com.avega.tgd.dto.response.CustomerTaxSummaryDto;
import com.avega.tgd.dto.response.ExceptionSummaryDto;

import java.util.List;

/**
 * Service interface for generating aggregated tax summary and exception reports.
 *
 * <p>Performance Design:
 * All aggregations (sums, counts, group-bys) are performed directly within the database engine
 * using optimized SQL queries and JPA Projections. This satisfies the assessment requirement that
 * reports run efficiently on real SQL databases without loading raw transaction entities into JVM memory.
 */
public interface ReportService {

    /**
     * Aggregates tax summaries for all customers across all successfully validated transactions.
     * Computes totals and compliance scores using database-level grouping.
     *
     * @return List of {@link CustomerTaxSummaryDto} for all customers with transactions
     */
    List<CustomerTaxSummaryDto> getAllCustomerTaxSummaries();

    /**
     * Aggregates tax summary and compliance score for a single customer.
     *
     * @param customerId Unique customer identifier
     * @return {@link CustomerTaxSummaryDto}
     * @throws com.avega.tgd.exception.ResourceNotFoundException if the customer has no valid transactions
     */
    CustomerTaxSummaryDto getCustomerTaxSummary(String customerId);

    /**
     * Aggregates exception metrics across the entire system:
     * <ul>
     *   <li>Total exception count</li>
     *   <li>Count breakdown by {@link com.avega.tgd.domain.enums.Severity} (HIGH, MEDIUM, LOW)</li>
     *   <li>Customer-wise exception frequency ranking</li>
     * </ul>
     *
     * @return {@link ExceptionSummaryDto} containing aggregated metrics
     */
    ExceptionSummaryDto getExceptionSummary();
}
