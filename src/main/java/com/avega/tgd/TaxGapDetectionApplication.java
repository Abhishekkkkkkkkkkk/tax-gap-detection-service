package com.avega.tgd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Tax Gap Detection & Compliance Validation Service
 *
 * <p>Spring Boot application providing automated compliance audit and tax gap detection
 * for financial transactions submitted by customers.
 *
 * <p>Core System Capabilities:
 * <ul>
 *   <li><b>Transaction Ingestion &amp; Validation:</b> Batch and single transaction ingestion
 *       with schema validation, date parsing, and failure isolation.</li>
 *   <li><b>Tax Gap Calculation Engine:</b> Expected tax and tax gap computations using arbitrary-precision
 *       {@link java.math.BigDecimal} arithmetic and rounding tolerance boundaries (&plusmn;1.00).</li>
 *   <li><b>Configurable Database Rule Engine:</b> Runtime-loadable compliance rules stored in database
 *       with JSON parameterization (High Value, Refund Validation, GST Slab Violation).</li>
 *   <li><b>Exception Management:</b> Audit and dispute tracking with multi-criteria dynamic filtering
 *       by customer, rule, and severity level.</li>
 *   <li><b>Regulatory Audit Logging:</b> Immutable event trail running with autonomous transaction
 *       boundaries ({@code REQUIRES_NEW}) capturing ingestion, rule execution, and tax computations.</li>
 *   <li><b>High-Performance Reporting:</b> Real-time SQL aggregations for customer tax summaries
 *       and executive exception breakdowns.</li>
 *   <li><b>Security &amp; RBAC:</b> HTTP Basic Authentication with BCrypt-hashed credentials stored
 *       in the database, distinguishing AUDITOR and ADMIN privileges.</li>
 * </ul>
 */
@SpringBootApplication
public class TaxGapDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxGapDetectionApplication.class, args);
    }
}
