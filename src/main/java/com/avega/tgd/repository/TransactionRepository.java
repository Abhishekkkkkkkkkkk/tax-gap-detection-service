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
 * Repository interface for managing {@link TransactionEntity} persistence and executing
 * database-level SQL aggregations for tax compliance reporting.
 *
 * <p>Key Design Highlights:
 * <ul>
 *   <li><b>Performance-Oriented SQL Aggregation:</b> Summary calculations (sums of amounts,
 *       reported tax, expected tax, tax gaps, and counts of compliant vs non-compliant records)
 *       are executed directly inside the SQL engine using JPQL {@code GROUP BY} and {@code CASE}
 *       expressions. This fulfills Requirement 6 ("Reports must run efficiently on real SQL DB")
 *       and prevents streaming large result sets into JVM memory.</li>
 *   <li><b>Spring Data Projections:</b> Query results are mapped directly into lightweight
 *       {@link CustomerTaxSummaryProjection} interfaces, bypassing the overhead of full entity
 *       hydration and persistence context tracking.</li>
 *   <li><b>Refund Rule Support:</b> Provides specialized lookups to verify original sales
 *       when validating refund transactions.</li>
 * </ul>
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    /**
     * Finds a single transaction by its unique external transaction identifier.
     *
     * @param transactionId the unique external transaction identifier
     * @return an {@link Optional} containing the transaction if found
     */
    Optional<TransactionEntity> findByTransactionId(String transactionId);

    /**
     * Retrieves all transactions associated with a given customer ID.
     *
     * @param customerId the customer identifier
     * @return list of matching transactions
     */
    List<TransactionEntity> findByCustomerId(String customerId);

    /**
     * Looks up an original sale transaction by transactionId and type (typically {@code TransactionType.SALE}).
     * Used by {@code RefundValidationRuleEvaluator} when a refund explicitly links to a parent transaction.
     *
     * @param transactionId   the original transaction identifier
     * @param transactionType the expected transaction type (e.g. SALE)
     * @return the matching original sale transaction if found
     */
    Optional<TransactionEntity> findFirstByTransactionIdAndTransactionType(String transactionId, TransactionType transactionType);

    /**
     * Finds the largest historical sale transaction for a customer ordered descending by amount.
     * Used by {@code RefundValidationRuleEvaluator} as a heuristic fallback when a refund does
     * not specify an original parent transaction ID.
     *
     * @param customerId      the customer identifier
     * @param transactionType the transaction type to search (typically SALE)
     * @return the largest sale transaction recorded for the customer
     */
    Optional<TransactionEntity> findTopByCustomerIdAndTransactionTypeOrderByAmountDesc(String customerId, TransactionType transactionType);

    /**
     * Aggregates tax and compliance metrics across all customers directly within the SQL database.
     *
     * <p>Only transactions with {@code validationStatus = SUCCESS} are included in the aggregation.
     * {@code COALESCE} functions are used to ensure NULL tax values do not corrupt numeric sums.
     * Conditional {@code CASE} statements categorize records into compliant and non-compliant counts.
     *
     * @return list of aggregated projections, one row per distinct customer
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
     * Aggregates tax and compliance metrics for a specific customer directly within the SQL database.
     *
     * @param customerId the customer identifier to filter and aggregate
     * @return optional containing the aggregated summary projection if matching records exist
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
