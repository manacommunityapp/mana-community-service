package com.manacommunity.api.opsresilience.repository;

import com.manacommunity.api.opsresilience.entity.TenantAccountingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantAccountingRuleRepository extends JpaRepository<TenantAccountingRule, Long> {
    Optional<TenantAccountingRule> findByCommunityIdAndRuleKey(Long communityId, String ruleKey);
    List<TenantAccountingRule> findByCommunityIdAndActiveTrueOrderByCreatedAtDesc(Long communityId);
}
