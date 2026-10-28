package com.avega.tgd.repository;

import com.avega.tgd.domain.entity.ExceptionEntity;
import com.avega.tgd.repository.projection.CustomerExceptionCountProjection;
import com.avega.tgd.repository.projection.SeverityCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing {@link ExceptionEntity} persistence.
 *
 * <p>Extends both {@link JpaRepository} and {@link JpaSpecificationExecutor} to enable:
 * <ul>
 *   <li>Standard CRUD operations and indexed lookups by transaction ID and customer ID.</li>
 *   <li>Dynamic multi-predicate criteria queries via {@link JpaSpecificationExecutor} for
 *       flexible filtering by customerId, severity, and ruleName simultaneously.</li>
 *   <li>Optimized SQL aggregate queries for generating executive summary reports
 *       grouped by severity and customer without JVM memory overhead.</li>
 * </ul>
 */
@Repository
public interface ExceptionRepository extends JpaRepository<ExceptionEntity, Long>, JpaSpecificationExecutor<ExceptionEntity> {

    /**
     * Retrieves all exception records associated with a specific transaction.
     *
     * @param transactionId the unique transaction identifier
     * @return list of matching exception entities
     */
    List<ExceptionEntity> findByTransactionId(String transactionId);

    /**
     * Retrieves all exception records associated with a specific customer.
     *
     * @param customerId the customer identifier
     * @return list of matching exception entities
     */
    List<ExceptionEntity> findByCustomerId(String customerId);

    /**
     * Counts exceptions grouped by severity level directly within the database engine.
     *
     * <p>Used by {@code ReportService} to fulfill Requirement 6.B without hydrating
     * individual entity rows.
     *
     * @return list of projections containing each {@link com.avega.tgd.domain.enums.Severity} and its count
     */
    @Query("SELECT e.severity AS severity, COUNT(e) AS count FROM ExceptionEntity e GROUP BY e.severity")
    List<SeverityCountProjection> countGroupedBySeverity();

    /**
     * Counts exceptions grouped by customer ordered by exception count descending.
     *
     * <p>Used by {@code ReportService} to identify top non-compliant customers.
     *
     * @return list of projections containing customerId and exception count
     */
    @Query("SELECT e.customerId AS customerId, COUNT(e) AS count FROM ExceptionEntity e GROUP BY e.customerId ORDER BY COUNT(e) DESC")
    List<CustomerExceptionCountProjection> countGroupedByCustomer();
}
