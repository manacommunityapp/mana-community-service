package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.BusinessRule;
import com.manacommunity.api.booking.entity.enums.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessRuleRepository extends JpaRepository<BusinessRule, Long> {

    List<BusinessRule> findByResourceIdAndIsActiveTrue(Long resourceId);

    List<BusinessRule> findByCategoryIdAndIsActiveTrue(Long categoryId);

    List<BusinessRule> findByCommunityIdAndIsActiveTrueOrderByPriorityAsc(Long communityId);

    List<BusinessRule> findByResourceIdAndRuleTypeAndIsActiveTrue(Long resourceId, RuleType ruleType);
}
