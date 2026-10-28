package com.avega.tgd.repository.projection;

/**
 * Spring Data JPA projection for exception counts grouped by customer.
 */
public interface CustomerExceptionCountProjection {

    String getCustomerId();

    Long getCount();
}
