package com.avega.tgd.repository.projection;

/**
 * Spring Data interface-based projection for aggregating exception counts by customer.
 *
 * <p>Supports Requirement 6.B ("Exception Summary Report - Customer-wise exception count"):
 * Enables executing {@code GROUP BY e.customerId} in SQL and projecting the aggregated results
 * without loading full exception entities into the JVM.
 */
public interface CustomerExceptionCountProjection {

    /**
     * @return the customer identifier
     */
    String getCustomerId();

    /**
     * @return the total number of rule/tax exceptions recorded for this customer
     */
    Long getCount();
}
