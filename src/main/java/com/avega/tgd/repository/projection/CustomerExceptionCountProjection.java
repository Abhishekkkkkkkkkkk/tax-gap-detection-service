package com.avega.tgd.repository.projection;

/**
 * Spring Data projection for customer-wise exception counts.
 */
public interface CustomerExceptionCountProjection {
    String getCustomerId();
    Long getCount();
}
