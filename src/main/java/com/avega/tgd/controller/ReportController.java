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
 * Controller exposing summary reporting endpoints executed with database-level SQL aggregations.
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Endpoints for retrieving high-performance tax and exception reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/customer-tax-summary")
    @Operation(summary = "Get Customer Tax Summary Report for all customers",
               description = "Computes totalAmount, totalReportedTax, totalExpectedTax, totalTaxGap, and complianceScore per customer")
    public ResponseEntity<ApiResponseDto<List<CustomerTaxSummaryDto>>> getAllCustomerTaxSummaries() {
        List<CustomerTaxSummaryDto> summaries = reportService.getAllCustomerTaxSummaries();
        return ResponseEntity.ok(ApiResponseDto.ok(summaries));
    }

    @GetMapping("/customer-tax-summary/{customerId}")
    @Operation(summary = "Get Customer Tax Summary Report for a single customer")
    public ResponseEntity<ApiResponseDto<CustomerTaxSummaryDto>> getCustomerTaxSummary(
            @PathVariable String customerId) {
        CustomerTaxSummaryDto summary = reportService.getCustomerTaxSummary(customerId);
        return ResponseEntity.ok(ApiResponseDto.ok(summary));
    }

    @GetMapping("/exception-summary")
    @Operation(summary = "Get Exception Summary Report",
               description = "Computes total exceptions, breakdown by severity, and customer-wise exception counts")
    public ResponseEntity<ApiResponseDto<ExceptionSummaryDto>> getExceptionSummary() {
        ExceptionSummaryDto summary = reportService.getExceptionSummary();
        return ResponseEntity.ok(ApiResponseDto.ok(summary));
    }
}
