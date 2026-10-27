package com.avega.tgd.repository.projection;

import java.math.BigDecimal;

/**
 * Spring Data projection interface for high-performance SQL aggregation of customer tax summaries.
 */
public interface CustomerTaxSummaryProjection {
    String getCustomerId();
    Long getTotalTransactions();
    BigDecimal getTotalAmount();
    BigDecimal getTotalReportedTax();
    BigDecimal getTotalExpectedTax();
    BigDecimal getTotalTaxGap();
    Long getCompliantCount();
    Long getNonCompliantCount();
}
