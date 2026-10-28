package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.dto.request.TaxRuleRequestDto;

import java.util.List;

/**
 * Service interface managing the database-driven, configurable tax compliance rule engine.
 *
 * <p>Design Architecture:
 * <ul>
 *   <li><b>Database-Driven Rules:</b> Rules are stored in the {@code tax_rules} table with associated severity,
 *       enabled flag, and JSON parameter configurations (e.g. thresholds, slabs).</li>
 *   <li><b>Strategy Pattern:</b> Each active rule maps to an implementation of {@link com.avega.tgd.service.rules.TaxRuleEvaluator}
 *       identified by its unique {@code ruleName}.</li>
 *   <li><b>Dynamic Lifecycle:</b> Rules can be created, edited, and toggled on/off at runtime without needing
 *       to recompile or restart the service.</li>
 * </ul>
 */
public interface RuleEngineService {

    /**
     * Executes all enabled compliance rules against a specific transaction.
     * Evaluates active rules in sequence, generating exception records for violations
     * and recording an audit trail event for every rule evaluated.
     *
     * @param transaction The persisted transaction entity to evaluate
     */
    void executeRules(TransactionEntity transaction);

    /**
     * Retrieves all configured tax rules stored in the database, including disabled rules.
     *
     * @return List of all rule entities
     */
    List<TaxRuleEntity> getAllRules();

    /**
     * Looks up a single tax rule configuration by its primary key ID.
     *
     * @param id The rule database ID
     * @return The matched {@link TaxRuleEntity}
     * @throws com.avega.tgd.exception.ResourceNotFoundException if no rule exists with the given ID
     */
    TaxRuleEntity getRuleById(Long id);

    /**
     * Persists a new configurable tax rule to the database.
     *
     * @param dto DTO containing ruleName, description, severity, enabled state, and JSON config
     * @return The saved {@link TaxRuleEntity}
     */
    TaxRuleEntity createRule(TaxRuleRequestDto dto);

    /**
     * Updates an existing rule's configuration, severity, description, or parameters.
     *
     * @param id  The rule ID to update
     * @param dto DTO with updated fields
     * @return The updated {@link TaxRuleEntity}
     */
    TaxRuleEntity updateRule(Long id, TaxRuleRequestDto dto);

    /**
     * Toggles the active status of a rule (inverts {@code enabled} between true and false).
     * Disabled rules are skipped during transaction evaluations.
     *
     * @param id The rule ID to toggle
     * @return The modified rule entity with its updated status
     */
    TaxRuleEntity toggleRule(Long id);
}
