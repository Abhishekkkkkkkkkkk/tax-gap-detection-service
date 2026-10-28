package com.avega.tgd.controller;

import com.avega.tgd.dto.response.ApiResponseDto;
import com.avega.tgd.dto.response.CustomerTaxSummaryDto;
import com.avega.tgd.dto.response.ExceptionSummaryDto;
import com.avega.tgd.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing summary tax and exception analytics endpoints.
 *
 * <p>Supports Requirement 6 ("Reporting API"):
 * <ul>
 *   <li><b>Customer Tax Summary Report (6.A):</b> Computes total volume, reported tax,
 *       expected tax, tax gap, and dynamic compliance scores per customer.</li>
 *   <li><b>Exception Summary Report (6.B):</b> Generates total exceptions, severity breakdown,
 *       and customer-wise exception rankings.</li>
 * </ul>
 *
 * <p>Performance Design: All reporting queries run directly inside the SQL engine via
 * projections, preventing out-of-memory errors on large historical datasets.
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Endpoints for retrieving high-performance tax and exception reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Generates customer tax summary reports across all customers in the database.
     *
     * @return HTTP 200 OK with list of {@link CustomerTaxSummaryDto} objects
     */
    @GetMapping("/customer-tax-summary")
    @Operation(summary = "Get Customer Tax Summary Report for all customers",
               description = "Computes totalAmount, totalReportedTax, totalExpectedTax, totalTaxGap, and complianceScore per customer")
    public ResponseEntity<ApiResponseDto<List<CustomerTaxSummaryDto>>> getAllCustomerTaxSummaries() {
        List<CustomerTaxSummaryDto> summaries = reportService.getAllCustomerTaxSummaries();
        return ResponseEntity.ok(ApiResponseDto.ok(summaries));
    }

    /**
     * Generates a tax summary report for a single customer identified by customerId.
     *
     * @param customerId the unique customer identifier
     * @return HTTP 200 OK with {@link CustomerTaxSummaryDto}, or HTTP 404 NOT FOUND
     */
    @GetMapping("/customer-tax-summary/{customerId}")
    @Operation(summary = "Get Customer Tax Summary Report for a single customer")
    public ResponseEntity<ApiResponseDto<CustomerTaxSummaryDto>> getCustomerTaxSummary(
            @PathVariable String customerId) {
        CustomerTaxSummaryDto summary = reportService.getCustomerTaxSummary(customerId);
        return ResponseEntity.ok(ApiResponseDto.ok(summary));
    }

    /**
     * Generates the executive exception summary report with overall counts, severity distribution,
     * and customer frequency rankings.
     *
     * @return HTTP 200 OK with {@link ExceptionSummaryDto}
     */
    @GetMapping("/exception-summary")
    @Operation(summary = "Get Exception Summary Report",
               description = "Computes total exceptions, breakdown by severity, and customer-wise exception counts")
    public ResponseEntity<ApiResponseDto<ExceptionSummaryDto>> getExceptionSummary() {
        ExceptionSummaryDto summary = reportService.getExceptionSummary();
        return ResponseEntity.ok(ApiResponseDto.ok(summary));
    }
}
