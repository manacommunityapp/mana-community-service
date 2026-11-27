package com.manacommunity.api.helpdesk.repository;

import com.manacommunity.api.helpdesk.entity.EscalationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscalationRuleRepository extends JpaRepository<EscalationRule, Long> {

    List<EscalationRule> findByCommunityIdAndCategoryAndActiveTrueOrderByEscalationLevelAsc(
            Long communityId, String category);

    Optional<EscalationRule> findByCommunityIdAndCategoryAndEscalationLevel(
            Long communityId, String category, Integer level);
}
