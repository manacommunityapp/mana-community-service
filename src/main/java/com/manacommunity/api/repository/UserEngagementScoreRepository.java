package com.manacommunity.api.repository;

import com.manacommunity.api.model.UserEngagementScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserEngagementScoreRepository extends JpaRepository<UserEngagementScore, Long> {
    Optional<UserEngagementScore> findByUserIdAndCommunityId(Long userId, Long communityId);

    @Query("SELECT e FROM UserEngagementScore e WHERE e.community.id = :communityId ORDER BY e.totalPoints DESC")
    List<UserEngagementScore> findTopContributors(Long communityId, Pageable pageable);
}
