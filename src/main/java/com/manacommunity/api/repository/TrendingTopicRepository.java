package com.manacommunity.api.repository;

import com.manacommunity.api.model.TrendingTopic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrendingTopicRepository extends JpaRepository<TrendingTopic, Long> {
    List<TrendingTopic> findByCommunityIdOrderByScoreDesc(Long communityId, Pageable pageable);
    long countByCommunityId(Long communityId);
    void deleteByCommunityId(Long communityId);
}
