package com.avega.tgd.repository;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing database-driven tax compliance rules ({@link TaxRuleEntity}).
 *
 * <p>Supports Requirement 3 ("Configurable Rule Engine Stored in Database"):
 * <ul>
 *   <li>Rules are persisted in the database with customizable JSON configurations.</li>
 *   <li>Rules can be enabled/disabled at runtime by administrators without restarting the application.</li>
 *   <li>The rule engine queries {@link #findByEnabledTrue()} per transaction batch to evaluate
 *       only active rules.</li>
 * </ul>
 */
@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRuleEntity, Long> {

    /**
     * Retrieves all tax compliance rules that are currently enabled.
     * Evaluated dynamically by {@code RuleEngineService} for every incoming transaction.
     *
     * @return list of active rule entities
     */
    List<TaxRuleEntity> findByEnabledTrue();

    /**
     * Finds a rule definition by its unique business rule name.
     *
     * @param ruleName the name of the rule (e.g., HIGH_VALUE_TRANSACTION)
     * @return an {@link Optional} containing the rule entity if present
     */
    Optional<TaxRuleEntity> findByRuleName(String ruleName);

    /**
     * Checks whether a rule definition exists by name.
     * Used by {@code DatabaseDataInitializer} during startup to prevent duplicate rule creation.
     *
     * @param ruleName the name of the rule to check
     * @return true if a rule with this name exists, false otherwise
     */
    boolean existsByRuleName(String ruleName);
}
