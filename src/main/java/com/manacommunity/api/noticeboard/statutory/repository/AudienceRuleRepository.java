package com.manacommunity.api.noticeboard.statutory.repository;

import com.manacommunity.api.noticeboard.statutory.entity.AudienceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudienceRuleRepository extends JpaRepository<AudienceRule, Long> {
    List<AudienceRule> findByAudienceGroupIdOrderByRuleOrder(Long groupId);
}
