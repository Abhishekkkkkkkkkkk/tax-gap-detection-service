package com.avega.tgd.repository;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing dynamic tax compliance rules stored in the database.
 */
@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRuleEntity, Long> {

    List<TaxRuleEntity> findByEnabledTrue();

    Optional<TaxRuleEntity> findByRuleName(String ruleName);

    boolean existsByRuleName(String ruleName);
}
