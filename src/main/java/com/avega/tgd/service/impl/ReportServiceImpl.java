package com.avega.tgd.service.impl;

import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.dto.response.CustomerTaxSummaryDto;
import com.avega.tgd.dto.response.ExceptionSummaryDto;
import com.avega.tgd.exception.ResourceNotFoundException;
import com.avega.tgd.repository.ExceptionRepository;
import com.avega.tgd.repository.TransactionRepository;
import com.avega.tgd.repository.projection.CustomerExceptionCountProjection;
import com.avega.tgd.repository.projection.CustomerTaxSummaryProjection;
import com.avega.tgd.repository.projection.SeverityCountProjection;
import com.avega.tgd.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ReportService} utilizing SQL database-level aggregations
 * for maximum query efficiency on relational databases.
 *
 * <p>Compliance Score Formula:
 * <pre>{@code
 *   complianceScore = 100 - (nonCompliantTransactions / totalTransactions * 100)
 * }</pre>
 *
 * <p>Database Efficiency:
 * Rather than loading all rows into Java collections and iterating in memory,
 * this service leverages ANSI-SQL {@code GROUP BY}, {@code SUM}, {@code COUNT}, and
 * {@code CASE WHEN} constructs in the database engine, returning lightweight interface projections.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private final TransactionRepository transactionRepository;
    private final ExceptionRepository exceptionRepository;

    public ReportServiceImpl(TransactionRepository transactionRepository, ExceptionRepository exceptionRepository) {
        this.transactionRepository = transactionRepository;
        this.exceptionRepository = exceptionRepository;
    }

    /**
     * Executes the customer aggregation query across all successfully validated transactions.
     *
     * @return List of mapped customer tax summary DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<CustomerTaxSummaryDto> getAllCustomerTaxSummaries() {
        List<CustomerTaxSummaryProjection> projections = transactionRepository.getCustomerTaxSummaries();
        return projections.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Executes the customer aggregation query for a single specified customer.
     *
     * @param customerId Target customer identifier
     * @return Customer tax summary DTO
     * @throws ResourceNotFoundException if customer has no valid transactions
     */
    @Override
    @Transactional(readOnly = true)
    public CustomerTaxSummaryDto getCustomerTaxSummary(String customerId) {
        return transactionRepository.getCustomerTaxSummaryByCustomerId(customerId)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("No transactions found for customer: " + customerId));
    }

    /**
     * Compiles an exception overview report:
     * <ol>
     *   <li>Total exception count via {@code COUNT(*)}.</li>
     *   <li>Exceptions grouped by severity via SQL {@code GROUP BY severity}.</li>
     *   <li>Customer-wise exception count via SQL {@code GROUP BY customer_id}.</li>
     * </ol>
     *
     * @return Combined exception summary DTO
     */
    @Override
    @Transactional(readOnly = true)
    public ExceptionSummaryDto getExceptionSummary() {
        long totalExceptions = exceptionRepository.count();

        // Step 1: Pre-populate EnumMap with 0 counts for every severity enum value (HIGH, MEDIUM, LOW)
        // to guarantee all severities are present in the response even if count is 0
        Map<Severity, Long> severityCounts = new EnumMap<>(Severity.class);
        for (Severity s : Severity.values()) {
            severityCounts.put(s, 0L);
        }
        List<SeverityCountProjection> severityProjections = exceptionRepository.countGroupedBySeverity();
        for (SeverityCountProjection p : severityProjections) {
            if (p.getSeverity() != null) {
                severityCounts.put(p.getSeverity(), p.getCount());
            }
        }

        // Step 2: Query customer-wise exception counts ordered by frequency descending
        Map<String, Long> customerCounts = new LinkedHashMap<>();
        List<CustomerExceptionCountProjection> customerProjections = exceptionRepository.countGroupedByCustomer();
        for (CustomerExceptionCountProjection p : customerProjections) {
            String cId = p.getCustomerId() != null ? p.getCustomerId() : "UNKNOWN";
            customerCounts.put(cId, p.getCount());
        }

        return new ExceptionSummaryDto(totalExceptions, severityCounts, customerCounts);
    }

    /**
     * Transforms database projection results into customer summary DTOs,
     * computing the compliance score.
     */
    private CustomerTaxSummaryDto mapToDto(CustomerTaxSummaryProjection p) {
        long total = p.getTotalTransactions() != null ? p.getTotalTransactions() : 0L;
        long nonCompliant = p.getNonCompliantCount() != null ? p.getNonCompliantCount() : 0L;
        long compliant = p.getCompliantCount() != null ? p.getCompliantCount() : 0L;

        return new CustomerTaxSummaryDto(
                p.getCustomerId(),
                total,
                p.getTotalAmount(),
                p.getTotalReportedTax(),
                p.getTotalExpectedTax(),
                p.getTotalTaxGap(),
                compliant,
                nonCompliant
        );
    }
}
