package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.dto.request.TaxRuleRequestDto;

import java.util.List;

/**
 * Service interface for executing active compliance rules and administering rule configurations.
 */
public interface RuleEngineService {

    /**
     * Executes all active rules against the given transaction.
     * Generates exceptions and audit logs for rule outcomes.
     */
    void executeRules(TransactionEntity transaction);

    /**
     * Returns all configured tax rules in the database.
     */
    List<TaxRuleEntity> getAllRules();

    /**
     * Returns a rule by ID.
     */
    TaxRuleEntity getRuleById(Long id);

    /**
     * Creates a new rule in the database.
     */
    TaxRuleEntity createRule(TaxRuleRequestDto dto);

    /**
     * Updates an existing rule configuration.
     */
    TaxRuleEntity updateRule(Long id, TaxRuleRequestDto dto);

    /**
     * Toggles a rule's active state (enabled/disabled).
     */
    TaxRuleEntity toggleRule(Long id);
}
