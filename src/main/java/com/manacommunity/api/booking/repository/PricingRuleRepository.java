package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    List<PricingRule> findByResourceIdAndIsActiveTrueOrderByPricingTypeAsc(Long resourceId);

    List<PricingRule> findByCategoryIdAndIsActiveTrueOrderByPricingTypeAsc(Long categoryId);

    List<PricingRule> findByCommunityIdAndIsActiveTrueOrderByPricingTypeAsc(Long communityId);
}
