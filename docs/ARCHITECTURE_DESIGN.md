# Architecture & System Design Document
**Project:** Tax Gap Detection & Compliance Validation Service  
**Author:** Abhishek Kumar  
**Target:** Technical Evaluation / Architecture Review  

---

## 1. Introduction & Context

When businesses file tax declarations, discrepancies inevitably arise between the taxes they calculate and what statutory tax schedules mandate. Some discrepancies are honest rounding differences; others stem from incorrect rate brackets, fraudulent refund claims, or unreported high-value transactions. 

The purpose of the **Tax Gap Detection & Compliance Validation Service** is to give tax auditors an automated, reliable backend system to ingest transaction batches, verify calculations against regulatory formulas, run configurable business rules, flag violations, maintain an unalterable audit trail, and generate real-time compliance summaries.

When designing this system, my goal was not just to make the formulas work, but to build an architecture that behaves predictably in real-world conditions—where network connections drop, payloads contain malformed dates, and database tables hold millions of historical records.

---

## 2. High-Level Architectural Flow

The service follows a traditional, disciplined **Layered Architecture** with strict directional dependencies:

```
[ HTTP REST Client / Auditor / Postman ]
                 │
                 ▼
      [ Spring Security Filter Chain ]
      (HTTP Basic Auth, Stateless Session, DB User Table)
                 │
                 ▼
       [ Controller Layer ]
       (Input Binding, DTOs, HTTP Status Codes)
                 │
                 ▼
       [ Service & Business Engine Layer ]
       ├── Ingestion & Schema Validation
       ├── Tax Gap Math Engine (BigDecimal)
       ├── Dynamic Rule Engine (Strategy Pattern)
       ├── Exception Management (JPA Specifications)
       ├── Audit Trail (Autonomous REQUIRES_NEW)
       └── Aggregation Reporting (SQL Push-down)
                 │
                 ▼
       [ Repository / Persistence Layer ]
       (Spring Data JPA, Interface Projections, Native SQL/JPQL)
                 │
                 ▼
       [ Relational Database Engine ]
       (MySQL / PostgreSQL / File-based Persistent SQL)
```

### Why Layered Architecture?
For an audit system, clean separation of concerns is vital. The controller layer knows nothing about how tax gaps are calculated; it only validates the shape of the HTTP request and returns consistent JSON response envelopes (`ApiResponseDto`). The service layer contains all business logic and remains completely decoupled from web APIs. The repository layer isolates database query optimization, allowing us to swap the underlying relational database (MySQL, PostgreSQL, or persistent file-based SQL) without touching a single line of business math.

---

## 3. Core Architectural Decisions & Trade-Offs

### A. Batch Ingestion: Fault Isolation over "All-or-Nothing"
In high-volume accounting systems, transactions arrive in large batches (e.g., 500 to 1,000 records). 

* **The Dilemma:** If transaction #47 has an invalid date or negative amount, should the entire batch fail?
* **The Decision:** No. Failing the entire batch creates severe operational bottlenecks and frustrates users. Instead, we designed **per-record fault isolation**. The ingestion pipeline evaluates each record inside its own boundary. Valid transactions are computed and persisted normally. Invalid transactions are saved with status `FAILURE` and diagnostic messages in `failure_reasons`.
* **Forensic Audit Integrity:** Even when a record is completely broken, we store the original unparsed JSON in a `raw_payload` text column. In regulatory audits, you must never throw away what a customer submitted; storing the raw payload guarantees legal non-repudiation.

### B. Financial Precision: Exact Arithmetic with BigDecimal
Floating-point data types (`float` and `double`) use binary IEEE-754 representations that cannot accurately represent base-10 fractions (such as `0.10` or `0.05`). In financial auditing, floating-point drift can falsely trigger underpayment warnings.

* **The Decision:** Every currency amount, rate, and calculation strictly uses `java.math.BigDecimal` with banker's standard rounding (`RoundingMode.HALF_UP`) scaled to two decimal places.
* **Rounding Tolerance Boundary:** Real-world POS terminals and ERP software often round line items before calculating taxes, leading to tiny fraction differences. To avoid false positives, we introduced a statutory &plusmn;1.00 tolerance rule:
  - $\lvert\text{taxGap}\rvert \le 1.00 \implies \text{COMPLIANT}$
  - $\text{taxGap} > 1.00 \implies \text{UNDERPAID}$
  - $\text{taxGap} < -1.00 \implies \text{OVERPAID}$
  - Missing reported tax $\implies \text{NON\_COMPLIANT}$

### C. Rule Engine: Strategy Pattern with Database-Driven Configurations
Compliance policies change frequently. New luxury tax thresholds or GST slab rules are introduced by tax authorities every fiscal year. Hardcoding these rules as nested `if-else` blocks in Java code would require recompiling and redeploying the application every time a number changes.

* **The Design:** We implemented the **Strategy Pattern**. Each compliance check implements a common interface:
  ```java
  public interface TaxRuleEvaluator {
      String getRuleName();
      RuleEvaluationResult evaluate(TransactionEntity transaction, String configJson, Severity defaultSeverity);
  }
  ```
* Evaluators register as Spring `@Component`s and are collected into a registry map on application startup.
* When a transaction is evaluated, the engine queries the `tax_rules` table for rules where `enabled = true`. It matches the rule name to the appropriate evaluator bean, passes the rule's `config_json` (e.g. `{"threshold": 100000.00}`), and runs the check.
* **Benefit:** Administrators can change threshold limits or disable a rule on the fly using REST APIs without any server downtime.

### D. Reporting: SQL Push-Down vs. In-Memory Calculation
A classic mistake in Spring Boot applications is writing reporting logic in Java: fetching 200,000 entities from the repository into a `List<TransactionEntity>`, looping through them with Java Streams, and calculating sums.

* **The Problem:** In production, streaming hundreds of thousands of entity objects into JVM memory creates extreme garbage collection pressure and triggers `OutOfMemoryError`.
* **The Solution:** We push the computation down to where the data lives—the database query engine. Using JPQL aggregations (`GROUP BY t.customerId`, `SUM`, `COUNT`, conditional `CASE` statements) and **Spring Data Interface Projections** (`CustomerTaxSummaryProjection`), the database returns pre-computed numeric aggregates directly from the JDBC ResultSet. Hibernate does not hydrate entity objects, keeping JVM memory footprint near zero.

### E. Audit Logging: Autonomous Transactions (`REQUIRES_NEW`)
Requirement 5 mandates logging every ingestion, tax computation, and rule execution.

* **The Risk:** If an outer business transaction fails or rolls back due to a database constraint or runtime error, a standard Spring `@Transactional` method rolls back *everything*, including the audit log of what just happened.
* **The Solution:** We annotated `AuditLogServiceImpl.logEvent` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`. This suspends the outer transaction and opens an independent physical database transaction. If the caller transaction fails, the audit record is still committed to the database, ensuring auditors can see the exact state leading up to the failure.

### F. Security & RBAC: Principle of Least Privilege
The system implements HTTP Basic Authentication with stateless session policy (`SessionCreationPolicy.STATELESS`), driven by a `users` table pre-seeded with BCrypt-hashed credentials.

* **Auditor (`ROLE_AUDITOR`):** Can ingest transactions, view reports, inspect audit trails, and review exceptions.
* **Administrator (`ROLE_ADMIN`):** Inherits all auditor privileges plus exclusive access to mutating rule endpoints (`POST /api/v1/rules`, `PUT /api/v1/rules/{id}/toggle`), enforced via method-level `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.

---

## 4. Database Schema Design

The relational schema is intentionally compact and centered around 5 indexed tables:

1. **`transactions`:** The financial ledger. Uses `precision = 18, scale = 2` for amounts and `DECIMAL(8,4)` for tax rates. Indexes on `transaction_id` (Unique), `customer_id`, `compliance_status`, and `transaction_date` optimize report grouping and temporal queries.
2. **`tax_rules`:** Stores rule names, severity, active state, and JSON threshold configurations.
3. **`exceptions`:** Stores compliance and tax gap violations. Indexed by `transaction_id`, `customer_id`, `rule_name`, and `severity` to support multi-criteria dynamic filtering.
4. **`audit_logs`:** Immutable, append-only log storing event types (`INGESTION`, `RULE_EXECUTION`, `TAX_COMPUTATION`), transaction references, timestamps, and serialized JSON state payloads.
5. **`users`:** Stores usernames, BCrypt password hashes, and granted roles.

---

## 5. Honest Assessment: Current Limitations & Future Improvements

To demonstrate pragmatic engineering judgment, here are architectural areas that could be expanded in future versions:

1. **Cumulative Refund Tracking:**  
   *Current state:* The refund rule validates that an individual refund amount does not exceed the referenced original sale.  
   *Production enhancement:* In high-frequency retail, a fraudster might issue three ₹400 refunds against a single ₹1,000 sale (total ₹1,200). A future enhancement would sum all historical refunds linked to that original transaction ID to guard against cumulative partial refunds.
2. **Asynchronous Batch Processing with Message Queues:**  
   *Current state:* Ingestion runs synchronously over HTTP REST batches (fast for hundreds of records).  
   *Production enhancement:* For processing files containing hundreds of thousands of transactions, integrating Apache Kafka or RabbitMQ with Spring Batch would decouple ingestion from HTTP timeouts and provide background worker scaling.
3. **Rule Configuration Caching:**  
   *Current state:* Active rules are queried from the database during batch processing.  
   *Production enhancement:* Adding an in-memory cache (such as Caffeine or Redis) with cache eviction on rule updates would shave off redundant SELECT queries during peak ingestion.

---

## 6. Summary

The **Tax Gap Detection & Compliance Validation Service** bridges real-world business compliance with clean software engineering. By combining layered architecture, arbitrary-precision decimal arithmetic, dynamic strategy evaluators, database-level SQL aggregations, and autonomous audit logging, the service delivers a robust, secure, and easily maintainable backend for financial auditors.
