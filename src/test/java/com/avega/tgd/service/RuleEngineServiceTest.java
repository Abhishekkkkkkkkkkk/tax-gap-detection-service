package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.dto.request.TaxRuleRequestDto;
import com.avega.tgd.exception.ResourceNotFoundException;
import com.avega.tgd.repository.TaxRuleRepository;
import com.avega.tgd.service.impl.RuleEngineServiceImpl;
import com.avega.tgd.service.rules.RuleEvaluationResult;
import com.avega.tgd.service.rules.TaxRuleEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Rule Engine Service Unit Tests")
class RuleEngineServiceTest {

    @Mock
    private TaxRuleRepository taxRuleRepository;

    @Mock
    private ExceptionService exceptionService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private TaxRuleEvaluator evaluatorA;

    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        when(evaluatorA.getRuleName()).thenReturn("RULE_A");
        ruleEngineService = new RuleEngineServiceImpl(taxRuleRepository, exceptionService, auditLogService, List.of(evaluatorA));
    }

    @Test
    @DisplayName("executeRules should run all enabled rules and generate exception on rule failure")
    void testExecuteRulesGeneratesExceptionOnFailure() {
        TaxRuleEntity rule = new TaxRuleEntity("RULE_A", "Test rule", true, Severity.HIGH, "{}");
        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        TransactionEntity tx = new TransactionEntity();
        tx.setTransactionId("TX-100");
        tx.setCustomerId("CUST-1");
        tx.setAmount(new BigDecimal("5000"));

        when(evaluatorA.evaluate(eq(tx), eq("{}"), eq(Severity.HIGH)))
                .thenReturn(RuleEvaluationResult.fail(Severity.HIGH, "Rule A triggered"));

        ruleEngineService.executeRules(tx);

        verify(exceptionService, times(1)).createException(
                eq("TX-100"), eq("CUST-1"), eq("RULE_A"), eq(Severity.HIGH), eq("Rule A triggered"));
        verify(auditLogService, times(1)).logEvent(
                eq(AuditEventType.RULE_EXECUTION), eq("TX-100"), any());
    }

    @Test
    @DisplayName("executeRules should log audit without exception when rule passes")
    void testExecuteRulesPassesWithoutException() {
        TaxRuleEntity rule = new TaxRuleEntity("RULE_A", "Test rule", true, Severity.HIGH, "{}");
        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        TransactionEntity tx = new TransactionEntity();
        tx.setTransactionId("TX-101");
        tx.setCustomerId("CUST-1");

        when(evaluatorA.evaluate(eq(tx), eq("{}"), eq(Severity.HIGH)))
                .thenReturn(RuleEvaluationResult.pass());

        ruleEngineService.executeRules(tx);

        verify(exceptionService, never()).createException(any(), any(), any(), any(), any());
        verify(auditLogService, times(1)).logEvent(
                eq(AuditEventType.RULE_EXECUTION), eq("TX-101"), any());
    }

    @Test
    @DisplayName("toggleRule should invert the enabled status of the rule")
    void testToggleRule() {
        TaxRuleEntity rule = new TaxRuleEntity("RULE_A", "Test rule", true, Severity.HIGH, "{}");
        rule.setId(1L);

        when(taxRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(taxRuleRepository.save(rule)).thenReturn(rule);

        TaxRuleEntity toggled = ruleEngineService.toggleRule(1L);

        assertFalse(toggled.isEnabled());
        verify(taxRuleRepository).save(rule);
    }

    @Test
    @DisplayName("getRuleById for non-existent rule should throw ResourceNotFoundException")
    void testGetRuleByIdNotFound() {
        when(taxRuleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ruleEngineService.getRuleById(99L));
    }

    @Test
    @DisplayName("createRule should save and return new rule")
    void testCreateRule() {
        TaxRuleRequestDto dto = new TaxRuleRequestDto("NEW_RULE", "Desc", true, Severity.LOW, "{}");
        TaxRuleEntity entity = new TaxRuleEntity("NEW_RULE", "Desc", true, Severity.LOW, "{}");

        when(taxRuleRepository.save(any(TaxRuleEntity.class))).thenReturn(entity);

        TaxRuleEntity created = ruleEngineService.createRule(dto);

        assertNotNull(created);
        assertEquals("NEW_RULE", created.getRuleName());
    }
}
