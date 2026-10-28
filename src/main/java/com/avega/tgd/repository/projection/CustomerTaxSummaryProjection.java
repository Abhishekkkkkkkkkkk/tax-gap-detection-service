package com.avega.tgd.repository.projection;

import java.math.BigDecimal;

/**
 * Spring Data JPA projection for customer tax summary aggregations.
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
