package com.avega.tgd.repository.projection;

import com.avega.tgd.domain.enums.Severity;

/**
 * Spring Data interface-based projection for aggregating exception counts grouped by severity level.
 *
 * <p>Supports Requirement 6.B ("Exception Summary Report - Count by severity"):
 * Directly retrieves {@code (severity, count)} pairs computed in SQL, which {@code ReportServiceImpl}
 * maps into a complete {@code Map<Severity, Long>} with zero-defaulted missing severities.
 */
public interface SeverityCountProjection {

    /**
     * @return the severity level (HIGH, MEDIUM, LOW)
     */
    Severity getSeverity();

    /**
     * @return the count of exceptions categorized at this severity level
     */
    Long getCount();
}
