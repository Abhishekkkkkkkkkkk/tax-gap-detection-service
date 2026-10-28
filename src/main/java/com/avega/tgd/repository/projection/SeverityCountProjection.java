package com.avega.tgd.repository.projection;

import com.avega.tgd.domain.enums.Severity;

/**
 * Spring Data JPA projection for exception counts by severity level.
 */
public interface SeverityCountProjection {

    Severity getSeverity();

    Long getCount();
}
