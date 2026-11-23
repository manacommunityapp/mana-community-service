package com.manacommunity.api.analytics.repository;

import com.manacommunity.api.analytics.model.CommunityInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityInsightRepository extends JpaRepository<CommunityInsight, Long> {

    List<CommunityInsight> findByCommunityIdAndDismissedFalseOrderBySeverityAscGeneratedAtDesc(Long communityId);
}
