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
 * Implementation of the Database-Driven Configurable Tax Rule Engine.
 */
@Service
public class RuleEngineServiceImpl implements RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineServiceImpl.class);

    private final TaxRuleRepository taxRuleRepository;
    private final ExceptionService exceptionService;
    private final AuditLogService auditLogService;
    private final Map<String, TaxRuleEvaluator> evaluatorMap = new HashMap<>();

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

    @Override
    @Transactional
    public void executeRules(TransactionEntity transaction) {
        // Fetch only enabled rules from the database
        List<TaxRuleEntity> activeRules = taxRuleRepository.findByEnabledTrue();

        for (TaxRuleEntity rule : activeRules) {
            TaxRuleEvaluator evaluator = evaluatorMap.get(rule.getRuleName());
            if (evaluator == null) {
                log.warn("No evaluator bean found for active rule '{}'. Skipping.", rule.getRuleName());
                continue;
            }

            RuleEvaluationResult result = evaluator.evaluate(transaction, rule.getConfigJson(), rule.getSeverity());

            Map<String, Object> auditPayload = new HashMap<>();
            auditPayload.put("ruleName", rule.getRuleName());
            auditPayload.put("ruleDescription", rule.getDescription());
            auditPayload.put("severity", rule.getSeverity());
            auditPayload.put("passed", result.isPassed());
            auditPayload.put("message", result.getMessage());

            if (!result.isPassed()) {
                // Generate exception record
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
                auditPayload.put("action", "COMPLIANT");
                auditLogService.logEvent(AuditEventType.RULE_EXECUTION, transaction.getTransactionId(), auditPayload);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxRuleEntity> getAllRules() {
        return taxRuleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public TaxRuleEntity getRuleById(Long id) {
        return taxRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax rule not found with id: " + id));
    }

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

    @Override
    @Transactional
    public TaxRuleEntity toggleRule(Long id) {
        TaxRuleEntity rule = getRuleById(id);
        rule.setEnabled(!rule.isEnabled());
        log.info("Tax rule '{}' (id: {}) toggled to enabled={}", rule.getRuleName(), id, rule.isEnabled());
        return taxRuleRepository.save(rule);
    }
}
