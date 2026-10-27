package com.avega.tgd.repository;

import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.TransactionType;
import com.avega.tgd.repository.projection.CustomerTaxSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing transaction entities and executing optimized aggregation queries.
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByTransactionId(String transactionId);

    List<TransactionEntity> findByCustomerId(String customerId);

    Optional<TransactionEntity> findFirstByTransactionIdAndTransactionType(String transactionId, TransactionType transactionType);

    Optional<TransactionEntity> findTopByCustomerIdAndTransactionTypeOrderByAmountDesc(String customerId, TransactionType transactionType);

    /**
     * Efficiently aggregates tax summary across all customers directly in SQL.
     */
    @Query("SELECT t.customerId AS customerId, " +
            "COUNT(t) AS totalTransactions, " +
            "SUM(t.amount) AS totalAmount, " +
            "SUM(COALESCE(t.reportedTax, 0)) AS totalReportedTax, " +
            "SUM(COALESCE(t.expectedTax, 0)) AS totalExpectedTax, " +
            "SUM(COALESCE(t.taxGap, 0)) AS totalTaxGap, " +
            "SUM(CASE WHEN t.complianceStatus = com.avega.tgd.domain.enums.ComplianceStatus.COMPLIANT THEN 1L ELSE 0L END) AS compliantCount, " +
            "SUM(CASE WHEN t.complianceStatus != com.avega.tgd.domain.enums.ComplianceStatus.COMPLIANT OR t.complianceStatus IS NULL THEN 1L ELSE 0L END) AS nonCompliantCount " +
            "FROM TransactionEntity t " +
            "WHERE t.validationStatus = com.avega.tgd.domain.enums.ValidationStatus.SUCCESS " +
            "GROUP BY t.customerId")
    List<CustomerTaxSummaryProjection> getCustomerTaxSummaries();

    /**
     * Efficiently aggregates tax summary for a specific customer directly in SQL.
     */
    @Query("SELECT t.customerId AS customerId, " +
            "COUNT(t) AS totalTransactions, " +
            "SUM(t.amount) AS totalAmount, " +
            "SUM(COALESCE(t.reportedTax, 0)) AS totalReportedTax, " +
            "SUM(COALESCE(t.expectedTax, 0)) AS totalExpectedTax, " +
            "SUM(COALESCE(t.taxGap, 0)) AS totalTaxGap, " +
            "SUM(CASE WHEN t.complianceStatus = com.avega.tgd.domain.enums.ComplianceStatus.COMPLIANT THEN 1L ELSE 0L END) AS compliantCount, " +
            "SUM(CASE WHEN t.complianceStatus != com.avega.tgd.domain.enums.ComplianceStatus.COMPLIANT OR t.complianceStatus IS NULL THEN 1L ELSE 0L END) AS nonCompliantCount " +
            "FROM TransactionEntity t " +
            "WHERE t.validationStatus = com.avega.tgd.domain.enums.ValidationStatus.SUCCESS AND t.customerId = :customerId " +
            "GROUP BY t.customerId")
    Optional<CustomerTaxSummaryProjection> getCustomerTaxSummaryByCustomerId(@Param("customerId") String customerId);
}
