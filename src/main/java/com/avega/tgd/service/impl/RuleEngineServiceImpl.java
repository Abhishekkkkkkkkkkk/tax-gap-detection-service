package com.avega.tgd.service.impl;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import com.avega.tgd.dto.request.TaxRuleRequestDto;
import com.avega.tgd.exception.ResourceNotFoundException;
import com.avega.tgd.repository.TaxRuleRepository;
import com.avega.tgd.service.AuditLogService;
import com.avega.tgd.service.ExceptionService;
import com.avega.tgd.service.RuleEngineService;
import com.avega.tgd.service.rules.RuleEvaluationResult;
import com.avega.tgd.service.rules.TaxRuleEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link RuleEngineService} providing database-driven rule execution.
 *
 * <p>Mechanisms:
 * <ul>
 *   <li>Evaluators are discovered via Spring dependency injection as a {@code List<TaxRuleEvaluator>}
 *       and indexed in an in-memory lookup map by {@code ruleName}.</li>
 *   <li>Only rules marked {@code enabled = true} in the database are fetched for evaluation.</li>
 *   <li>Each evaluated rule logs an immutable audit event to provide transparency into which rules ran,
 *       what parameters were evaluated, and whether they passed or triggered an exception.</li>
 * </ul>
 */
@Service
public class RuleEngineServiceImpl implements RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineServiceImpl.class);

    private final TaxRuleRepository taxRuleRepository;
    private final ExceptionService exceptionService;
    private final AuditLogService auditLogService;
    private final Map<String, TaxRuleEvaluator> evaluatorMap = new HashMap<>();

    /**
     * Constructs the rule engine and registers all available {@link TaxRuleEvaluator} beans.
     *
     * @param taxRuleRepository Repository for database rule definitions
     * @param exceptionService  Service for generating compliance exceptions
     * @param auditLogService   Service for recording execution audit logs
     * @param evaluators        All Spring-managed evaluator beans
     */
    public RuleEngineServiceImpl(TaxRuleRepository taxRuleRepository,
                                  ExceptionService exceptionService,
                                  AuditLogService auditLogService,
                                  List<TaxRuleEvaluator> evaluators) {
        this.taxRuleRepository = taxRuleRepository;
        this.exceptionService = exceptionService;
        this.auditLogService = auditLogService;
        if (evaluators != null) {
            for (TaxRuleEvaluator evaluator : evaluators) {
                this.evaluatorMap.put(evaluator.getRuleName(), evaluator);
            }
        }
    }

    /**
     * Executes all active rules against the given transaction.
     * Queries {@code taxRuleRepository.findByEnabledTrue()} so disabled rules are omitted dynamically.
     *
     * @param transaction Persisted transaction entity to inspect
     */
    @Override
    @Transactional
    public void executeRules(TransactionEntity transaction) {
        // Query only active/enabled rules from the database
        List<TaxRuleEntity> activeRules = taxRuleRepository.findByEnabledTrue();

        for (TaxRuleEntity rule : activeRules) {
            TaxRuleEvaluator evaluator = evaluatorMap.get(rule.getRuleName());
            if (evaluator == null) {
                // Defensive handling: database rule has no matching Java strategy component
                log.warn("No evaluator bean found for active rule '{}'. Skipping.", rule.getRuleName());
                continue;
            }

            // Execute evaluation passing transaction, rule's custom JSON config, and fallback severity
            RuleEvaluationResult result = evaluator.evaluate(transaction, rule.getConfigJson(), rule.getSeverity());

            Map<String, Object> auditPayload = new HashMap<>();
            auditPayload.put("ruleName", rule.getRuleName());
            auditPayload.put("ruleDescription", rule.getDescription());
            auditPayload.put("severity", rule.getSeverity());
            auditPayload.put("passed", result.isPassed());
            auditPayload.put("message", result.getMessage());

            if (!result.isPassed()) {
                // Violation detected: persist an Exception record for auditor review
                exceptionService.createException(
                        transaction.getTransactionId(),
                        transaction.getCustomerId(),
                        rule.getRuleName(),
                        result.getSeverity() != null ? result.getSeverity() : rule.getSeverity(),
                        result.getMessage()
                );

                auditPayload.put("action", "EXCEPTION_GENERATED");
                auditLogService.logEvent(AuditEventType.RULE_EXECUTION, transaction.getTransactionId(), auditPayload);
            } else {
                // Rule passed: record compliant execution in the audit trail
                auditPayload.put("action", "COMPLIANT");
                auditLogService.logEvent(AuditEventType.RULE_EXECUTION, transaction.getTransactionId(), auditPayload);
            }
        }
    }

    /**
     * Returns all tax rules from the database.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TaxRuleEntity> getAllRules() {
        return taxRuleRepository.findAll();
    }

    /**
     * Finds a single rule by ID.
     *
     * @throws ResourceNotFoundException if rule ID does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public TaxRuleEntity getRuleById(Long id) {
        return taxRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax rule not found with id: " + id));
    }

    /**
     * Creates a new rule definition in the database.
     */
    @Override
    @Transactional
    public TaxRuleEntity createRule(TaxRuleRequestDto dto) {
        TaxRuleEntity entity = new TaxRuleEntity(
                dto.getRuleName(),
                dto.getDescription(),
                dto.isEnabled(),
                dto.getSeverity(),
                dto.getConfigJson()
        );
        return taxRuleRepository.save(entity);
    }

    /**
     * Updates an existing rule configuration in the database.
     */
    @Override
    @Transactional
    public TaxRuleEntity updateRule(Long id, TaxRuleRequestDto dto) {
        TaxRuleEntity rule = getRuleById(id);
        rule.setRuleName(dto.getRuleName());
        rule.setDescription(dto.getDescription());
        rule.setEnabled(dto.isEnabled());
        rule.setSeverity(dto.getSeverity());
        rule.setConfigJson(dto.getConfigJson());
        return taxRuleRepository.save(rule);
    }

    /**
     * Inverts the enabled state of a rule. If active, disables it; if disabled, enables it.
     */
    @Override
    @Transactional
    public TaxRuleEntity toggleRule(Long id) {
        TaxRuleEntity rule = getRuleById(id);
        rule.setEnabled(!rule.isEnabled());
        log.info("Tax rule '{}' (id: {}) toggled to enabled={}", rule.getRuleName(), id, rule.isEnabled());
        return taxRuleRepository.save(rule);
    }
}
