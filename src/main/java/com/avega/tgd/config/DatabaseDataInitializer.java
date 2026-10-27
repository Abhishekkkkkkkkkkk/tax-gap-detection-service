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
 * Initializes default database records for Users and Mandatory Tax Rules on application startup.
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

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedTaxRules();
    }

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
