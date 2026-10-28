package com.avega.tgd.service;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import com.avega.tgd.domain.entity.TransactionEntity;
import com.avega.tgd.dto.request.TaxRuleRequestDto;

import java.util.List;

/**
 * Service managing database-driven compliance rule execution and lifecycle.
 */
public interface RuleEngineService {

    void executeRules(TransactionEntity transaction);

    List<TaxRuleEntity> getAllRules();

    TaxRuleEntity getRuleById(Long id);

    TaxRuleEntity createRule(TaxRuleRequestDto dto);

    TaxRuleEntity updateRule(Long id, TaxRuleRequestDto dto);

    TaxRuleEntity toggleRule(Long id);
}
