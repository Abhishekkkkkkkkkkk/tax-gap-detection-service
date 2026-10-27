package com.avega.tgd.repository.projection;

import com.avega.tgd.domain.enums.Severity;

/**
 * Spring Data projection for exception count grouped by severity.
 */
public interface SeverityCountProjection {
    Severity getSeverity();
    Long getCount();
}
