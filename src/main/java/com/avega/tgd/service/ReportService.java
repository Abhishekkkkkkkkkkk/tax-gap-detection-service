package com.avega.tgd.service;

import com.avega.tgd.dto.response.CustomerTaxSummaryDto;
import com.avega.tgd.dto.response.ExceptionSummaryDto;

import java.util.List;

/**
 * Service for generating aggregated tax summary and exception reports.
 */
public interface ReportService {

    List<CustomerTaxSummaryDto> getAllCustomerTaxSummaries();

    CustomerTaxSummaryDto getCustomerTaxSummary(String customerId);

    ExceptionSummaryDto getExceptionSummary();
}
