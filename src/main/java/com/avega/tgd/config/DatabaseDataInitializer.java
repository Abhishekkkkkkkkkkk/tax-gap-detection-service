package com.avega.tgd.config;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import com.avega.tgd.domain.entity.UserEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.domain.enums.UserRole;
import com.avega.tgd.repository.TaxRuleRepository;
import com.avega.tgd.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application startup initializer seeding essential default records into the database.
 *
 * <p>Implements {@link CommandLineRunner} to execute immediately after the Spring application
 * context is fully bootstrapped.
 *
 * <p>Key Responsibilities:
 * <ul>
 *   <li><b>Security User Seeding:</b> Ensures the {@code users} table is prefilled with standard
 *       accounts (auditor and admin) with BCrypt-hashed passwords, fulfilling the requirement:
 *       <i>"Spring Security (authentication & authorization using a User table with prefilled data in DB)"</i>.</li>
 *   <li><b>Mandatory Rule Seeding:</b> Automatically provisions the three mandatory compliance rules
 *       specified in Requirement 3:
 *     <ol>
 *       <li>{@code HIGH_VALUE_TRANSACTION}: Flags transactions exceeding threshold (100,000.00).</li>
 *       <li>{@code REFUND_VALIDATION}: Validates refund amounts against original sales.</li>
 *       <li>{@code GST_SLAB_VIOLATION}: Enforces minimum tax rate (18%) for high-bracket amounts (50,000.00).</li>
 *     </ol>
 *   </li>
 *   <li><b>Idempotency:</b> Employs {@code existsByUsername} and {@code existsByRuleName} existence checks
 *       before persisting, making the initializer completely safe across multiple application restarts
 *       and compatible with persistent relational databases (PostgreSQL, MySQL, etc.).</li>
 * </ul>
 */
@Component
public class DatabaseDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDataInitializer.class);

    private final UserRepository userRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseDataInitializer(UserRepository userRepository,
                                   TaxRuleRepository taxRuleRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.taxRuleRepository = taxRuleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Entry point executed on Spring Boot application startup.
     *
     * @param args command-line arguments passed to the application
     */
    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedTaxRules();
    }

    /**
     * Seeds initial user accounts for RBAC authentication if they do not already exist.
     */
    private void seedUsers() {
        if (!userRepository.existsByUsername("auditor")) {
            UserEntity auditor = new UserEntity(
                    "auditor",
                    passwordEncoder.encode("auditor123"),
                    UserRole.ROLE_AUDITOR.name(),
                    true
            );
            userRepository.save(auditor);
            log.info("Initialized default user 'auditor' with role ROLE_AUDITOR");
        }

        if (!userRepository.existsByUsername("admin")) {
            UserEntity admin = new UserEntity(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    UserRole.ROLE_ADMIN.name(),
                    true
            );
            userRepository.save(admin);
            log.info("Initialized default user 'admin' with role ROLE_ADMIN");
        }
    }

    /**
     * Seeds the three mandatory compliance rules required by Requirement 3 with their JSON configurations.
     */
    private void seedTaxRules() {
        // (A) High-Value Transaction Rule
        if (!taxRuleRepository.existsByRuleName("HIGH_VALUE_TRANSACTION")) {
            TaxRuleEntity highValueRule = new TaxRuleEntity(
                    "HIGH_VALUE_TRANSACTION",
                    "Flag transactions exceeding the configured high-value threshold (default 100,000.00)",
                    true,
                    Severity.HIGH,
                    "{\"threshold\": 100000.00}"
            );
            taxRuleRepository.save(highValueRule);
            log.info("Initialized default rule: HIGH_VALUE_TRANSACTION");
        }

        // (B) Refund Validation Rule
        if (!taxRuleRepository.existsByRuleName("REFUND_VALIDATION")) {
            TaxRuleEntity refundRule = new TaxRuleEntity(
                    "REFUND_VALIDATION",
                    "Verify that refund amounts do not exceed the original sale amount",
                    true,
                    Severity.HIGH,
                    "{\"allowUnlinkedRefund\": false}"
            );
            taxRuleRepository.save(refundRule);
            log.info("Initialized default rule: REFUND_VALIDATION");
        }

        // (C) GST Slab Violation Rule
        if (!taxRuleRepository.existsByRuleName("GST_SLAB_VIOLATION")) {
            TaxRuleEntity gstSlabRule = new TaxRuleEntity(
                    "GST_SLAB_VIOLATION",
                    "Flag transactions exceeding the slab threshold where tax rate is lower than required slab",
                    true,
                    Severity.MEDIUM,
                    "{\"slabThreshold\": 50000.00, \"requiredTaxRate\": 0.18}"
            );
            taxRuleRepository.save(gstSlabRule);
            log.info("Initialized default rule: GST_SLAB_VIOLATION");
        }
    }
}
