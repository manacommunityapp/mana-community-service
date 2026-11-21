package com.manacommunity.api.repository;

import com.manacommunity.api.model.UserEngagementScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface UserEngagementScoreRepository extends JpaRepository<UserEngagementScore, Long> {
    Optional<UserEngagementScore> findByUserIdAndCommunityId(Long userId, Long communityId);

    @Query("SELECT e FROM UserEngagementScore e JOIN FETCH e.user WHERE e.community.id = :communityId ORDER BY e.totalPoints DESC")
    List<UserEngagementScore> findTopContributors(Long communityId, Pageable pageable);

    long countByCommunityId(Long communityId);

    @Query("SELECT COUNT(e) FROM UserEngagementScore e WHERE e.community.id = :communityId " +
            "AND e.lastActivityAt >= :since")
    long countActiveSince(@Param("communityId") Long communityId,
                          @Param("since") LocalDateTime since);

}
