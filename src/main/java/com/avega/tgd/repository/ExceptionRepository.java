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
 * Repository interface for managing exception records, supporting dynamic specifications and summary aggregations.
 */
@Repository
public interface ExceptionRepository extends JpaRepository<ExceptionEntity, Long>, JpaSpecificationExecutor<ExceptionEntity> {

    List<ExceptionEntity> findByTransactionId(String transactionId);

    List<ExceptionEntity> findByCustomerId(String customerId);

    /**
     * Efficiently counts exceptions grouped by severity directly in SQL.
     */
    @Query("SELECT e.severity AS severity, COUNT(e) AS count FROM ExceptionEntity e GROUP BY e.severity")
    List<SeverityCountProjection> countGroupedBySeverity();

    /**
     * Efficiently counts exceptions grouped by customer directly in SQL.
     */
    @Query("SELECT e.customerId AS customerId, COUNT(e) AS count FROM ExceptionEntity e GROUP BY e.customerId ORDER BY COUNT(e) DESC")
    List<CustomerExceptionCountProjection> countGroupedByCustomer();
}
