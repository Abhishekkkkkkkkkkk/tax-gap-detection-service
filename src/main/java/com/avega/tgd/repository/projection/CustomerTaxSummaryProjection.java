package com.avega.tgd.repository.projection;

import java.math.BigDecimal;

/**
 * Spring Data interface-based projection for high-performance SQL aggregation of customer tax summaries.
 *
 * <p>Why Interface Projections are used:
 * <ul>
 *   <li>Allows the RDBMS to compute sums and counts directly in the query engine.</li>
 *   <li>Spring Data JPA creates a lightweight dynamic proxy backed directly by the JDBC {@code ResultSet},
 *       avoiding the overhead of instantiating managed Hibernate entities.</li>
 *   <li>Satisfies Requirement 6.A ("Customer Tax Summary Report") with minimal memory footprint.</li>
 * </ul>
 */
public interface CustomerTaxSummaryProjection {

    /**
     * @return the unique customer identifier
     */
    String getCustomerId();

    /**
     * @return the total number of successfully validated transactions for this customer
     */
    Long getTotalTransactions();

    /**
     * @return the aggregate monetary transaction volume (sum of amounts)
     */
    BigDecimal getTotalAmount();

    /**
     * @return the aggregate tax amount reported by the customer
     */
    BigDecimal getTotalReportedTax();

    /**
     * @return the aggregate expected tax computed by the system engine
     */
    BigDecimal getTotalExpectedTax();

    /**
     * @return the aggregate tax gap (totalExpectedTax - totalReportedTax)
     */
    BigDecimal getTotalTaxGap();

    /**
     * @return the count of transactions categorized as COMPLIANT (|taxGap| &le; 1)
     */
    Long getCompliantCount();

    /**
     * @return the count of transactions categorized as UNDERPAID, OVERPAID, or NON_COMPLIANT
     */
    Long getNonCompliantCount();
}
