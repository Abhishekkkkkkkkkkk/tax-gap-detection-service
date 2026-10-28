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
 * Implementation of {@link ReportService} utilizing SQL-level aggregations and JPA projections.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private final TransactionRepository transactionRepository;
    private final ExceptionRepository exceptionRepository;

    public ReportServiceImpl(TransactionRepository transactionRepository, ExceptionRepository exceptionRepository) {
        this.transactionRepository = transactionRepository;
        this.exceptionRepository = exceptionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerTaxSummaryDto> getAllCustomerTaxSummaries() {
        List<CustomerTaxSummaryProjection> projections = transactionRepository.getCustomerTaxSummaries();
        return projections.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerTaxSummaryDto getCustomerTaxSummary(String customerId) {
        return transactionRepository.getCustomerTaxSummaryByCustomerId(customerId)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("No transactions found for customer: " + customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public ExceptionSummaryDto getExceptionSummary() {
        long totalExceptions = exceptionRepository.count();

        // Default all severity levels to 0 so every level is present in the response
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

        Map<String, Long> customerCounts = new LinkedHashMap<>();
        List<CustomerExceptionCountProjection> customerProjections = exceptionRepository.countGroupedByCustomer();
        for (CustomerExceptionCountProjection p : customerProjections) {
            String cId = p.getCustomerId() != null ? p.getCustomerId() : "UNKNOWN";
            customerCounts.put(cId, p.getCount());
        }

        return new ExceptionSummaryDto(totalExceptions, severityCounts, customerCounts);
    }

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
